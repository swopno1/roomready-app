package com.vivereply.roomready.data

import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.photos.PhotoStore
import com.vivereply.roomready.data.pms.ConnectionStore
import com.vivereply.roomready.data.pms.TicketServerIds
import com.vivereply.roomready.security.SecureCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full local wipe for Disconnect: credentials first (so a crash can never
 * leave secrets behind while reporting success), then connection state,
 * ticket id map, photo bytes, and finally every database table.
 */
class DataWipe(
  private val vault: SecureCredentialStore,
  private val conn: ConnectionStore,
  private val ticketIds: TicketServerIds,
  private val photos: PhotoStore,
  private val database: RoomReadyDatabase,
) {
  suspend fun execute() =
    withContext(Dispatchers.IO) {
      vault.clearAll()
      conn.clear()
      ticketIds.clear()
      photos.clearAll()
      database.clearAllTables()
    }
}
