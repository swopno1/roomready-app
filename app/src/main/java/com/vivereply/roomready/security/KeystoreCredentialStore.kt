// EncryptedSharedPreferences/MasterKey are deprecated in security-crypto 1.1.0
// but remain the Keystore-backed encrypted store with no successor in this
// artifact; the SecureCredentialStore interface isolates them for a future swap.
@file:Suppress("DEPRECATION")

package com.vivereply.roomready.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole

/**
 * [SecureCredentialStore] backed by Android Keystore + EncryptedSharedPreferences.
 * Nothing here touches logs, analytics, or plain SharedPreferences.
 */
class KeystoreCredentialStore(context: Context) : SecureCredentialStore {

  private val appContext: Context = context.applicationContext

  private val prefs: SharedPreferences by lazy {
    val masterKey =
      MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    EncryptedSharedPreferences.create(
      appContext,
      PREFS_FILE,
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  }

  override fun saveApiKey(apiKey: String) {
    require(apiKey.isNotBlank()) { "API key must not be blank" }
    prefs.edit { putString(KEY_API_KEY, apiKey) }
  }

  override fun hasApiKey(): Boolean = prefs.contains(KEY_API_KEY)

  override fun apiKeyForAuth(): String? = prefs.getString(KEY_API_KEY, null)

  override fun apiKeyLast4(): String = ApiKeyMask.last4(prefs.getString(KEY_API_KEY, null))

  override fun maskedApiKey(): String = ApiKeyMask.masked(prefs.getString(KEY_API_KEY, null))

  override fun saveStaffIdentity(staff: StaffMember) {
    prefs.edit {
      putString(KEY_STAFF_NAME, staff.name)
      putString(KEY_STAFF_ROLE, staff.role.name)
    }
  }

  override fun staffIdentity(): StaffMember? {
    val name = prefs.getString(KEY_STAFF_NAME, null) ?: return null
    val roleName = prefs.getString(KEY_STAFF_ROLE, null) ?: return null
    val role = runCatching { StaffRole.valueOf(roleName) }.getOrNull() ?: return null
    if (name.isBlank()) return null
    return StaffMember(name, role)
  }

  override fun clearApiKey() {
    prefs.edit { remove(KEY_API_KEY) }
  }

  override fun clearAll() {
    prefs.edit { clear() }
  }

  companion object {
    /** Must match the backup-exclusion rules in backup_rules.xml. */
    const val PREFS_FILE: String = "roomready_credentials"

    private const val KEY_API_KEY = "pms_api_key"
    private const val KEY_STAFF_NAME = "staff_name"
    private const val KEY_STAFF_ROLE = "staff_role"
  }
}
