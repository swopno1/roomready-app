package com.vivereply.roomready.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vivereply.roomready.data.sync.SyncEntityType

@Dao
interface SyncQueueDao {
  @Insert
  suspend fun enqueue(mutation: SyncMutationEntity): Long

  /**
   * Due entries in send order: earliest dueAt first, FIFO within a dueAt.
   * Must match [com.vivereply.roomready.data.sync.SyncRetryPolicy.orderByDueAt].
   */
  @Query(
    "SELECT * FROM sync_queue WHERE status = 'PENDING' AND dueAt <= :now " +
      "ORDER BY dueAt ASC, id ASC LIMIT :limit",
  )
  suspend fun peekDue(now: Long, limit: Int): List<SyncMutationEntity>

  @Query("UPDATE sync_queue SET status = 'IN_FLIGHT', updatedAt = :now WHERE id IN (:ids)")
  suspend fun markInFlight(ids: List<Long>, now: Long)

  @Query("DELETE FROM sync_queue WHERE id = :id")
  suspend fun markDelivered(id: Long)

  @Query(
    "UPDATE sync_queue SET status = 'PENDING', attempts = :attempts, " +
      "dueAt = :dueAt, updatedAt = :now WHERE id = :id",
  )
  suspend fun reschedule(id: Long, attempts: Int, dueAt: Long, now: Long)

  @Query(
    "UPDATE sync_queue SET status = 'FAILED', attempts = :attempts, " +
      "updatedAt = :now WHERE id = :id",
  )
  suspend fun markFailed(id: Long, attempts: Int, now: Long)

  @Query(
    "UPDATE sync_queue SET status = 'PENDING', dueAt = :now, updatedAt = :now " +
      "WHERE status = 'IN_FLIGHT' AND updatedAt < :cutoff",
  )
  suspend fun resetStaleInFlight(cutoff: Long, now: Long): Int

  /** Entries still awaiting PMS acknowledgement for one entity. */
  @Query(
    "SELECT COUNT(*) FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId " +
      "AND status IN ('PENDING', 'IN_FLIGHT')",
  )
  suspend fun unsyncedCountFor(entityType: SyncEntityType, entityId: String): Int

  @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
  suspend fun pendingCount(): Int
}
