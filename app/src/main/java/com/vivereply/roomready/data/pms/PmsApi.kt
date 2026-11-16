package com.vivereply.roomready.data.pms

import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import java.io.IOException

/** One outbound queue entry packaged for the PMS. */
data class PushRequest(
  val baseUrl: PmsAddress,
  val apiKey: String,
  val entityType: SyncEntityType,
  val entityId: String,
  val op: SyncOp,
  /** Per-type payload object JSON (no actor, no credentials). */
  val payloadJson: String,
  /** PMS actor string: app:<keyId>:<staffName>. */
  val actor: String,
  val atEpochMillis: Long,
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
 * Raw PMS HTTP calls. Implementations block; callers run them on Dispatchers.IO.
 */
interface PmsApi {
  suspend fun fetchBootstrap(baseUrl: PmsAddress, apiKey: String): String

  suspend fun push(request: PushRequest)
}
