package com.vivereply.roomready.data.sync

import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.pms.PmsApiException
import com.vivereply.roomready.data.pms.PmsAuthException
import com.vivereply.roomready.domain.model.ActivityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncParkTest {

  private lateinit var queue: FakeQueue
  private lateinit var activity: FakeActivity
  private lateinit var clock: FakeClock

  @Before
  fun setUp() {
    queue = FakeQueue()
    activity = FakeActivity()
    clock = FakeClock()
  }

  @Test
  fun `a 400 parks the bad entry flags activity and delivers the rest`() =
    runTest {
      val bad = enqueue(SyncEntityType.TICKET, "t-bad")
      val good = enqueue(SyncEntityType.UNIT_STATUS, "u-101")
      val transport = ScriptedTransport(failWith = mapOf(bad to 400))
      val engine = SyncEngine(queue, activity, transport, clock)

      val result = engine.drain()
      assertEquals(1, result.delivered)
      assertEquals(1, result.parked)
      assertEquals(0, result.rescheduled)
      assertEquals(SyncEntryStatus.PARKED, queue.statusOf(bad))
      assertEquals("HTTP 400", queue.errorOf(bad))
      assertEquals(null, queue.statusOf(good))
      val flag = activity.events.single { it.type == ActivityType.PUSH_PARKED }
      assertTrue(flag.detail?.contains("t-bad") == true)
      assertTrue(flag.detail?.contains("HTTP 400") == true)
    }

  @Test
  fun `parked entries are never retried`() =
    runTest {
      val bad = enqueue(SyncEntityType.TICKET, "t-bad")
      val engine = SyncEngine(queue, activity, ScriptedTransport(failWith = mapOf(bad to 400)), clock)
      engine.drain()
      clock.advance(10_000_000L)
      val second = engine.drain()
      assertEquals(0, second.delivered)
      assertEquals(0, second.parked)
      assertEquals(0, queue.pendingCount())
    }

  @Test
  fun `server errors still reschedule next to a parked entry`() =
    runTest {
      val bad = enqueue(SyncEntityType.TICKET, "t-bad")
      val flaky = enqueue(SyncEntityType.UNIT_STATUS, "u-101")
      val engine = SyncEngine(queue, activity, ScriptedTransport(failWith = mapOf(bad to 400, flaky to 500)), clock)

      val result = engine.drain()
      assertEquals(1, result.parked)
      assertEquals(1, result.rescheduled)
      assertEquals(SyncEntryStatus.PENDING, queue.statusOf(flaky))
    }

  @Test
  fun `missing credentials abort the drain without touching entries`() =
    runTest {
      val first = enqueue(SyncEntityType.UNIT_STATUS, "u-101")
      val second = enqueue(SyncEntityType.TICKET, "t-1")
      val engine = SyncEngine(queue, activity, AuthFailingTransport(), clock)

      val result = engine.drain()
      assertTrue(result.authAborted)
      assertEquals(0, result.parked)
      assertEquals(2, queue.pendingCount())
      assertEquals(SyncEntryStatus.PENDING, queue.statusOf(first))
      assertEquals(SyncEntryStatus.PENDING, queue.statusOf(second))
    }

  private suspend fun enqueue(type: SyncEntityType, entityId: String): Long =
    queue.enqueue(
      SyncMutationEntity(
        entityType = type,
        entityId = entityId,
        op = SyncOp.UPSERT,
        payloadJson = "{}",
        createdAt = clock.now,
        dueAt = clock.now,
        updatedAt = clock.now,
      ),
    )

  private class FakeClock(var now: Long = 1_000_000L) : Clock {
    override fun nowEpochMillis(): Long = now

    fun advance(millis: Long) {
      now += millis
    }
  }

  private class FakeQueue : SyncQueueDao {
    private val store = mutableMapOf<Long, SyncMutationEntity>()
    private var nextId = 1L

    fun statusOf(id: Long): SyncEntryStatus? = store[id]?.status

    fun errorOf(id: Long): String? = store[id]?.lastError

    override suspend fun enqueue(mutation: SyncMutationEntity): Long {
      val id = nextId++
      store[id] = mutation.copy(id = id)
      return id
    }

    override suspend fun peekDue(now: Long, limit: Int): List<SyncMutationEntity> =
      store.values.filter { it.status == SyncEntryStatus.PENDING && it.dueAt <= now }
        .sortedWith(compareBy({ it.dueAt }, { it.id })).take(limit)

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

    override suspend fun resetStaleInFlight(cutoff: Long, now: Long): Int = 0

    override suspend fun unsyncedCountFor(entityType: SyncEntityType, entityId: String): Int = 0

    override suspend fun pendingCount(): Int =
      store.values.count { it.status == SyncEntryStatus.PENDING }
  }

  private class FakeActivity : ActivityDao {
    val events = mutableListOf<ActivityEntity>()

    override suspend fun insert(event: ActivityEntity): Long {
      events += event.copy(id = (events.size + 1).toLong())
      return events.size.toLong()
    }

    override suspend fun getRecent(limit: Int): List<ActivityEntity> = events.takeLast(limit)

    override fun observeRecent(limit: Int): Flow<List<ActivityEntity>> =
      MutableStateFlow(events.toList())

    override suspend fun lastSentBack(unitId: String): ActivityEntity? =
      events.filter { it.type == ActivityType.SENT_BACK && it.unitId == unitId }
        .maxWithOrNull(compareBy({ it.createdAt }, { it.id }))
  }

  private class ScriptedTransport(private val failWith: Map<Long, Int>) : PmsTransport {
    override suspend fun send(mutation: OutgoingMutation) {
      val code = failWith[mutation.queueId] ?: return
      throw PmsApiException("POST outbound failed", code)
    }
  }

  private class AuthFailingTransport : PmsTransport {
    override suspend fun send(mutation: OutgoingMutation) {
      throw PmsAuthException("missing api key")
    }
  }
}
