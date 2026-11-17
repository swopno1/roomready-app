package com.vivereply.roomready.data.local

import androidx.room.TypeConverter
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncEntryStatus
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.domain.model.TurnoverStep

/** Enum <-> String converters for the Room database. */
class RoomConverters {

  @TypeConverter
  fun fromHousekeepingState(value: HousekeepingState): String = value.name

  @TypeConverter
  fun toHousekeepingState(value: String): HousekeepingState =
    HousekeepingState.valueOf(value)

  @TypeConverter
  fun fromOccupancy(value: Occupancy): String = value.name

  @TypeConverter
  fun toOccupancy(value: String): Occupancy = Occupancy.valueOf(value)

  @TypeConverter
  fun fromTurnoverStep(value: TurnoverStep): String = value.key

  @TypeConverter
  fun toTurnoverStep(value: String): TurnoverStep =
    TurnoverStep.fromKey(value)
      ?: throw IllegalArgumentException("Unknown turnover step key: $value")

  @TypeConverter
  fun fromTicketCategory(value: TicketCategory): String = value.name

  @TypeConverter
  fun toTicketCategory(value: String): TicketCategory =
    try {
      TicketCategory.valueOf(value)
    } catch (e: IllegalArgumentException) {
      TicketCategory.OTHER
    }

  @TypeConverter
  fun fromTicketStatus(value: TicketStatus): String = value.name

  @TypeConverter
  fun toTicketStatus(value: String): TicketStatus = TicketStatus.valueOf(value)

  @TypeConverter
  fun fromActivityType(value: ActivityType): String = value.name

  @TypeConverter
  fun toActivityType(value: String): ActivityType = ActivityType.valueOf(value)

  @TypeConverter
  fun fromStaffRole(value: StaffRole): String = value.name

  @TypeConverter
  fun toStaffRole(value: String): StaffRole = StaffRole.valueOf(value)

  @TypeConverter
  fun fromSyncEntityType(value: SyncEntityType): String = value.name

  @TypeConverter
  fun toSyncEntityType(value: String): SyncEntityType = SyncEntityType.valueOf(value)

  @TypeConverter
  fun fromSyncOp(value: SyncOp): String = value.name

  @TypeConverter
  fun toSyncOp(value: String): SyncOp = SyncOp.valueOf(value)

  @TypeConverter
  fun fromSyncEntryStatus(value: SyncEntryStatus): String = value.name

  @TypeConverter
  fun toSyncEntryStatus(value: String): SyncEntryStatus = SyncEntryStatus.valueOf(value)
}
