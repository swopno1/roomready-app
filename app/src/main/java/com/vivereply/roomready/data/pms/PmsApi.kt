package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.PmsHousekeeping
import com.vivereply.roomready.domain.model.TicketStatus
import java.io.IOException

/** Room-status actions on POST /rooms/{unitId}/status. */
enum class StatusAction(val wire: String) {
  MARK_CLEAN("MARK_CLEAN"),
  MARK_DIRTY("MARK_DIRTY"),
  INSPECT_PASS("INSPECT_PASS"),
  INSPECT_FAIL("INSPECT_FAIL"),
}

/** POST /rooms/{unitId}/status body: exactly these fields, note omitted when blank. */
data class StatusBody(
  val expected: PmsHousekeeping,
  val action: StatusAction,
  val staffName: String,
  val note: String? = null,
)

/** POST /tickets body. The app has no priority concept, so NORMAL always. */
data class TicketCreateBody(
  val unitId: String,
  /** Category English label: Plumbing / Electrical / AC / Furniture / Other. */
  val title: String,
  val description: String? = null,
  val priority: String = "NORMAL",
  val staffName: String,
)

/** PATCH /tickets/{serverId} body. */
data class TicketUpdateBody(
  val status: TicketStatus,
  val staffName: String,
)

/**
 * Transport-level PMS failure. Carries the HTTP status (null for
 * connection-level failures) and the cause; never the API key.
 */
open class PmsApiException(
  message: String,
  val statusCode: Int? = null,
  cause: Throwable? = null,
) : IOException(message, cause) {

  fun isAuthFailure(): Boolean = statusCode == 401 || statusCode == 403

  fun isNotFound(): Boolean = statusCode == 404

  /**
   * True when the server refused this specific entry and retrying is
   * pointless (park it). 408/425/429 and 5xx/network failures stay retryable.
   * 401 stays included as a fallback branch; the engine intercepts 401 first
   * (stop the drain, wipe the key) so it never parks.
   */
  fun parkable(): Boolean {
    val code = statusCode ?: return false
    return code in 400..499 && code != 408 && code != 425 && code != 429
  }
}

/**
 * Local credentials are missing or malformed at send time. The drain must
 * abort without parking or rescheduling: no entry is at fault.
 */
class PmsAuthException(message: String) : IOException(message)

/**
 * One queue entry the PMS refused (or that cannot be translated): park it
 * with [lastError] on the queue row and flag [flagDetail] on the activity
 * log, then continue the drain. [httpStatus] is null for entries that never
 * reached the wire.
 */
class PmsRefusedException(
  val httpStatus: Int?,
  val lastError: String,
  val flagDetail: String,
) : IOException(flagDetail)

/**
 * The entry is valid but its ticket create is still queued ahead of it.
 * The drain defers it back to pending (attempts unchanged) instead of
 * parking: create-then-done order resolves it on a later pass.
 */
class DeferEntryException : IOException("create still queued")

/**
 * Raw PMS HTTP calls. Implementations block; callers run them on Dispatchers.IO.
 */
interface PmsApi {
  suspend fun fetchBootstrap(baseUrl: PmsAddress, apiKey: String): String

  suspend fun postRoomStatus(
    baseUrl: PmsAddress,
    apiKey: String,
    unitId: String,
    body: StatusBody,
  )

  /** Returns the server ticket id from the 201 body. */
  suspend fun createTicket(
    baseUrl: PmsAddress,
    apiKey: String,
    body: TicketCreateBody,
  ): String

  suspend fun updateTicket(
    baseUrl: PmsAddress,
    apiKey: String,
    serverId: String,
    body: TicketUpdateBody,
  )

  /** Raw JSON array body for one ticket status. */
  suspend fun fetchTickets(
    baseUrl: PmsAddress,
    apiKey: String,
    status: TicketStatus,
  ): String
}
