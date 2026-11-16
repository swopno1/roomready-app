package com.vivereply.roomready.domain.model

/**
 * A physical room unit from the PMS bootstrap payload.
 *
 * - [occupancy] is read-only in the app; only PMS import sets it.
 * - [hkState] changes only through validated state-machine transitions.
 */
data class RoomUnit(
  /** Stable PMS identifier. */
  val id: String,
  /** Human room number/label, e.g. "101". */
  val number: String,
  val roomTypeId: String,
  val floor: String?,
  val occupancy: Occupancy,
  val hkState: HousekeepingState,
  val updatedAtEpochMillis: Long,
)
