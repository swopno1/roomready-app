package com.vivereply.roomready.domain.model

/**
 * Room occupancy as reported by the PMS.
 *
 * Read-only in the app: only the PMS bootstrap/sync import may set it.
 * No local housekeeping action changes occupancy.
 */
enum class Occupancy {
  VACANT,
  OCCUPIED,
  OUT_OF_ORDER,
  UNKNOWN,
  ;

  companion object {
    fun fromCode(code: String?): Occupancy {
      if (code.isNullOrBlank()) return UNKNOWN
      return when (code.trim().uppercase()) {
        // AVAILABLE is the live PMS synonym for a vacant unit (unitStatus).
        "VACANT", "VAC", "AVAILABLE" -> VACANT
        "OCCUPIED", "OCC" -> OCCUPIED
        "OUT_OF_ORDER", "OOO", "OUT-OF-ORDER" -> OUT_OF_ORDER
        else -> UNKNOWN
      }
    }
  }
}
