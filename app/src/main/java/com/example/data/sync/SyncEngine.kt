package com.example.data.sync

import com.example.data.Clock
import com.example.data.local.ActivityDao
import com.example.data.local.ActivityEntity
import com.example.data.local.SyncQueueDao
import com.example.domain.model.ActivityType
import kotlinx.coroutines.CancellationException

/**
 * Drains due sync-queue entries through a [PmsTransport].
 *
 * Offline behavior: with no connectivity the transport throws, every entry
 * is rescheduled with backoff, and all local state stays intact. Local
 * repository actions never call the engine; they only enqueue.
 */
class SyncEngine(
  private val queue: SyncQueueDao,
  private val activity: ActivityDao,
  private val transport: PmsTransport,
  private val clock: Clock,
) {

  data class DrainResult(
    val delivered: Int,
    val rescheduled: Int,
    val failed: Int,
  )

  suspend fun drain(limit: Int = SyncRetryPolicy.DEFAULT_DRAIN_LIMIT): DrainResult {
    val now = clock.nowEpochMillis()
    queue.resetStaleInFlight(now - SyncRetryPolicy.STALE_IN_FLIGHT_MILLIS, now)
    val due = queue.peekDue(now, limit)
    if (due.isEmpty()) return DrainResult(0, 0, 0)

    queue.markInFlight(due.map { it.id }, now)
    var delivered = 0
    var rescheduled = 0
    var failed = 0
    for (entry in due) {
      val attemptNow = clock.nowEpochMillis()
      try {
        transport.send(
          OutgoingMutation(
            queueId = entry.id,
            entityType = entry.entityType,
            entityId = entry.entityId,
            op = entry.op,
            payloadJson = entry.payloadJson,
          ),
        )
        queue.markDelivered(entry.id)
        delivered++
      } catch (cancelled: CancellationException) {
        queue.reschedule(entry.id, entry.attempts, attemptNow, attemptNow)
        throw cancelled
      } catch (e: Exception) {
        // Any transport/server failure reschedules with backoff; entries are never lost.
        val attempts = entry.attempts + 1
        if (SyncRetryPolicy.shouldRetry(attempts)) {
          queue.reschedule(entry.id, attempts, SyncRetryPolicy.nextDueAt(attemptNow, attempts), attemptNow)
          rescheduled++
        } else {
          queue.markFailed(entry.id, attempts, attemptNow)
          failed++
        }
      }
    }
    if (delivered > 0) {
      activity.insert(
        ActivityEntity(
          type = ActivityType.SYNC_COMPLETED,
          actorName = SYSTEM_ACTOR,
          role = null,
          unitId = null,
          roomNumber = null,
          detail = delivered.toString(),
          createdAt = clock.nowEpochMillis(),
        ),
      )
    }
    return DrainResult(delivered, rescheduled, failed)
  }

  companion object {
    const val SYSTEM_ACTOR: String = "system"
  }
}
