package com.vivereply.roomready.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.local.RoomUnitEntity
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.photos.PhotoStore
import com.vivereply.roomready.data.pms.ConnectionPrefs
import com.vivereply.roomready.data.pms.PmsAddress
import com.vivereply.roomready.data.pms.PrefsTicketServerIds
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncEntryStatus
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.security.SecureCredentialStore
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Disconnect wipes everything: the vault goes first (so a crash can never
 * leave secrets behind), then connection state, ticket ids, photo bytes,
 * and every database table.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataWipeTest {

  private val context: Context get() = ApplicationProvider.getApplicationContext()
  private lateinit var db: RoomReadyDatabase

  @Before
  fun setUp() {
    db =
      Room.inMemoryDatabaseBuilder(context, RoomReadyDatabase::class.java)
        .allowMainThreadQueries()
        .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `wipe empties vault connection tickets photos and tables`() =
    runTest {
      val order = mutableListOf<String>()
      val vault = RecordingVault(order)
      vault.saveApiKey("rk.test.secret")
      vault.saveStaffIdentity(StaffMember("Ayesha", StaffRole.HOUSEKEEPER))
      val conn = RecordingConn(ConnectionPrefs(context), order)
      conn.saveBaseUrl(PmsAddress("https://pms.example.com"))
      conn.saveProperty("prop-1", "Krishnachura Retreat")
      conn.saveLastSyncedAt(1_234L)
      val ticketIds = RecordingTicketIds(PrefsTicketServerIds(context), order)
      ticketIds.save("local-1", "srv-9")
      val photos = PhotoStore(context)
      photos.savePending("u-101", "photo.jpg")
      File(context.filesDir, "cleaning_photos/photo.jpg").writeText("bytes")
      File(context.filesDir, "ticket_photos/t.jpg").writeText("bytes")
      db.roomUnitDao()
        .upsert(
          RoomUnitEntity("u-101", "101", "rt-t", "1", Occupancy.VACANT, HousekeepingState.DIRTY, 1L),
        )
      db.syncQueueDao()
        .enqueue(
          SyncMutationEntity(
            entityType = SyncEntityType.UNIT_STATUS,
            entityId = "u-101",
            op = SyncOp.UPSERT,
            payloadJson = "{}",
            createdAt = 1L,
            dueAt = 1L,
            status = SyncEntryStatus.PENDING,
            updatedAt = 1L,
          ),
        )
      db.activityDao()
        .insert(
          ActivityEntity(
            type = ActivityType.STATUS_CHANGED,
            actorName = "Ayesha",
            role = null,
            unitId = "u-101",
            roomNumber = "101",
            detail = null,
            createdAt = 1L,
          ),
        )

      DataWipe(vault, conn, ticketIds, photos, db).execute()

      assertEquals(listOf("vault", "conn", "ticketIds"), order)
      assertTrue(vault.cleared)
      assertFalse(vault.hasApiKey())
      assertNull(vault.staffIdentity())
      assertNull(conn.baseUrl())
      assertNull(conn.lastSyncedAt())
      assertNull(conn.propertyName())
      assertNull(ticketIds.serverIdFor("local-1"))
      assertNull(ticketIds.localIdFor("srv-9"))
      assertNull(photos.pendingRef("u-101"))
      assertTrue(File(context.filesDir, "cleaning_photos").listFiles().isNullOrEmpty())
      assertTrue(File(context.filesDir, "ticket_photos").listFiles().isNullOrEmpty())
      assertEquals(0, db.roomUnitDao().count())
      assertEquals(0, db.syncQueueDao().pendingCount())
      assertTrue(db.activityDao().observeRecent(10).first().isEmpty())
    }

  private class RecordingConn(
    private val delegate: com.vivereply.roomready.data.pms.ConnectionStore,
    private val order: MutableList<String>,
  ) : com.vivereply.roomready.data.pms.ConnectionStore by delegate {
    override fun clear() {
      order.add("conn")
      delegate.clear()
    }
  }

  private class RecordingTicketIds(
    private val delegate: com.vivereply.roomready.data.pms.TicketServerIds,
    private val order: MutableList<String>,
  ) : com.vivereply.roomready.data.pms.TicketServerIds by delegate {
    override fun clear() {
      order.add("ticketIds")
      delegate.clear()
    }
  }

  private class RecordingVault(private val order: MutableList<String>) : SecureCredentialStore {
    var cleared: Boolean = false
    private var key: String? = null
    private var staff: StaffMember? = null

    override fun saveApiKey(apiKey: String) {
      key = apiKey
    }

    override fun hasApiKey(): Boolean = key != null

    override fun apiKeyForAuth(): String? = key

    override fun apiKeyLast4(): String = key?.takeLast(4) ?: ""

    override fun maskedApiKey(): String = if (key == null) "" else "••••" + apiKeyLast4()

    override fun saveStaffIdentity(staff: StaffMember) {
      this.staff = staff
    }

    override fun staffIdentity(): StaffMember? = staff

    override fun clearApiKey() {
      key = null
    }

    override fun clearAll() {
      order.add("vault")
      key = null
      staff = null
      cleared = true
    }
  }
}
