package com.vivereply.roomready.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole

/** PMS staff roster snapshot (replaced on every bootstrap import). */
@Entity(tableName = "staff_roster")
data class StaffRosterEntity(
  @PrimaryKey val name: String,
  val role: StaffRole,
) {
  fun toDomain(): StaffMember = StaffMember(name, role)

  companion object {
    fun fromDomain(staff: StaffMember): StaffRosterEntity =
      StaffRosterEntity(staff.name, staff.role)
  }
}
