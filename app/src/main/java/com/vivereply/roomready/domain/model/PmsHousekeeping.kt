package com.vivereply.roomready.domain.model

/**
 * Housekeeping states as reported by the resort PMS.
 *
 * Contract mapping (PMS -> local):
 * - DIRTY -> Dirty
 * - CLEAN -> Cleaned
 * - INSPECTED -> Ready
 *
 * The PMS has no in-progress state, so local [HousekeepingState.CLEANING]
 * maps back to PMS DIRTY: from the PMS viewpoint the room is still dirty
 * until the checklist is finished.
 */
enum class PmsHousekeeping(val code: String) {
  DIRTY("DIRTY"),
  CLEAN("CLEAN"),
  INSPECTED("INSPECTED");

  fun toLocal(): HousekeepingState =
    when (this) {
      DIRTY -> HousekeepingState.DIRTY
      CLEAN -> HousekeepingState.CLEANED
      INSPECTED -> HousekeepingState.READY
    }

  companion object {
    /**
     * Parses a PMS housekeeping code. Returns null for null/blank/unknown
     * codes so the importer can keep local state instead of guessing.
     */
    fun fromCode(code: String?): PmsHousekeeping? {
      if (code.isNullOrBlank()) return null
      return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
    }

    fun fromLocal(state: HousekeepingState): PmsHousekeeping =
      when (state) {
        HousekeepingState.DIRTY -> DIRTY
        HousekeepingState.CLEANING -> DIRTY
        HousekeepingState.CLEANED -> CLEAN
        HousekeepingState.READY -> INSPECTED
      }
  }
}
