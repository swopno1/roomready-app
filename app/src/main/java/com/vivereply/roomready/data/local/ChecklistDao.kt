package com.vivereply.roomready.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivereply.roomready.domain.model.TurnoverStep

@Dao
interface ChecklistDao {
  @Query("SELECT * FROM checklist_steps WHERE unitId = :unitId")
  suspend fun getForUnit(unitId: String): List<ChecklistStepEntity>

  @Upsert
  suspend fun upsert(step: ChecklistStepEntity)

  @Query("DELETE FROM checklist_steps WHERE unitId = :unitId AND step = :step")
  suspend fun removeStep(unitId: String, step: TurnoverStep)

  @Query("DELETE FROM checklist_steps WHERE unitId = :unitId")
  suspend fun clearUnit(unitId: String)
}
