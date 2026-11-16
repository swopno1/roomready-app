package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.PmsHousekeeping
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TurnoverStep
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses GET /api/housekeeping/bootstrap (contractVersion 1).
 *
 * Hard degradation rules (learned from live failures):
 * - Missing or unparseable housekeepingAt degrades to null, NEVER fails.
 * - Unknown enum values degrade to a safe default + warning:
 *   housekeeping -> Dirty, occupancy -> Unknown, staff role -> Housekeeper,
 *   checklist keys -> skipped.
 * - Missing fields take silent defaults; present-but-bad values warn.
 * - Units without an id and staff without a name are skipped with a warning.
 *
 * Only a contract mismatch or malformed JSON fails the whole parse.
 */
object BootstrapParser {

  fun parse(json: String, receivedAtEpochMillis: Long): Result<ParsedBootstrap> {
    val root =
      try {
        JSONObject(json)
      } catch (e: Exception) {
        return Result.failure(BootstrapException.MalformedJson(e.message ?: "unparseable"))
      }
    val version = if (root.isNull("contractVersion")) null else root.optInt("contractVersion", -2)
    if (version != PmsContract.CONTRACT_VERSION) {
      return Result.failure(BootstrapException.ContractMismatch(version))
    }

    val warnings = mutableListOf<BootstrapWarning>()
    val property = root.optJSONObject("property")
    val types = parseTypes(root.optJSONArray("roomTypes"))
    val typeIds = types.map { it.id }.toSet()
    val units = parseUnits(root.optJSONArray("units"), typeIds, receivedAtEpochMillis, warnings)
    val staff = parseStaff(root.optJSONArray("staff"), warnings)
    val checklistKeys = parseChecklist(root.optJSONArray("checklist"), warnings)

    return Result.success(
      ParsedBootstrap(
        propertyId = property?.optStringOrNull("id"),
        propertyName = property?.optStringOrNull("name"),
        roomTypes = types,
        units = units,
        staff = staff,
        checklistKeys = checklistKeys,
        warnings = warnings,
      ),
    )
  }

  private fun parseTypes(array: JSONArray?): List<RoomType> {
    if (array == null) return emptyList()
    val out = mutableListOf<RoomType>()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      val id = obj.optStringOrNull("id") ?: continue
      val code = obj.optStringOrNull("code") ?: id
      val nameEn = obj.optStringOrNull("nameEn") ?: obj.optStringOrNull("name") ?: code
      out +=
        RoomType(
          id = id,
          code = code,
          nameEn = nameEn,
          nameBn = obj.optStringOrNull("nameBn") ?: nameEn,
          bedSummary = obj.optStringOrNull("bedSummary") ?: "",
          baseOccupancy = obj.optInt("baseOccupancy", 2),
        )
    }
    return out
  }

  private fun parseUnits(
    array: JSONArray?,
    typeIds: Set<String>,
    receivedAtEpochMillis: Long,
    warnings: MutableList<BootstrapWarning>,
  ): List<RoomUnit> {
    if (array == null) return emptyList()
    val out = mutableListOf<RoomUnit>()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      val id = obj.optStringOrNull("id")
      if (id.isNullOrBlank()) {
        warnings += BootstrapWarning.SkippedUnit("unit #$i has no id")
        continue
      }
      val roomTypeId = obj.optStringOrNull("roomTypeId") ?: ""
      if (roomTypeId.isNotEmpty() && roomTypeId !in typeIds) {
        warnings += BootstrapWarning.DanglingRoomType(id, roomTypeId)
      }

      val occupancyRaw = obj.optStringOrNull("occupancy")
      val occupancy = Occupancy.fromCode(occupancyRaw)
      if (!occupancyRaw.isNullOrBlank() && occupancy == Occupancy.UNKNOWN &&
        !occupancyRaw.equals("UNKNOWN", ignoreCase = true)
      ) {
        warnings += BootstrapWarning.UnknownOccupancy(id, occupancyRaw)
      }

      val hkRaw = obj.optStringOrNull("housekeeping")
      val hk = PmsHousekeeping.fromCode(hkRaw)?.toLocal() ?: HousekeepingState.DIRTY
      if (!hkRaw.isNullOrBlank() && PmsHousekeeping.fromCode(hkRaw) == null) {
        warnings += BootstrapWarning.UnknownHousekeeping(id, hkRaw)
      }

      val tsRaw = obj.opt("housekeepingAt")
      val housekeepingAt =
        if (tsRaw == null || tsRaw == JSONObject.NULL) {
          null
        } else {
          LenientTimestamps.parseAny(tsRaw)
        }
      if (housekeepingAt == null && tsRaw != null && tsRaw != JSONObject.NULL) {
        warnings += BootstrapWarning.BadTimestamp(id, tsRaw.toString())
      }

      out +=
        RoomUnit(
          id = id,
          number = obj.optStringOrNull("number") ?: id,
          roomTypeId = roomTypeId,
          floor = obj.optStringOrNull("floor"),
          occupancy = occupancy,
          hkState = hk,
          updatedAtEpochMillis = housekeepingAt ?: receivedAtEpochMillis,
          housekeepingAtEpochMillis = housekeepingAt,
        )
    }
    return out
  }

  private fun parseStaff(array: JSONArray?, warnings: MutableList<BootstrapWarning>): List<StaffMember> {
    if (array == null) return emptyList()
    val out = mutableListOf<StaffMember>()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      val name = obj.optStringOrNull("name")
      if (name.isNullOrBlank()) {
        warnings += BootstrapWarning.SkippedStaff("staff #$i has no name")
        continue
      }
      val roleRaw = obj.optStringOrNull("role")
      val role =
        roleRaw?.let { raw ->
          StaffRole.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        } ?: StaffRole.HOUSEKEEPER
      if (!roleRaw.isNullOrBlank() &&
        StaffRole.entries.none { it.name.equals(roleRaw.trim(), ignoreCase = true) }
      ) {
        warnings += BootstrapWarning.UnknownStaffRole(name, roleRaw)
      }
      out += StaffMember(name.trim(), role)
    }
    return out
  }

  private fun parseChecklist(array: JSONArray?, warnings: MutableList<BootstrapWarning>): List<String> {
    if (array == null) return emptyList()
    val out = mutableListOf<String>()
    for (i in 0 until array.length()) {
      val key =
        when (val entry = array.opt(i)) {
          is JSONObject -> entry.optStringOrNull("key")
          is String -> entry
          else -> null
        }
      if (key.isNullOrBlank()) continue
      if (TurnoverStep.fromKey(key) == null) {
        warnings += BootstrapWarning.UnknownChecklistKey(key)
        continue
      }
      if (key !in out) out += key
    }
    return out
  }

  private fun JSONObject.optStringOrNull(name: String): String? {
    if (isNull(name)) return null
    val value = optString(name, null) ?: return null
    return value.ifBlank { null }?.trim()
  }
}
