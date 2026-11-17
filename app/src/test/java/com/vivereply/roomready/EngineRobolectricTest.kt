package com.vivereply.roomready

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.data.local.ChecklistStepEntity
import com.vivereply.roomready.data.local.RoomConverters
import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.local.RoomTypeEntity
import com.vivereply.roomready.data.local.RoomUnitEntity
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.TicketEntity
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncEntryStatus
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.domain.model.TurnoverStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Engine persistence against a real (in-memory) Room database, plus
 * verbatim checklist strings and Bengali parity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EngineRobolectricTest {

  private lateinit var db: RoomReadyDatabase

  @Before
  fun setUp() {
    db =
      Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        RoomReadyDatabase::class.java,
      ).allowMainThreadQueries().build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `room types and units round trip with enums intact`() =
    runTest {
      db.roomTypeDao().upsertAll(
        listOf(RoomTypeEntity("t-dlx", "DLX", "Deluxe", "ডিলাক্স", "1 king", 2)),
      )
      db.roomUnitDao().upsert(
        RoomUnitEntity("u-101", "101", "t-dlx", "1", Occupancy.OCCUPIED, HousekeepingState.DIRTY, 1000L),
      )
      db.roomUnitDao().updateHkState("u-101", HousekeepingState.CLEANING, 2000L)

      val unit = db.roomUnitDao().getById("u-101")
      assertNotNull(unit)
      assertEquals(Occupancy.OCCUPIED, unit?.occupancy)
      assertEquals(HousekeepingState.CLEANING, unit?.hkState)
      assertEquals(2000L, unit?.updatedAt)
      assertEquals(listOf("101"), db.roomUnitDao().observeAll().first().map { it.number })
      assertEquals(1, db.roomUnitDao().count())
      assertEquals("DLX", db.roomTypeDao().getAll().single().code)
    }

  @Test
  fun `checklist steps store by stable key and clear per unit`() =
    runTest {
      db.checklistDao().upsert(ChecklistStepEntity("u-101", TurnoverStep.BED_LINEN, 1000L, "Ayesha"))
      db.checklistDao().upsert(ChecklistStepEntity("u-101", TurnoverStep.VACUUM_MOP, 1000L, "Ayesha"))
      assertEquals(2, db.checklistDao().getForUnit("u-101").size)

      db.checklistDao().removeStep("u-101", TurnoverStep.BED_LINEN)
      assertEquals(
        listOf(TurnoverStep.VACUUM_MOP),
        db.checklistDao().getForUnit("u-101").map { it.step },
      )
      db.checklistDao().clearUnit("u-101")
      assertTrue(db.checklistDao().getForUnit("u-101").isEmpty())
    }

  @Test
  fun `tickets persist with photo ref and status updates`() =
    runTest {
      db.ticketDao().insert(
        TicketEntity(
          "t-1",
          "u-101",
          "101",
          TicketCategory.PLUMBING,
          "Tap leaks",
          "p1.jpg",
          TicketStatus.OPEN,
          "Ayesha",
          1000L,
          1000L,
        ),
      )
      assertEquals(1, db.ticketDao().observeOpen().first().size)
      db.ticketDao().updateStatus("t-1", TicketStatus.DONE, 2000L)
      assertEquals(TicketStatus.DONE, db.ticketDao().getById("t-1")?.status)
      assertTrue(db.ticketDao().observeOpen().first().isEmpty())
    }

  @Test
  fun `activity log returns newest first`() =
    runTest {
      db.activityDao().insert(
        ActivityEntity(
          type = ActivityType.STATUS_CHANGED,
          actorName = "Ayesha",
          role = StaffRole.HOUSEKEEPER,
          unitId = "u-101",
          roomNumber = "101",
          detail = "DIRTY>CLEANING",
          createdAt = 1000L,
        ),
      )
      db.activityDao().insert(
        ActivityEntity(
          type = ActivityType.CHECKLIST_COMPLETED,
          actorName = "Ayesha",
          role = StaffRole.HOUSEKEEPER,
          unitId = "u-101",
          roomNumber = "101",
          detail = null,
          createdAt = 2000L,
        ),
      )
      val recent = db.activityDao().getRecent(10)
      assertEquals(
        listOf(ActivityType.CHECKLIST_COMPLETED, ActivityType.STATUS_CHANGED),
        recent.map { it.type },
      )
      assertEquals(2, db.activityDao().observeRecent(10).first().size)
    }

  @Test
  fun `sync queue SQL orders by dueAt then fifo and skips future entries`() =
    runTest {
      val queue = db.syncQueueDao()
      queue.enqueue(entry(SyncEntityType.TICKET, "t-9", createdAt = 3000L, dueAt = 3000L))
      queue.enqueue(entry(SyncEntityType.UNIT_STATUS, "u-1", createdAt = 1000L, dueAt = 1000L))
      queue.enqueue(entry(SyncEntityType.UNIT_STATUS, "u-2", createdAt = 1000L, dueAt = 1000L))
      queue.enqueue(entry(SyncEntityType.CHECKLIST, "u-1", createdAt = 4000L, dueAt = 999_000L))

      val due = queue.peekDue(now = 5000L, limit = 10)
      assertEquals(listOf("u-1", "u-2", "t-9"), due.map { it.entityId })
      assertEquals(4, queue.pendingCount())
      assertEquals(1, queue.unsyncedCountFor(SyncEntityType.UNIT_STATUS, "u-1"))
    }

  @Test
  fun `sync queue tracks in-flight stale recovery and failure`() =
    runTest {
      val queue = db.syncQueueDao()
      val id = queue.enqueue(entry(SyncEntityType.UNIT_STATUS, "u-1", createdAt = 1000L, dueAt = 1000L))
      queue.markInFlight(listOf(id), now = 2000L)
      assertTrue(queue.peekDue(now = 5000L, limit = 10).isEmpty())

      assertEquals(1, queue.resetStaleInFlight(cutoff = 3000L, now = 3000L))
      assertEquals(1, queue.peekDue(now = 5000L, limit = 10).size)

      queue.reschedule(id, attempts = 1, dueAt = 60_000L, now = 3000L)
      assertTrue(queue.peekDue(now = 5000L, limit = 10).isEmpty())
      assertEquals(1, queue.peekDue(now = 60_000L, limit = 10).size)

      queue.markFailed(id, attempts = 10, now = 60_000L)
      assertEquals(0, queue.pendingCount())
      assertEquals(0, queue.unsyncedCountFor(SyncEntityType.UNIT_STATUS, "u-1"))

      val id2 = queue.enqueue(entry(SyncEntityType.TICKET, "t-1", createdAt = 1000L, dueAt = 1000L))
      queue.markDelivered(id2)
      assertEquals(0, queue.pendingCount())
    }

  @Test
  fun `converters round trip every stored enum`() {
    val converters = RoomConverters()
    for (state in HousekeepingState.entries) {
      assertEquals(state, converters.toHousekeepingState(converters.fromHousekeepingState(state)))
    }
    for (occupancy in Occupancy.entries) {
      assertEquals(occupancy, converters.toOccupancy(converters.fromOccupancy(occupancy)))
    }
    for (step in TurnoverStep.entries) {
      assertEquals(step, converters.toTurnoverStep(converters.fromTurnoverStep(step)))
    }
    for (category in TicketCategory.entries) {
      assertEquals(category, converters.toTicketCategory(converters.fromTicketCategory(category)))
    }
    assertEquals(
      listOf(
        TicketCategory.PLUMBING,
        TicketCategory.ELECTRICAL,
        TicketCategory.AIR_CONDITIONING,
        TicketCategory.FURNITURE,
        TicketCategory.OTHER,
      ),
      TicketCategory.entries,
    )
    // Retired values from older installs degrade instead of crashing reads.
    assertEquals(TicketCategory.OTHER, converters.toTicketCategory("LINEN"))
    assertEquals(TicketCategory.OTHER, converters.toTicketCategory("MINIBAR"))
    assertEquals(TicketCategory.OTHER, converters.toTicketCategory("BOGUS"))
    for (status in TicketStatus.entries) {
      assertEquals(status, converters.toTicketStatus(converters.fromTicketStatus(status)))
    }
    for (type in ActivityType.entries) {
      assertEquals(type, converters.toActivityType(converters.fromActivityType(type)))
    }
    for (role in StaffRole.entries) {
      assertEquals(role, converters.toStaffRole(converters.fromStaffRole(role)))
    }
    for (entity in SyncEntityType.entries) {
      assertEquals(entity, converters.toSyncEntityType(converters.fromSyncEntityType(entity)))
    }
    for (op in SyncOp.entries) {
      assertEquals(op, converters.toSyncOp(converters.fromSyncOp(op)))
    }
    for (status in SyncEntryStatus.entries) {
      assertEquals(status, converters.toSyncEntryStatus(converters.fromSyncEntryStatus(status)))
    }
  }

  @Test
  fun `english checklist labels are verbatim in fixed order`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val labels = TurnoverStep.entries.sortedBy { it.order }.map { context.getString(it.labelRes) }
    assertEquals(
      listOf(
        "Change bed linen",
        "Fresh towels & toiletries",
        "Vacuum & mop",
        "Bathroom deep clean",
        "Minibar restock",
        "AC/lights/sockets check",
        "Balcony tidy",
        "Final walkthrough",
      ),
      labels,
    )
  }

  @Test
  @Config(qualifiers = "bn")
  fun `bengali strings resolve for every new key`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val stepLabels = TurnoverStep.entries.map { context.getString(it.labelRes) }
    assertEquals(8, stepLabels.toSet().size)
    stepLabels.forEach { assertTrue(it.isNotBlank()) }
    assertEquals("বিছানার চাদর বদলান", stepLabels.first())

    val categories = TicketCategory.entries.map { context.getString(it.labelRes) }
    assertEquals(5, categories.toSet().size)
    categories.forEach { assertTrue(it.isNotBlank()) }

    assertEquals("হাউসকিপার", context.getString(StaffRole.HOUSEKEEPER.labelRes))
    assertEquals("সুপারভাইজার", context.getString(StaffRole.SUPERVISOR.labelRes))
  }

  private fun entry(
    entityType: SyncEntityType,
    entityId: String,
    createdAt: Long,
    dueAt: Long,
  ): SyncMutationEntity =
    SyncMutationEntity(
      entityType = entityType,
      entityId = entityId,
      op = SyncOp.UPSERT,
      payloadJson = "{}",
      createdAt = createdAt,
      dueAt = dueAt,
      updatedAt = createdAt,
    )
}
