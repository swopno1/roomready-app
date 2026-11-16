package com.vivereply.roomready.data.local

import androidx.room.Entity
import androidx.room.Index
import com.vivereply.roomready.domain.model.TurnoverStep

/** One completed checklist step for a unit; absence of a row means not done. */
@Entity(tableName = "checklist_steps", primaryKeys = ["unitId", "step"], indices = [Index("unitId")])
data class ChecklistStepEntity(
  val unitId: String,
  val step: TurnoverStep,
  val doneAt: Long,
  val doneBy: String,
)
