package com.vivereply.roomready.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StaffRosterDao {
  @Query("DELETE FROM staff_roster")
  suspend fun clear()

  @Upsert
  suspend fun upsertAll(staff: List<StaffRosterEntity>)

  @Query("SELECT * FROM staff_roster ORDER BY name ASC")
  suspend fun getAll(): List<StaffRosterEntity>
}
