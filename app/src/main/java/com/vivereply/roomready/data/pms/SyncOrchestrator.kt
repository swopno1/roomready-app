package com.vivereply.roomready.data.pms

import android.util.Log
import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.data.sync.SyncEngine
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.security.SecureCredentialStore
import java.net.UnknownHostException
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
  private val queue: SyncQueueDao,
  private val activity: ActivityDao,
  private val clock: Clock,
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

      val engine = SyncEngine(queue, activity, outboundTransport(), clock)
      val drain = engine.drain()
      Result.success(
        SyncReport(
          roomTypes = import.types,
          units = import.units,
          delivered = drain.delivered,
          parked = drain.parked,
          pendingAfter = queue.pendingCount(),
          warnings = parsed.warnings,
        ),
      )
    }

  private fun outboundTransport(): PmsOutboundTransport =
    PmsOutboundTransport(api, vault, conn, clock)

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
