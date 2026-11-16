package com.vivereply.roomready.data

import android.content.Context
import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.repository.ActivityRepository
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.data.repository.TicketRepository
import com.vivereply.roomready.data.sync.PmsTransport
import com.vivereply.roomready.data.sync.SyncEngine
import com.vivereply.roomready.security.KeystoreCredentialStore
import com.vivereply.roomready.security.SecureCredentialStore

/**
 * Composition root for the offline engine (no DI framework on purpose:
 * small surface, cheap on 2 GB RAM devices).
 */
class EngineGraph(context: Context, val clock: Clock = SystemClock) {
  private val appContext: Context = context.applicationContext

  val database: RoomReadyDatabase by lazy { RoomReadyDatabase.create(appContext) }

  val credentials: SecureCredentialStore by lazy { KeystoreCredentialStore(appContext) }

  val rooms: RoomRepository by lazy {
    RoomRepository(
      database.roomUnitDao(),
      database.roomTypeDao(),
      database.checklistDao(),
      database.activityDao(),
      database.syncQueueDao(),
      clock,
    )
  }

  val tickets: TicketRepository by lazy {
    TicketRepository(
      database.roomUnitDao(),
      database.ticketDao(),
      database.activityDao(),
      database.syncQueueDao(),
      clock,
    )
  }

  val activity: ActivityRepository by lazy { ActivityRepository(database.activityDao()) }

  /** Sync engine bound to a transport; local actions never touch the network. */
  fun syncEngine(transport: PmsTransport): SyncEngine =
    SyncEngine(database.syncQueueDao(), database.activityDao(), transport, clock)
}
