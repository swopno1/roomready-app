package com.vivereply.roomready.data

import android.content.Context
import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.photos.PhotoStore
import com.vivereply.roomready.data.pms.ConnectionPrefs
import com.vivereply.roomready.data.pms.HttpPmsApi
import com.vivereply.roomready.data.pms.PmsApi
import com.vivereply.roomready.data.pms.PrefsTicketServerIds
import com.vivereply.roomready.data.pms.SyncOrchestrator
import com.vivereply.roomready.data.pms.TicketServerIds
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

  val conn: ConnectionPrefs by lazy { ConnectionPrefs(appContext) }

  val photos: PhotoStore by lazy { PhotoStore(appContext) }

  val pmsApi: PmsApi by lazy { HttpPmsApi() }

  val rooms: RoomRepository by lazy {
    RoomRepository(
      database.roomUnitDao(),
      database.roomTypeDao(),
      database.staffRosterDao(),
      database.checklistDao(),
      database.cleaningEvidenceDao(),
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

  val ticketIds: TicketServerIds by lazy { PrefsTicketServerIds(appContext) }

  val orchestrator: SyncOrchestrator by lazy {
    SyncOrchestrator(
      pmsApi,
      credentials,
      conn,
      rooms,
      tickets,
      database.syncQueueDao(),
      database.activityDao(),
      ticketIds,
      clock,
      appContext::getString,
    )
  }
}
