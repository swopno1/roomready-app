package com.vivereply.roomready.security

import com.vivereply.roomready.domain.model.StaffMember

/**
 * Keystore-backed encrypted storage for the property API key and staff identity.
 *
 * Rules enforced by this API:
 * - The key is only readable via [apiKeyForAuth], whose contract limits use
 *   to request signing at send time. It must never be logged, persisted
 *   elsewhere, or embedded in sync payloads.
 * - UI reads only [maskedApiKey] / [apiKeyLast4].
 */
interface SecureCredentialStore {
  fun saveApiKey(apiKey: String)

  fun hasApiKey(): Boolean

  /**
   * Full key for PMS request signing ONLY. Call at send time, never log,
   * never store the result beyond the request.
   */
  fun apiKeyForAuth(): String?

  fun apiKeyLast4(): String

  fun maskedApiKey(): String

  fun saveStaffIdentity(staff: StaffMember)

  fun staffIdentity(): StaffMember?

  fun clearApiKey()

  fun clearAll()
}
