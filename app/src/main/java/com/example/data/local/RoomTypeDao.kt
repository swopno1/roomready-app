package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface RoomTypeDao {
  @Upsert
  suspend fun upsertAll(types: List<RoomTypeEntity>)

  @Query("SELECT * FROM room_types ORDER BY code ASC")
  suspend fun getAll(): List<RoomTypeEntity>
}
