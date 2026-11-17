package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.TicketStatus

/**
 * Live PMS housekeeping API contract (contractVersion 1).
 *
 * - Setup validates via GET {address}/api/housekeeping/bootstrap.
 * - Room status: POST {address}/api/housekeeping/rooms/{unitId}/status
 *   with {expected, action, staffName, note?}; success is HTTP 200.
 * - Tickets: POST {address}/api/housekeeping/tickets (201 + server id),
 *   PATCH {address}/api/housekeeping/tickets/{serverId} (supervisor only),
 *   GET {address}/api/housekeeping/tickets?status=OPEN|DONE (bare array).
 * - Every request carries `Authorization: Bearer <full-key>` (header only).
 * - The server builds the audit actor app:<keyId>:<staffName> itself;
 *   the app never sends an actor field, anywhere, in any payload.
 * - Checklist progress, photos, and evidence refs never cross the wire;
 *   only text evidence travels (skip reason as note on MARK_CLEAN,
 *   send-back reason as note on INSPECT_FAIL).
 */
object PmsContract {
  const val CONTRACT_VERSION: Int = 1
  const val BOOTSTRAP_PATH: String = "/api/housekeeping/bootstrap"
  const val ROOMS_PATH: String = "/api/housekeeping/rooms"
  const val TICKETS_PATH: String = "/api/housekeeping/tickets"
  const val AUTH_SCHEME: String = "Bearer"
  const val CONNECT_TIMEOUT_MILLIS: Int = 10_000
  const val READ_TIMEOUT_MILLIS: Int = 15_000

  fun bootstrapUrl(baseOrigin: String): String = baseOrigin + BOOTSTRAP_PATH

  fun roomStatusUrl(baseOrigin: String, unitId: String): String =
    "$baseOrigin$ROOMS_PATH/${encodePath(unitId)}/status"

  fun ticketsUrl(baseOrigin: String): String = baseOrigin + TICKETS_PATH

  fun ticketUrl(baseOrigin: String, serverId: String): String =
    "$baseOrigin$TICKETS_PATH/${encodePath(serverId)}"

  fun ticketsQueryUrl(baseOrigin: String, status: TicketStatus): String =
    "$baseOrigin$TICKETS_PATH?status=${status.name}"

  fun authHeaderValue(apiKey: String): String = "$AUTH_SCHEME $apiKey"

  private fun encodePath(segment: String): String =
    java.net.URLEncoder.encode(segment, Charsets.UTF_8.name())
}
