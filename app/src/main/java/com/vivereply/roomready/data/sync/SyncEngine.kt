package com.vivereply.roomready.data.sync

import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.pms.PmsApiException
import com.vivereply.roomready.data.pms.PmsAuthException
import com.vivereply.roomready.domain.model.ActivityType
import kotlinx.coroutines.CancellationException

/**
 * Drains due sync-queue entries through a [PmsTransport].
 *
 * Offline behavior: with no connectivity the transport throws, every entry
 * is rescheduled with backoff, and all local state stays intact. Local
 * repository actions never call the engine; they only enqueue.
 *
 * A PMS refusal (parkable 4xx, e.g. HTTP 400) parks that entry with its
 * reason and flags it on the activity log; the rest of the batch still
 * syncs. Missing local credentials abort the drain without touching entries.
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
    val parked: Int = 0,
    val authAborted: Boolean = false,
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
    var parked = 0
    for ((index, entry) in due.withIndex()) {
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
      } catch (auth: PmsAuthException) {
        // No entry is at fault: return this and later entries to pending untouched.
        for (rest in due.drop(index)) {
          queue.reschedule(rest.id, rest.attempts, attemptNow, attemptNow)
        }
        return DrainResult(delivered, rescheduled, failed, parked, authAborted = true)
      } catch (api: PmsApiException) {
        if (api.parkable()) {
          parkRefused(entry, api, attemptNow)
          parked++
        } else {
          if (retryOrFail(entry, attemptNow)) rescheduled++ else failed++
        }
      } catch (e: Exception) {
        // Any other transport/server failure reschedules with backoff; entries are never lost.
        if (retryOrFail(entry, attemptNow)) rescheduled++ else failed++
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
    return DrainResult(delivered, rescheduled, failed, parked)
  }

  /** Returns true when rescheduled, false when moved to FAILED. */
  private suspend fun retryOrFail(entry: SyncMutationEntity, now: Long): Boolean {
    val attempts = entry.attempts + 1
    return if (SyncRetryPolicy.shouldRetry(attempts)) {
      queue.reschedule(entry.id, attempts, SyncRetryPolicy.nextDueAt(now, attempts), now)
      true
    } else {
      queue.markFailed(entry.id, attempts, now)
      false
    }
  }

  private suspend fun parkRefused(entry: SyncMutationEntity, api: PmsApiException, now: Long) {
    val reason = "HTTP ${api.statusCode}"
    queue.park(entry.id, reason, now)
    activity.insert(
      ActivityEntity(
        type = ActivityType.PUSH_PARKED,
        actorName = SYSTEM_ACTOR,
        role = null,
        unitId = entry.unitIdOrNull(),
        roomNumber = null,
        detail = "${entry.entityType} ${entry.entityId} $reason",
        createdAt = now,
      ),
    )
  }

  private fun SyncMutationEntity.unitIdOrNull(): String? =
    when (entityType) {
      SyncEntityType.UNIT_STATUS, SyncEntityType.CHECKLIST -> entityId
      SyncEntityType.TICKET, SyncEntityType.TICKET_STATUS -> null
    }

  companion object {
    const val SYSTEM_ACTOR: String = "system"
  }
}
