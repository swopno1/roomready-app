package com.vivereply.roomready.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivereply.roomready.domain.model.HousekeepingState
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomUnitDao {
  @Query("SELECT * FROM room_units ORDER BY number ASC")
  fun observeAll(): Flow<List<RoomUnitEntity>>

  @Query("SELECT * FROM room_units WHERE id = :id")
  suspend fun getById(id: String): RoomUnitEntity?

  @Upsert
  suspend fun upsert(unit: RoomUnitEntity)

  @Upsert
  suspend fun upsertAll(units: List<RoomUnitEntity>)

  @Query("UPDATE room_units SET hkState = :state, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateHkState(id: String, state: HousekeepingState, updatedAt: Long)

  @Query("SELECT COUNT(*) FROM room_units")
  suspend fun count(): Int
}
