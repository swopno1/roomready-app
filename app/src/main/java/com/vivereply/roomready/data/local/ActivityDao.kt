package com.vivereply.roomready.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
  @Insert
  suspend fun insert(event: ActivityEntity): Long

  @Query("SELECT * FROM activity_log ORDER BY createdAt DESC, id DESC LIMIT :limit")
  suspend fun getRecent(limit: Int): List<ActivityEntity>

  @Query("SELECT * FROM activity_log ORDER BY createdAt DESC, id DESC LIMIT :limit")
  fun observeRecent(limit: Int): Flow<List<ActivityEntity>>

  /** Latest supervisor send-back for one unit, so the redo shows its reason. */
  @Query(
    "SELECT * FROM activity_log WHERE type = 'SENT_BACK' AND unitId = :unitId " +
      "ORDER BY createdAt DESC, id DESC LIMIT 1",
  )
  suspend fun lastSentBack(unitId: String): ActivityEntity?
}
