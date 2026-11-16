package com.vivereply.roomready.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vivereply.roomready.domain.model.ActivityEvent
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.StaffRole

@Entity(tableName = "activity_log", indices = [Index("unitId"), Index("createdAt")])
data class ActivityEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val type: ActivityType,
  val actorName: String,
  val role: StaffRole?,
  val unitId: String?,
  val roomNumber: String?,
  val detail: String?,
  val createdAt: Long,
) {
  fun toDomain(): ActivityEvent =
    ActivityEvent(
      id = id,
      type = type,
      actorName = actorName,
      role = role,
      unitId = unitId,
      roomNumber = roomNumber,
      detail = detail,
      createdAtEpochMillis = createdAt,
    )

  companion object {
    fun fromDomain(event: ActivityEvent): ActivityEntity =
      ActivityEntity(
        id = event.id,
        type = event.type,
        actorName = event.actorName,
        role = event.role,
        unitId = event.unitId,
        roomNumber = event.roomNumber,
        detail = event.detail,
        createdAt = event.createdAtEpochMillis,
      )
  }
}
