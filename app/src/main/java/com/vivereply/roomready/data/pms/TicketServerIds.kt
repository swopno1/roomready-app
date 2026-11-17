package com.vivereply.roomready.data.pms

import android.content.Context
import androidx.core.content.edit

/**
 * Local ticket id <-> PMS server id map. The server id replaces the local
 * id for all future wire purposes (PATCH paths, reconciliation), without
 * touching the ticket table: plain prefs, synchronous writes, no secrets.
 */
interface TicketServerIds {
  fun save(localId: String, serverId: String)

  fun serverIdFor(localId: String): String?

  fun localIdFor(serverId: String): String?

  fun clear()
}

class PrefsTicketServerIds(context: Context) : TicketServerIds {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

  override fun save(localId: String, serverId: String) {
    prefs.edit(commit = true) { putString(key(localId), serverId) }
  }

  override fun serverIdFor(localId: String): String? = prefs.getString(key(localId), null)

  override fun localIdFor(serverId: String): String? {
    for ((key, value) in prefs.all) {
      if (key.startsWith(PREFIX) && value == serverId) return key.removePrefix(PREFIX)
    }
    return null
  }

  override fun clear() {
    prefs.edit(commit = true) { clear() }
  }

  private fun key(localId: String): String = "$PREFIX$localId"

  companion object {
    const val PREFS_FILE: String = "roomready_ticket_ids"

    private const val PREFIX = "t:"
  }
}
