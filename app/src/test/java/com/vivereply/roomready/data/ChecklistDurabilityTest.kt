package com.vivereply.roomready.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.local.RoomTypeEntity
import com.vivereply.roomready.data.local.RoomUnitEntity
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TurnoverStep
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Process-death durability: checklist progress and cleaning state live in
 * the on-disk database, so kill-and-reopen restores them exactly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChecklistDurabilityTest {

  @Test
  fun `kill and reopen mid-checklist restores progress exactly`() =
    runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val name = "durability-test.db"
      context.deleteDatabase(name)

      var db = open(context, name)
      var rooms = repository(db)
      db.roomTypeDao().upsertAll(
        listOf(RoomTypeEntity("t-dlx", "DLX", "Deluxe", "ডিলাক্স", "1 king", 2)),
      )
      db.roomUnitDao().upsert(
        RoomUnitEntity("u-101", "101", "t-dlx", "1", Occupancy.VACANT, HousekeepingState.DIRTY, 1L),
      )
      val housekeeper = StaffMember("Ayesha", StaffRole.HOUSEKEEPER)
      assertTrue(rooms.startCleaning("u-101", housekeeper).isSuccess)
      TurnoverStep.entries.take(5).forEach { step ->
        assertTrue(rooms.setStepDone("u-101", step, true, housekeeper).isSuccess)
      }
      val before = rooms.progress("u-101")
      assertEquals(5, before.doneCount)
      db.close()

      // Simulate process death: reopen the same database file fresh.
      db = open(context, name)
      rooms = repository(db)
      val after = rooms.progress("u-101")
      assertEquals(before, after)
      assertEquals(HousekeepingState.CLEANING, rooms.getUnit("u-101")?.hkState)

      // And the reopened session can continue where it left off.
      TurnoverStep.entries.drop(5).forEach { step ->
        assertTrue(rooms.setStepDone("u-101", step, true, housekeeper).isSuccess)
      }
      assertTrue(rooms.progress("u-101").isComplete)
      assertTrue(rooms.finishCleaning("u-101", housekeeper, null, "too dark").isSuccess)
      assertEquals(HousekeepingState.CLEANED, rooms.getUnit("u-101")?.hkState)
      assertEquals("too dark", rooms.evidenceFor("u-101")?.skipReason)

      db.close()
      context.deleteDatabase(name)
    }

  private fun open(context: Context, name: String): RoomReadyDatabase =
    Room.databaseBuilder(context, RoomReadyDatabase::class.java, name)
      .allowMainThreadQueries()
      .build()

  private fun repository(db: RoomReadyDatabase): RoomRepository =
    RoomRepository(
      db.roomUnitDao(),
      db.roomTypeDao(),
      db.staffRosterDao(),
      db.checklistDao(),
      db.cleaningEvidenceDao(),
      db.activityDao(),
      db.syncQueueDao(),
      SystemClock,
      db,
    )
}
