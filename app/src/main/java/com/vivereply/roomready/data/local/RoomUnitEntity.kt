package com.vivereply.roomready.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomUnit

@Entity(tableName = "room_units", indices = [Index("number"), Index("roomTypeId")])
data class RoomUnitEntity(
  @PrimaryKey val id: String,
  val number: String,
  val roomTypeId: String,
  val floor: String?,
  val occupancy: Occupancy,
  val hkState: HousekeepingState,
  val updatedAt: Long,
  val housekeepingAt: Long? = null,
) {
  fun toDomain(): RoomUnit =
    RoomUnit(
      id = id,
      number = number,
      roomTypeId = roomTypeId,
      floor = floor,
      occupancy = occupancy,
      hkState = hkState,
      updatedAtEpochMillis = updatedAt,
      housekeepingAtEpochMillis = housekeepingAt,
    )

  companion object {
    fun fromDomain(unit: RoomUnit): RoomUnitEntity =
      RoomUnitEntity(
        id = unit.id,
        number = unit.number,
        roomTypeId = unit.roomTypeId,
        floor = unit.floor,
        occupancy = unit.occupancy,
        hkState = unit.hkState,
        updatedAt = unit.updatedAtEpochMillis,
        housekeepingAt = unit.housekeepingAtEpochMillis,
      )
  }
}
