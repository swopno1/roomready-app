package com.vivereply.roomready.data.pms

import android.content.Context
import androidx.core.content.edit

/**
 * Non-secret connection state: PMS address, last sync time, property display
 * name. The API key and staff identity live only in the encrypted vault,
 * never here.
 */
interface ConnectionStore {
  fun baseUrl(): PmsAddress?

  fun saveBaseUrl(address: PmsAddress)

  fun lastSyncedAt(): Long?

  fun saveLastSyncedAt(atEpochMillis: Long)

  fun propertyName(): String?

  fun saveProperty(id: String?, name: String?)

  fun clear()
}

/** [ConnectionStore] in plain SharedPreferences. */
class ConnectionPrefs(context: Context) : ConnectionStore {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

  override fun baseUrl(): PmsAddress? {
    val raw = prefs.getString(KEY_BASE_URL, null) ?: return null
    return PmsAddressValidator.validate(raw).getOrNull()
  }

  override fun saveBaseUrl(address: PmsAddress) {
    prefs.edit { putString(KEY_BASE_URL, address.origin) }
  }

  override fun lastSyncedAt(): Long? {
    val value = prefs.getLong(KEY_LAST_SYNCED_AT, 0L)
    return if (value > 0L) value else null
  }

  override fun saveLastSyncedAt(atEpochMillis: Long) {
    prefs.edit { putLong(KEY_LAST_SYNCED_AT, atEpochMillis) }
  }

  override fun propertyName(): String? = prefs.getString(KEY_PROPERTY_NAME, null)

  override fun saveProperty(id: String?, name: String?) {
    prefs.edit {
      if (id == null) remove(KEY_PROPERTY_ID) else putString(KEY_PROPERTY_ID, id)
      if (name == null) remove(KEY_PROPERTY_NAME) else putString(KEY_PROPERTY_NAME, name)
    }
  }

  override fun clear() {
    prefs.edit { clear() }
  }

  companion object {
    const val PREFS_FILE: String = "roomready_conn"

    private const val KEY_BASE_URL = "pms_base_url"
    private const val KEY_LAST_SYNCED_AT = "last_synced_at"
    private const val KEY_PROPERTY_ID = "property_id"
    private const val KEY_PROPERTY_NAME = "property_name"
  }
}
