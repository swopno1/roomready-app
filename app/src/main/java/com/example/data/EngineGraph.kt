package com.example.data

import android.content.Context
import com.example.data.local.RoomReadyDatabase
import com.example.data.repository.ActivityRepository
import com.example.data.repository.RoomRepository
import com.example.data.repository.TicketRepository
import com.example.data.sync.PmsTransport
import com.example.data.sync.SyncEngine
import com.example.security.KeystoreCredentialStore
import com.example.security.SecureCredentialStore

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
