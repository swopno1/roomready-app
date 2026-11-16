package com.example.data.sync

/**
 * Retry and ordering policy for the outbound sync queue.
 *
 * Ordering rule (mirrored by the DAO query): earliest [dueAt] first,
 * ties broken by ascending row id (FIFO). New entries are due immediately;
 * failed entries are rescheduled with exponential backoff.
 */
object SyncRetryPolicy {
  const val MAX_ATTEMPTS: Int = 10
  const val BASE_DELAY_MILLIS: Long = 30_000L
  const val MAX_DELAY_MILLIS: Long = 3_600_000L
  const val STALE_IN_FLIGHT_MILLIS: Long = 300_000L
  const val DEFAULT_DRAIN_LIMIT: Int = 50

  /** attempts starts at 1 for the first retry. Capped exponential backoff. */
  fun delayForAttempt(attempts: Int): Long {
    require(attempts >= 1) { "attempts must be >= 1" }
    var delay = BASE_DELAY_MILLIS
    repeat((attempts - 1).coerceAtMost(16)) {
      delay = (delay * 2L).coerceAtMost(MAX_DELAY_MILLIS)
    }
    return delay
  }

  fun shouldRetry(attempts: Int): Boolean = attempts < MAX_ATTEMPTS

  fun nextDueAt(nowEpochMillis: Long, attempts: Int): Long =
    nowEpochMillis + delayForAttempt(attempts)

  /**
   * In-memory equivalent of the queue ORDER BY (dueAt ASC, id ASC),
   * used by tests and any non-SQL queue implementation.
   */
  fun <T> orderByDueAt(items: List<T>, dueAt: (T) -> Long, id: (T) -> Long): List<T> =
    items.sortedWith(compareBy(dueAt, id))
}
