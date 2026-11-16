package com.example.domain.model

import androidx.annotation.StringRes
import com.example.R

/**
 * Staff roles with distinct housekeeping powers:
 * - HOUSEKEEPER: start cleaning (Dirty -> Cleaning), complete the checklist,
 *   finish (Cleaning -> Cleaned).
 * - SUPERVISOR: inspect (Cleaned -> Ready), or send back to Dirty with a reason.
 */
enum class StaffRole(@param:StringRes val labelRes: Int) {
  HOUSEKEEPER(R.string.role_housekeeper),
  SUPERVISOR(R.string.role_supervisor),
}

/** Signed-in staff member performing local actions. */
data class StaffMember(
  val name: String,
  val role: StaffRole,
) {
  init {
    require(name.isNotBlank()) { "Staff name must not be blank" }
  }
}
