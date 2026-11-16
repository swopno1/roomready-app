package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffMember

/**
 * Degradation warnings collected while parsing a bootstrap payload.
 * Safe defaults were applied in every case; the sync never fails for these.
 * Nothing here carries secrets.
 */
sealed interface BootstrapWarning {
  data class UnknownHousekeeping(val unitId: String, val raw: String?) : BootstrapWarning

  data class UnknownOccupancy(val unitId: String, val raw: String) : BootstrapWarning

  data class UnknownStaffRole(val name: String, val raw: String) : BootstrapWarning

  data class UnknownChecklistKey(val raw: String) : BootstrapWarning

  data class BadTimestamp(val unitId: String, val raw: String) : BootstrapWarning

  data class SkippedUnit(val reason: String) : BootstrapWarning

  data class SkippedStaff(val reason: String) : BootstrapWarning

  data class DanglingRoomType(val unitId: String, val roomTypeId: String) : BootstrapWarning
}

/** Fatal bootstrap problems: the payload cannot be used at all. */
sealed class BootstrapException(message: String) : Exception(message) {
  data class ContractMismatch(val serverVersion: Int?) :
    BootstrapException("contractVersion $serverVersion, need ${PmsContract.CONTRACT_VERSION}")

  data class MalformedJson(val detail: String) : BootstrapException("malformed JSON: $detail")
}

/** Parsed bootstrap payload with degradation warnings. */
data class ParsedBootstrap(
  val propertyId: String?,
  val propertyName: String?,
  val roomTypes: List<RoomType>,
  val units: List<RoomUnit>,
  val staff: List<StaffMember>,
  /** Known checklist keys in PMS order (app's verbatim list stays authoritative). */
  val checklistKeys: List<String>,
  val warnings: List<BootstrapWarning>,
)
