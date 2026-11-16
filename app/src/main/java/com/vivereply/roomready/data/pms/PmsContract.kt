package com.vivereply.roomready.data.pms

/**
 * Live PMS housekeeping API contract (contractVersion 1).
 *
 * Specified by the product brief:
 * - Setup validates via GET {address}/api/housekeeping/bootstrap.
 * - Bootstrap returns room types, units with PMS housekeeping state,
 *   a staff roster, and the checklist.
 * - Outbound pushes record the actor as "app:<keyId>:<staffName>".
 *
 * Assumptions made where the deployed API was not documented; each is a
 * one-line change here if the live API differs:
 * - Auth travels as an `Authorization: Bearer <full-key>` header.
 * - Each queued mutation pushes to POST {address}/api/housekeeping/outbound
 *   with body {"entityType","entityId","op","payload","actor","at"}.
 * - Success is any 2xx; 401/403 mean wrong key, 404 means wrong address,
 *   400/422 mean the entry itself is refused (park, don't retry).
 */
object PmsContract {
  const val CONTRACT_VERSION: Int = 1
  const val BOOTSTRAP_PATH: String = "/api/housekeeping/bootstrap"
  const val OUTBOUND_PATH: String = "/api/housekeeping/outbound"
  const val AUTH_SCHEME: String = "Bearer"
  const val CONNECT_TIMEOUT_MILLIS: Int = 10_000
  const val READ_TIMEOUT_MILLIS: Int = 15_000

  fun bootstrapUrl(baseOrigin: String): String = baseOrigin + BOOTSTRAP_PATH

  fun outboundUrl(baseOrigin: String): String = baseOrigin + OUTBOUND_PATH

  fun authHeaderValue(apiKey: String): String = "$AUTH_SCHEME $apiKey"

  /** Actor string the PMS records for app-originated pushes. */
  fun actorFor(keyId: String, staffName: String): String = "app:$keyId:$staffName"
}
