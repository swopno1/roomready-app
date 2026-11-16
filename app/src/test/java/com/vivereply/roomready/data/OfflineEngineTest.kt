package com.vivereply.roomready.data

import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.data.local.ChecklistDao
import com.vivereply.roomready.data.local.ChecklistStepEntity
import com.vivereply.roomready.data.local.RoomTypeDao
import com.vivereply.roomready.data.local.RoomTypeEntity
import com.vivereply.roomready.data.local.RoomUnitDao
import com.vivereply.roomready.data.local.RoomUnitEntity
import com.vivereply.roomready.data.local.StaffRosterDao
import com.vivereply.roomready.data.local.StaffRosterEntity
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.local.TicketDao
import com.vivereply.roomready.data.local.TicketEntity
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.data.repository.TicketRepository
import com.vivereply.roomready.data.sync.OutgoingMutation
import com.vivereply.roomready.data.sync.PmsTransport
import com.vivereply.roomready.data.sync.SyncEngine
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncEntryStatus
import com.vivereply.roomready.data.sync.SyncRetryPolicy
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.domain.model.TurnoverStep
import com.vivereply.roomready.domain.state.HkTransitionError
import com.vivereply.roomready.domain.state.HkTransitionException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineEngineTest {

  private lateinit var units: FakeRoomUnitDao
  private lateinit var types: FakeRoomTypeDao
  private lateinit var roster: FakeStaffRosterDao
  private lateinit var steps: FakeChecklistDao
  private lateinit var tickets: FakeTicketDao
  private lateinit var activity: FakeActivityDao
  private lateinit var queue: FakeSyncQueueDao
  private lateinit var clock: FakeClock
  private lateinit var rooms: RoomRepository
  private lateinit var ticketRepo: TicketRepository

  private val housekeeper = StaffMember("Ayesha", StaffRole.HOUSEKEEPER)
  private val supervisor = StaffMember("Rahim", StaffRole.SUPERVISOR)

  @Before
  fun setUp() {
    units = FakeRoomUnitDao()
    types = FakeRoomTypeDao()
    roster = FakeStaffRosterDao()
    steps = FakeChecklistDao()
    tickets = FakeTicketDao()
    activity = FakeActivityDao()
    queue = FakeSyncQueueDao()
    clock = FakeClock()
    rooms = RoomRepository(units, types, roster, steps, activity, queue, clock)
    ticketRepo = TicketRepository(units, tickets, activity, queue, clock)
  }

  @Test
  fun `full turnover flow works offline end to end`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)

      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      assertEquals(HousekeepingState.CLEANING, rooms.getUnit("u-101")?.hkState)

      for (step in TurnoverStep.entries) {
        clock.advance(1_000L)
        assertSuccess(rooms.setStepDone("u-101", step, true, housekeeper))
      }
      assertTrue(rooms.progress("u-101").isComplete)

      assertSuccess(rooms.finishCleaning("u-101", housekeeper))
      assertEquals(HousekeepingState.CLEANED, rooms.getUnit("u-101")?.hkState)

      assertSuccess(rooms.inspect("u-101", supervisor))
      assertEquals(HousekeepingState.READY, rooms.getUnit("u-101")?.hkState)

      // 1 start + 8 steps + 1 finish + 1 inspect = 11 queued mutations.
      // Local actions take no transport parameter, so no network call is possible here.
      assertEquals(11, queue.pendingCount())
      assertTrue(activity.events.any { it.type.name == "CHECKLIST_COMPLETED" })
    }

  @Test
  fun `airplane mode performs every local action with zero network traffic`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      seedUnit(id = "u-102", number = "102", hk = HousekeepingState.CLEANED)
      val offlineTransport = RecordingTransport(failuresBeforeSuccess = Int.MAX_VALUE)

      // Every local action below succeeds with no transport involved (enqueue-only design).
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      assertSuccess(rooms.setStepDone("u-101", TurnoverStep.BED_LINEN, true, housekeeper))
      assertSuccess(rooms.setStepDone("u-101", TurnoverStep.BED_LINEN, false, housekeeper))
      assertSuccess(rooms.inspect("u-102", supervisor))
      assertSuccess(
        ticketRepo.raiseTicket("u-101", TicketCategory.PLUMBING, "Tap leaks", "p1.jpg", housekeeper),
      )
      val ticket = ticketRepo.observeOpenTickets().first().single()
      assertSuccess(ticketRepo.markDone(ticket.id, housekeeper))
      seedUnit(id = "u-103", number = "103", hk = HousekeepingState.CLEANED)
      assertSuccess(rooms.sendBack("u-103", supervisor, "dust on shelf"))

      val queuedBeforeDrain = queue.pendingCount()
      assertTrue(queuedBeforeDrain > 0)
      assertEquals(3, rooms.observeUnits().first().size)

      // Even an explicit sync attempt while offline loses nothing: all entries stay queued.
      val offlineDrain = SyncEngine(queue, activity, offlineTransport, clock).drain()
      assertEquals(0, offlineDrain.delivered)
      assertEquals(queuedBeforeDrain, offlineDrain.rescheduled)
      assertEquals(queuedBeforeDrain, queue.pendingCount())
      assertEquals(HousekeepingState.CLEANING, rooms.getUnit("u-101")?.hkState)
      assertEquals(HousekeepingState.READY, rooms.getUnit("u-102")?.hkState)
    }

  @Test
  fun `occupancy is read-only for local actions`() =
    runTest {
      seedUnit(
        id = "u-101",
        number = "101",
        hk = HousekeepingState.DIRTY,
        occupancy = Occupancy.OCCUPIED,
      )
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      for (step in TurnoverStep.entries) {
        assertSuccess(rooms.setStepDone("u-101", step, true, housekeeper))
      }
      assertSuccess(rooms.finishCleaning("u-101", housekeeper))
      assertSuccess(rooms.inspect("u-101", supervisor))
      assertEquals(Occupancy.OCCUPIED, rooms.getUnit("u-101")?.occupancy)
    }

  @Test
  fun `finish is blocked until all eight steps are done`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      TurnoverStep.entries.dropLast(1).forEach { step ->
        assertSuccess(rooms.setStepDone("u-101", step, true, housekeeper))
      }
      val result = rooms.finishCleaning("u-101", housekeeper)
      assertTrue(result.isFailure)
      val error = (result.exceptionOrNull() as HkTransitionException).error
      assertEquals(HkTransitionError.ChecklistIncomplete(missingSteps = 1), error)
      assertEquals(HousekeepingState.CLEANING, rooms.getUnit("u-101")?.hkState)
    }

  @Test
  fun `checklist steps are locked outside cleaning`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      val result = rooms.setStepDone("u-101", TurnoverStep.BED_LINEN, true, housekeeper)
      assertTrue(result.isFailure)
    }

  @Test
  fun `send back clears checklist so redo is fresh`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      for (step in TurnoverStep.entries) {
        assertSuccess(rooms.setStepDone("u-101", step, true, housekeeper))
      }
      assertSuccess(rooms.finishCleaning("u-101", housekeeper))
      assertSuccess(rooms.sendBack("u-101", supervisor, "dust on shelf"))

      assertEquals(HousekeepingState.DIRTY, rooms.getUnit("u-101")?.hkState)
      assertEquals(0, rooms.progress("u-101").doneCount)
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      assertTrue(rooms.finishCleaning("u-101", housekeeper).isFailure)
    }

  @Test
  fun `ticket raise validates unit note and photo`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      assertTrue(
        ticketRepo.raiseTicket("missing", TicketCategory.LINEN, "note", null, housekeeper).isFailure,
      )
      assertTrue(
        ticketRepo.raiseTicket("u-101", TicketCategory.LINEN, "   ", null, housekeeper).isFailure,
      )
      val raised =
        ticketRepo.raiseTicket("u-101", TicketCategory.MINIBAR, "  restock cola ", null, housekeeper)
      assertSuccess(raised)
      assertEquals("restock cola", raised.getOrNull()?.note)
      assertEquals("101", raised.getOrNull()?.roomNumber)
      assertEquals(null, raised.getOrNull()?.photoRef)
    }

  @Test
  fun `ticket done is one way`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      val ticket =
        ticketRepo
          .raiseTicket("u-101", TicketCategory.ELECTRICAL, "socket sparking", null, housekeeper)
          .getOrThrow()
      assertSuccess(ticketRepo.markDone(ticket.id, housekeeper))
      assertEquals(TicketStatus.DONE, ticketRepo.getTicket(ticket.id)?.status)
      assertTrue(ticketRepo.markDone(ticket.id, housekeeper).isFailure)
      assertEquals(0, ticketRepo.observeOpenTickets().first().size)
    }

  @Test
  fun `bootstrap preserves local cleaning when pms reports dirty`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      assertSuccess(rooms.startCleaning("u-101", housekeeper))

      val result =
        rooms.importBootstrap(
          roomTypes = listOf(roomType()),
          roomUnits =
            listOf(
              RoomUnit(
                "u-101",
                "101",
                "t-dlx",
                "1",
                Occupancy.VACANT,
                HousekeepingState.DIRTY,
                999L,
              ),
            ),
        )
      assertEquals(1, result.hkPreserved)
      // Local CLEANING kept (PMS has no in-progress state), occupancy refreshed from PMS.
      assertEquals(HousekeepingState.CLEANING, rooms.getUnit("u-101")?.hkState)
      assertEquals(Occupancy.VACANT, rooms.getUnit("u-101")?.occupancy)
    }

  @Test
  fun `bootstrap applies pms state when nothing local is unsynced`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      val result =
        rooms.importBootstrap(
          roomTypes = listOf(roomType()),
          roomUnits =
            listOf(
              RoomUnit(
                "u-101",
                "101",
                "t-dlx",
                "1",
                Occupancy.VACANT,
                HousekeepingState.CLEANED,
                999L,
              ),
            ),
        )
      assertEquals(0, result.hkPreserved)
      assertEquals(HousekeepingState.CLEANED, rooms.getUnit("u-101")?.hkState)
    }

  @Test
  fun `sync engine delivers due entries in fifo order`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      clock.advance(5_000L)
      assertSuccess(
        ticketRepo.raiseTicket("u-101", TicketCategory.LINEN, "no towels", null, housekeeper),
      )
      val transport = RecordingTransport()
      val engine = SyncEngine(queue, activity, transport, clock)

      val result = engine.drain()
      assertEquals(2, result.delivered)
      assertEquals(0, queue.pendingCount())
      assertEquals(
        listOf(SyncEntityType.UNIT_STATUS, SyncEntityType.TICKET),
        transport.sent.map { it.entityType },
      )
    }

  @Test
  fun `sync engine reschedules failures with backoff then delivers`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      val transport = RecordingTransport(failuresBeforeSuccess = 1)
      val engine = SyncEngine(queue, activity, transport, clock)

      val first = engine.drain()
      assertEquals(0, first.delivered)
      assertEquals(1, first.rescheduled)
      // Not due again until backoff elapses.
      assertEquals(0, engine.drain().delivered)

      clock.advance(SyncRetryPolicy.BASE_DELAY_MILLIS)
      val second = engine.drain()
      assertEquals(1, second.delivered)
      assertEquals(0, queue.pendingCount())
    }

  @Test
  fun `sync engine parks entries as failed after max attempts`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      val transport = RecordingTransport(failuresBeforeSuccess = Int.MAX_VALUE)
      val engine = SyncEngine(queue, activity, transport, clock)

      var failed = 0
      repeat(SyncRetryPolicy.MAX_ATTEMPTS + 2) {
        clock.advance(SyncRetryPolicy.MAX_DELAY_MILLIS)
        failed += engine.drain().failed
      }
      assertEquals(1, failed)
      assertEquals(0, queue.pendingCount())
      assertEquals(SyncEntryStatus.FAILED, queue.all().single().status)
    }

  @Test
  fun `sync engine recovers stale in-flight entries`() =
    runTest {
      seedUnit(id = "u-101", number = "101", hk = HousekeepingState.DIRTY)
      assertSuccess(rooms.startCleaning("u-101", housekeeper))
      val id = queue.all().single().id
      queue.markInFlight(listOf(id), clock.nowEpochMillis())
      clock.advance(SyncRetryPolicy.STALE_IN_FLIGHT_MILLIS + 1)

      val engine = SyncEngine(queue, activity, RecordingTransport(), clock)
      assertEquals(1, engine.drain().delivered)
    }

  private suspend fun seedUnit(
    id: String,
    number: String,
    hk: HousekeepingState,
    occupancy: Occupancy = Occupancy.VACANT,
  ) {
    types.upsertAll(listOf(RoomTypeEntity("t-dlx", "DLX", "Deluxe", "ডিলাক্স", "1 king", 2)))
    units.upsert(
      RoomUnitEntity(id, number, "t-dlx", "1", occupancy, hk, clock.nowEpochMillis()),
    )
  }

  private fun roomType(): RoomType =
    RoomType("t-dlx", "DLX", "Deluxe", "ডিলাক্স", "1 king", 2)

  private fun <T> assertSuccess(result: Result<T>) {
    assertTrue("Expected success but got $result", result.isSuccess)
  }

  // ---- Fakes ----

  private class FakeClock(var now: Long = 1_000_000L) : Clock {
    override fun nowEpochMillis(): Long = now

    fun advance(millis: Long) {
      now += millis
    }
  }

  private class FakeRoomUnitDao : RoomUnitDao {
    private val store = mutableMapOf<String, RoomUnitEntity>()
    private val flow = MutableStateFlow<List<RoomUnitEntity>>(emptyList())

    override fun observeAll(): Flow<List<RoomUnitEntity>> = flow

    override suspend fun getById(id: String): RoomUnitEntity? = store[id]

    override suspend fun upsert(unit: RoomUnitEntity) {
      store[unit.id] = unit
      emit()
    }

    override suspend fun upsertAll(units: List<RoomUnitEntity>) {
      units.forEach { store[it.id] = it }
      emit()
    }

    override suspend fun updateHkState(id: String, state: HousekeepingState, updatedAt: Long) {
      store[id]?.let { store[id] = it.copy(hkState = state, updatedAt = updatedAt) }
      emit()
    }

    override suspend fun count(): Int = store.size

    private fun emit() {
      flow.value = store.values.sortedBy { it.number }
    }
  }

  private class FakeRoomTypeDao : RoomTypeDao {
    private val store = mutableMapOf<String, RoomTypeEntity>()

    override suspend fun upsertAll(types: List<RoomTypeEntity>) {
      types.forEach { store[it.id] = it }
    }

    override suspend fun getAll(): List<RoomTypeEntity> = store.values.sortedBy { it.code }
  }

  private class FakeStaffRosterDao : StaffRosterDao {
    private val store = mutableMapOf<String, StaffRosterEntity>()

    override suspend fun clear() {
      store.clear()
    }

    override suspend fun upsertAll(staff: List<StaffRosterEntity>) {
      staff.forEach { store[it.name] = it }
    }

    override suspend fun getAll(): List<StaffRosterEntity> = store.values.sortedBy { it.name }
  }

  private class FakeChecklistDao : ChecklistDao {
    private val store = mutableMapOf<Pair<String, TurnoverStep>, ChecklistStepEntity>()

    override suspend fun getForUnit(unitId: String): List<ChecklistStepEntity> =
      store.filterKeys { it.first == unitId }.values.toList()

    override suspend fun upsert(step: ChecklistStepEntity) {
      store[step.unitId to step.step] = step
    }

    override suspend fun removeStep(unitId: String, step: TurnoverStep) {
      store.remove(unitId to step)
    }

    override suspend fun clearUnit(unitId: String) {
      store.keys.removeAll { it.first == unitId }
    }
  }

  private class FakeTicketDao : TicketDao {
    private val store = mutableMapOf<String, TicketEntity>()
    private val openFlow = MutableStateFlow<List<TicketEntity>>(emptyList())

    override fun observeOpen(): Flow<List<TicketEntity>> = openFlow

    override suspend fun getById(id: String): TicketEntity? = store[id]

    override suspend fun insert(ticket: TicketEntity) {
      store[ticket.id] = ticket
      emit()
    }

    override suspend fun updateStatus(id: String, status: TicketStatus, updatedAt: Long) {
      store[id]?.let { store[id] = it.copy(status = status, updatedAt = updatedAt) }
      emit()
    }

    private fun emit() {
      openFlow.value =
        store.values.filter { it.status == TicketStatus.OPEN }.sortedBy { it.createdAt }
    }
  }

  private class FakeActivityDao : ActivityDao {
    val events = mutableListOf<ActivityEntity>()
    private var nextId = 1L

    override suspend fun insert(event: ActivityEntity): Long {
      val id = nextId++
      events += event.copy(id = id)
      return id
    }

    override suspend fun getRecent(limit: Int): List<ActivityEntity> =
      events.sortedWith(compareByDescending<ActivityEntity> { it.createdAt }.thenByDescending { it.id })
        .take(limit)

    override fun observeRecent(limit: Int): Flow<List<ActivityEntity>> =
      MutableStateFlow(events.toList())
  }

  private class FakeSyncQueueDao : SyncQueueDao {
    private val store = mutableMapOf<Long, SyncMutationEntity>()
    private var nextId = 1L

    fun all(): List<SyncMutationEntity> = store.values.sortedBy { it.id }

    override suspend fun enqueue(mutation: SyncMutationEntity): Long {
      val id = nextId++
      store[id] = mutation.copy(id = id)
      return id
    }

    override suspend fun peekDue(now: Long, limit: Int): List<SyncMutationEntity> =
      SyncRetryPolicy.orderByDueAt(
        store.values.filter { it.status == SyncEntryStatus.PENDING && it.dueAt <= now },
        { it.dueAt },
        { it.id },
      ).take(limit)

    override suspend fun markInFlight(ids: List<Long>, now: Long) {
      ids.forEach { id ->
        store[id]?.let { store[id] = it.copy(status = SyncEntryStatus.IN_FLIGHT, updatedAt = now) }
      }
    }

    override suspend fun markDelivered(id: Long) {
      store.remove(id)
    }

    override suspend fun reschedule(id: Long, attempts: Int, dueAt: Long, now: Long) {
      store[id]?.let {
        store[id] = it.copy(status = SyncEntryStatus.PENDING, attempts = attempts, dueAt = dueAt, updatedAt = now)
      }
    }

    override suspend fun markFailed(id: Long, attempts: Int, now: Long) {
      store[id]?.let {
        store[id] = it.copy(status = SyncEntryStatus.FAILED, attempts = attempts, updatedAt = now)
      }
    }

    override suspend fun park(id: Long, error: String, now: Long) {
      store[id]?.let {
        store[id] = it.copy(status = SyncEntryStatus.PARKED, lastError = error, updatedAt = now)
      }
    }

    override suspend fun resetStaleInFlight(cutoff: Long, now: Long): Int {
      val stale =
        store.values.filter { it.status == SyncEntryStatus.IN_FLIGHT && it.updatedAt < cutoff }
      stale.forEach { entry ->
        store[entry.id] = entry.copy(status = SyncEntryStatus.PENDING, dueAt = now, updatedAt = now)
      }
      return stale.size
    }

    override suspend fun unsyncedCountFor(entityType: SyncEntityType, entityId: String): Int =
      store.values.count {
        it.entityType == entityType && it.entityId == entityId &&
          (it.status == SyncEntryStatus.PENDING || it.status == SyncEntryStatus.IN_FLIGHT)
      }

    override suspend fun pendingCount(): Int =
      store.values.count { it.status == SyncEntryStatus.PENDING }
  }

  private class RecordingTransport(private var failuresBeforeSuccess: Int = 0) : PmsTransport {
    val sent = mutableListOf<OutgoingMutation>()

    override suspend fun send(mutation: OutgoingMutation) {
      if (failuresBeforeSuccess > 0) {
        failuresBeforeSuccess--
        throw java.io.IOException("offline")
      }
      sent += mutation
    }
  }
}
