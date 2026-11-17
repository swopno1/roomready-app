package com.vivereply.roomready.data.pms

import android.util.Log
import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.data.repository.TicketRepository
import com.vivereply.roomready.data.sync.SyncEngine
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.security.SecureCredentialStore
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Outcome of a successful first connect. */
data class ConnectReport(
  val roomTypes: Int,
  val units: Int,
  val staff: Int,
  val role: StaffRole,
  val warnings: List<BootstrapWarning>,
)

/** Outcome of a manual sync. */
data class SyncReport(
  val roomTypes: Int,
  val units: Int,
  val delivered: Int,
  val parked: Int,
  val pendingAfter: Int,
  val warnings: List<BootstrapWarning>,
  val refreshed: Boolean = false,
)

/**
 * Owns the PMS relationship: first-connect validation, credential storage,
 * bootstrap import, and manual sync. Network runs on Dispatchers.IO.
 *
 * Ordering guarantee: on connect, nothing is written to the vault, prefs, or
 * database until the bootstrap payload has validated AND parsed. A wrong key
 * is refused cleanly with zero local trace.
 */
class SyncOrchestrator(
  private val api: PmsApi,
  private val vault: SecureCredentialStore,
  private val conn: ConnectionStore,
  private val rooms: RoomRepository,
  private val tickets: TicketRepository,
  private val queue: SyncQueueDao,
  private val activity: ActivityDao,
  private val ticketIds: TicketServerIds,
  private val clock: Clock,
  private val strings: (Int) -> String,
) {

  fun isConnected(): Boolean =
    try {
      conn.baseUrl() != null && vault.hasApiKey()
    } catch (e: Exception) {
      Log.w(TAG, "credential check failed", e)
      false
    }

  fun lastSyncedAt(): Long? = conn.lastSyncedAt()

  fun propertyName(): String? = conn.propertyName()

  fun pmsAddress(): String? = conn.baseUrl()?.origin

  fun staffIdentity(): StaffMember? = vault.staffIdentity()

  suspend fun pendingCount(): Int = queue.pendingCount()

  suspend fun validateAndConnect(
    rawUrl: String?,
    rawKey: String?,
    staffName: String?,
  ): Result<ConnectReport> =
    withContext(Dispatchers.IO) {
      val address =
        PmsAddressValidator.validate(rawUrl).getOrElse {
          return@withContext Result.failure(PmsException(PmsError.BadAddress()))
        }
      val key = rawKey?.trim()
      if (ApiKeyFormat.parse(key) == null) {
        return@withContext Result.failure(PmsException(PmsError.BadKeyFormat))
      }
      val name = staffName?.trim()
      if (name.isNullOrEmpty()) {
        return@withContext Result.failure(PmsException(PmsError.BlankName))
      }

      val body =
        try {
          api.fetchBootstrap(address, key!!)
        } catch (e: PmsApiException) {
          return@withContext Result.failure(PmsException(mapBootstrapError(e), e))
        }
      val parsed =
        try {
          BootstrapParser.parse(body, clock.nowEpochMillis()).getOrThrow()
        } catch (e: BootstrapException.ContractMismatch) {
          return@withContext Result.failure(PmsException(PmsError.ContractMismatch(e.serverVersion)))
        } catch (e: BootstrapException) {
          return@withContext Result.failure(PmsException(PmsError.ServerDown("bad payload")))
        }

      // Snapshot first, credentials second: a DB failure leaves no half-connected state.
      val import = rooms.importBootstrap(parsed.roomTypes, parsed.units)
      rooms.importRoster(parsed.staff)
      val role =
        parsed.staff.firstOrNull { it.name.equals(name, ignoreCase = true) }?.role
          ?: StaffRole.HOUSEKEEPER
      vault.saveApiKey(key)
      vault.saveStaffIdentity(StaffMember(name, role))
      conn.saveBaseUrl(address)
      conn.saveProperty(parsed.propertyId, parsed.propertyName)
      conn.saveLastSyncedAt(clock.nowEpochMillis())
      logWarnings(parsed.warnings)

      Result.success(
        ConnectReport(
          roomTypes = import.types,
          units = import.units,
          staff = parsed.staff.size,
          role = role,
          warnings = parsed.warnings,
        ),
      )
    }

  suspend fun syncNow(): Result<SyncReport> =
    withContext(Dispatchers.IO) {
      val address = conn.baseUrl()
      val key = vault.apiKeyForAuth()
      if (address == null || key == null) {
        return@withContext Result.failure(PmsException(PmsError.BadAddress("not connected")))
      }
      val body =
        try {
          api.fetchBootstrap(address, key)
        } catch (e: PmsApiException) {
          if (e.statusCode == 401) vault.clearApiKey()
          return@withContext Result.failure(PmsException(mapBootstrapError(e), e))
        }
      val parsed =
        try {
          BootstrapParser.parse(body, clock.nowEpochMillis()).getOrThrow()
        } catch (e: BootstrapException.ContractMismatch) {
          return@withContext Result.failure(PmsException(PmsError.ContractMismatch(e.serverVersion)))
        } catch (e: BootstrapException) {
          return@withContext Result.failure(PmsException(PmsError.ServerDown("bad payload")))
        }

      val import = rooms.importBootstrap(parsed.roomTypes, parsed.units)
      rooms.importRoster(parsed.staff)
      conn.saveLastSyncedAt(clock.nowEpochMillis())
      logWarnings(parsed.warnings)
      reconcileTickets(address, key)

      val engine = SyncEngine(queue, activity, outboundTransport(), clock)
      val drain = engine.drain()
      if (drain.authRevoked) {
        vault.clearApiKey()
        return@withContext Result.failure(PmsException(PmsError.WrongKey))
      }
      var refreshed = false
      if (drain.refreshScheduled) {
        refreshed = refreshAfterConflict(address, key)
      }
      Result.success(
        SyncReport(
          roomTypes = import.types,
          units = import.units,
          delivered = drain.delivered,
          parked = drain.parked,
          pendingAfter = queue.pendingCount(),
          warnings = parsed.warnings,
          refreshed = refreshed,
        ),
      )
    }

  /**
   * Reconciles local tickets against both server lists. Server status wins
   * one way (OPEN -> DONE only; the domain forbids reopening), with the
   * server's done timestamp. Locally unsynced tickets and remote-only rows
   * are untouched. Any fetch failure skips reconciliation; the drain still
   * runs.
   */
  private suspend fun reconcileTickets(address: PmsAddress, key: String) {
    try {
      val remote =
        ServerTickets.parseArray(api.fetchTickets(address, key, TicketStatus.OPEN)) +
          ServerTickets.parseArray(api.fetchTickets(address, key, TicketStatus.DONE))
      for (ticket in remote) {
        val localId = ticketIds.localIdFor(ticket.id) ?: continue
        val local = tickets.getTicket(localId) ?: continue
        if (local.status == TicketStatus.OPEN && ticket.status == TicketStatus.DONE) {
          tickets.applyServerDone(localId, ticket.doneBy ?: "PMS", ticket.doneAt ?: clock.nowEpochMillis())
        }
      }
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (e: Exception) {
      Log.w(TAG, "ticket reconcile skipped", e)
    }
  }

  /**
   * Re-pulls bootstrap after a 409: the PMS moved since `expected`, so the
   * server is re-read as the source of truth. Best effort within this sync.
   */
  private suspend fun refreshAfterConflict(address: PmsAddress, key: String): Boolean {
    try {
      val body = api.fetchBootstrap(address, key)
      val parsed = BootstrapParser.parse(body, clock.nowEpochMillis()).getOrThrow()
      rooms.importBootstrap(parsed.roomTypes, parsed.units)
      rooms.importRoster(parsed.staff)
      return true
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (e: Exception) {
      Log.w(TAG, "conflict refresh skipped", e)
      return false
    }
  }

  private fun outboundTransport(): PmsOutboundTransport =
    PmsOutboundTransport(api, vault, conn, queue, ticketIds, strings)

  private fun mapBootstrapError(e: PmsApiException): PmsError =
    when {
      e.isAuthFailure() -> PmsError.WrongKey
      e.isNotFound() -> PmsError.BadAddress("bootstrap 404")
      e.cause is UnknownHostException -> PmsError.BadAddress("unknown host")
      else -> PmsError.ServerDown("HTTP ${e.statusCode}")
    }

  private fun logWarnings(warnings: List<BootstrapWarning>) {
    warnings.forEach { Log.w(TAG, "bootstrap: $it") }
  }

  companion object {
    private const val TAG = "RoomReadySync"
  }
}
