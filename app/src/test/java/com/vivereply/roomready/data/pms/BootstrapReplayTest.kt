package com.vivereply.roomready.data.pms

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.domain.model.HousekeepingState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Replay proof: the bootstrap payload shape flows parser -> snapshot writer
 * -> board query and lands 5 room types / 19 units. The fixture lives in
 * test sources only; shipped code never contains inventory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BootstrapReplayTest {

  private lateinit var db: RoomReadyDatabase
  private lateinit var rooms: RoomRepository

  @Before
  fun setUp() {
    db =
      Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        RoomReadyDatabase::class.java,
      ).allowMainThreadQueries().build()
    rooms =
      RoomRepository(
        db.roomUnitDao(),
        db.roomTypeDao(),
        db.staffRosterDao(),
        db.checklistDao(),
        db.activityDao(),
        db.syncQueueDao(),
        FakeClock(),
      )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `replay stores 5 types and 19 units with the per-type split`() =
    runTest {
      val json = javaClass.classLoader!!.getResourceAsStream("bootstrap_replay.json")!!
        .bufferedReader().use { it.readText() }
      val parsed = BootstrapParser.parse(json, 9_000L).getOrThrow()
      assertTrue("replay payload must parse warning-free: ${parsed.warnings}", parsed.warnings.isEmpty())

      val import = rooms.importBootstrap(parsed.roomTypes, parsed.units)
      rooms.importRoster(parsed.staff)
      assertEquals(5, import.types)
      assertEquals(19, import.units)

      // Board query: types, units, and the per-type unit split.
      val types = db.roomTypeDao().getAll()
      assertEquals(
        listOf("Bamboo Cottage", "Clay House Suite", "Earth Lodge", "Palm Loft", "Terracotta Villa"),
        types.map { it.nameEn },
      )
      val board = db.roomUnitDao().observeAll().first()
      assertEquals(19, board.size)
      val perType = board.groupBy { it.roomTypeId }.mapValues { it.value.size }
      assertEquals(4, perType["rt-terracotta"])
      assertEquals(6, perType["rt-bamboo"])
      assertEquals(4, perType["rt-clayhouse"])
      assertEquals(3, perType["rt-palmloft"])
      assertEquals(2, perType["rt-earthlodge"])

      // States mapped per contract; the unit missing housekeepingAt degraded to null.
      val byNumber = board.associateBy { it.number }
      assertEquals(HousekeepingState.DIRTY, byNumber["101"]?.hkState)
      assertEquals(HousekeepingState.CLEANED, byNumber["103"]?.hkState)
      assertEquals(HousekeepingState.READY, byNumber["104"]?.hkState)
      assertNull(byNumber["202"]?.housekeepingAt)
      assertEquals(1_794_809_400_000L, byNumber["101"]?.housekeepingAt)

      assertEquals(4, db.staffRosterDao().getAll().size)
      assertEquals("Krishnachura Retreat", parsed.propertyName)
      assertEquals(8, parsed.checklistKeys.size)
    }

  private class FakeClock : Clock {
    override fun nowEpochMillis(): Long = 9_000L
  }
}
