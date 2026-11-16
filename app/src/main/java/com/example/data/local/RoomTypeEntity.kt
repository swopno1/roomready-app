package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.RoomType

@Entity(tableName = "room_types")
data class RoomTypeEntity(
  @PrimaryKey val id: String,
  val code: String,
  val nameEn: String,
  val nameBn: String,
  val bedSummary: String,
  val baseOccupancy: Int,
) {
  fun toDomain(): RoomType =
    RoomType(
      id = id,
      code = code,
      nameEn = nameEn,
      nameBn = nameBn,
      bedSummary = bedSummary,
      baseOccupancy = baseOccupancy,
    )

  companion object {
    fun fromDomain(type: RoomType): RoomTypeEntity =
      RoomTypeEntity(
        id = type.id,
        code = type.code,
        nameEn = type.nameEn,
        nameBn = type.nameBn,
        bedSummary = type.bedSummary,
        baseOccupancy = type.baseOccupancy,
      )
  }
}
