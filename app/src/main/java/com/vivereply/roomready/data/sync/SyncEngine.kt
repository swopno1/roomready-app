package com.vivereply.roomready.data.sync

import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.pms.DeferEntryException
import com.vivereply.roomready.data.pms.PmsApiException
import com.vivereply.roomready.data.pms.PmsAuthException
import com.vivereply.roomready.data.pms.PmsRefusedException
import com.vivereply.roomready.domain.model.ActivityType
import kotlinx.coroutines.CancellationException

/**
 * Drains due sync-queue entries through a [PmsTransport].
 *
 * Offline behavior: with no connectivity the transport throws, every entry
 * is rescheduled with backoff, and all local state stays intact. Local
 * repository actions never call the engine; they only enqueue.
 *
 * A PMS refusal parks that entry with its reason and flags it on the
 * activity log; the rest of the batch still syncs. A 401 stops the whole
 * drain (the orchestrator wipes the revoked key); a 409 also schedules a
 * bootstrap refresh. Deferred entries (ticket-done ahead of its create)
 * return to pending untouched. Missing local credentials abort the drain
 * without touching entries.
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
    val authRevoked: Boolean = false,
    val refreshScheduled: Boolean = false,
    val deferred: Int = 0,
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
    var deferred = 0
    var refreshScheduled = false
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
        abortBatch(due, index, attemptNow)
        return DrainResult(delivered, rescheduled, failed, parked, authAborted = true)
      } catch (deferredEntry: DeferEntryException) {
        // Not a failure: still due now, attempts untouched, retried on a later pass.
        queue.reschedule(entry.id, entry.attempts, attemptNow, attemptNow)
        deferred++
      } catch (refused: PmsRefusedException) {
        parkDetailed(entry, refused.lastError, refused.flagDetail, attemptNow)
        parked++
        if (refused.httpStatus == 409) refreshScheduled = true
      } catch (api: PmsApiException) {
        if (api.statusCode == 401) {
          // Key wrong or revoked: stop everything, keep every entry queued.
          abortBatch(due, index, attemptNow)
          return DrainResult(
            delivered,
            rescheduled,
            failed,
            parked,
            authRevoked = true,
            refreshScheduled = refreshScheduled,
            deferred = deferred,
          )
        }
        if (api.parkable()) {
          // Fallback for transports that throw raw API errors (tests).
          val reason = "HTTP ${api.statusCode}"
          parkDetailed(
            entry,
            reason,
            "${entry.entityType} ${entry.entityId} $reason",
            attemptNow,
          )
          parked++
          if (api.statusCode == 409) refreshScheduled = true
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
    return DrainResult(
      delivered,
      rescheduled,
      failed,
      parked,
      refreshScheduled = refreshScheduled,
      deferred = deferred,
    )
  }

  private suspend fun abortBatch(
    due: List<SyncMutationEntity>,
    index: Int,
    now: Long,
  ) {
    for (rest in due.drop(index)) {
      queue.reschedule(rest.id, rest.attempts, now, now)
    }
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

  private suspend fun parkDetailed(
    entry: SyncMutationEntity,
    lastError: String,
    flagDetail: String,
    now: Long,
  ) {
    queue.park(entry.id, lastError, now)
    activity.insert(
      ActivityEntity(
        type = ActivityType.PUSH_PARKED,
        actorName = SYSTEM_ACTOR,
        role = null,
        unitId = entry.unitIdOrNull(),
        roomNumber = null,
        detail = flagDetail,
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
