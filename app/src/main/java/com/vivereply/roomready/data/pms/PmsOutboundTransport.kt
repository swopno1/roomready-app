package com.vivereply.roomready.data.pms

import com.vivereply.roomready.R
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.sync.OutgoingMutation
import com.vivereply.roomready.data.sync.PmsTransport
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.security.SecureCredentialStore

/**
 * [PmsTransport] speaking the real PMS contract: each queued mutation is
 * mapped to its wire call at send time. Credentials come from the vault
 * and connection store only here; anything missing aborts the drain via
 * [PmsAuthException] (no entry is at fault, so nothing parks).
 *
 * Never sends an actor field: the PMS builds app:<keyId>:<staffName>
 * itself from the key and staffName.
 */
class PmsOutboundTransport(
  private val api: PmsApi,
  private val vault: SecureCredentialStore,
  private val conn: ConnectionStore,
  private val queue: SyncQueueDao,
  private val ticketIds: TicketServerIds,
  private val strings: (Int) -> String,
) : PmsTransport {

  override suspend fun send(mutation: OutgoingMutation) {
    val baseUrl = conn.baseUrl() ?: throw PmsAuthException("not connected")
    val fullKey = vault.apiKeyForAuth() ?: throw PmsAuthException("missing api key")
    if (ApiKeyFormat.keyIdOf(fullKey) == null) throw PmsAuthException("malformed api key")
    val staff = vault.staffIdentity() ?: throw PmsAuthException("missing staff identity")
    when (val call = OutboundMapper.map(mutation.entityType, mutation.entityId, mutation.payloadJson)) {
      is MappedCall.LocalResolve -> return
      is MappedCall.Untranslatable ->
        throw PmsRefusedException(
          httpStatus = null,
          lastError = "UNTRANSLATABLE",
          flagDetail = "${mutation.entityType} ${mutation.entityId} UNTRANSLATABLE ${call.reason}",
        )
      is MappedCall.RoomStatus ->
        execute(mutation, staff.role) {
          api.postRoomStatus(
            baseUrl,
            fullKey,
            call.unitId,
            StatusBody(call.expected, call.action, staff.name, call.note),
          )
        }
      is MappedCall.TicketCreate -> {
        val serverId =
          execute(mutation, staff.role) {
            api.createTicket(
              baseUrl,
              fullKey,
              TicketCreateBody(call.unitId, call.title, call.description, "NORMAL", staff.name),
            )
          }
        ticketIds.save(call.localId, serverId)
      }
      is MappedCall.TicketDone -> {
        val serverId = ticketIds.serverIdFor(call.localId)
        if (serverId == null) {
          if (queue.unsyncedCountFor(SyncEntityType.TICKET, call.localId) > 0) {
            throw DeferEntryException()
          }
          throw PmsRefusedException(
            httpStatus = null,
            lastError = "UNTRANSLATABLE",
            flagDetail = "TICKET_STATUS ${call.localId} UNTRANSLATABLE missing-server-id",
          )
        }
        execute(mutation, staff.role) {
          api.updateTicket(
            baseUrl,
            fullKey,
            serverId,
            TicketUpdateBody(TicketStatus.DONE, staff.name),
          )
        }
      }
    }
  }

  /**
   * Runs one wire call. 401 and anything non-parkable propagate for the
   * engine (stop-the-drain / backoff); refusals become parked flags.
   */
  private suspend fun <T> execute(
    mutation: OutgoingMutation,
    role: StaffRole?,
    block: suspend () -> T,
  ): T {
    try {
      return block()
    } catch (e: PmsApiException) {
      if (e.statusCode == 401 || !e.parkable()) throw e
      throw PmsRefusedException(
        httpStatus = e.statusCode,
        lastError = "HTTP ${e.statusCode}",
        flagDetail = flagDetail(mutation, e.statusCode!!, role),
      )
    }
  }

  private fun flagDetail(mutation: OutgoingMutation, code: Int, role: StaffRole?): String {
    val head = "${mutation.entityType} ${mutation.entityId} HTTP $code"
    val message =
      when {
        code == 403 && mutation.entityType == SyncEntityType.TICKET_STATUS &&
          role == StaffRole.HOUSEKEEPER -> strings(R.string.sync_parked_ticket_done_role)
        code == 403 -> strings(R.string.sync_parked_no_permission)
        else -> null
      }
    return if (message == null) head else "$head: $message"
  }
}
