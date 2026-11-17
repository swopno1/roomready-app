package com.vivereply.roomready.data.pms

import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.PmsHousekeeping
import org.json.JSONObject

/**
 * One queued entry translated to its wire call (or local verdict).
 * Untranslatable entries park visibly; they never crash the drain.
 */
sealed interface MappedCall {
  /** Nothing crosses the wire; the entry resolves locally as delivered. */
  data object LocalResolve : MappedCall

  data class RoomStatus(
    val unitId: String,
    val expected: PmsHousekeeping,
    val action: StatusAction,
    val note: String?,
  ) : MappedCall

  data class TicketCreate(
    val localId: String,
    val unitId: String,
    val title: String,
    val description: String?,
  ) : MappedCall

  /** Server id is resolved at send time from [TicketServerIds]. */
  data class TicketDone(val localId: String) : MappedCall

  data class Untranslatable(val reason: String) : MappedCall
}

/**
 * Maps stored queue entries onto the real PMS contract. Reads the R3
 * payload shape (still what repositories enqueue), so entries written
 * before this fix translate wherever they carry enough information.
 *
 * - Start cleaning (localState CLEANING) and checklist progress have no
 *   wire form: resolved locally, progress stays on the device.
 * - MARK_DIRTY has no producer under the current domain (the only
 *   to-Dirty move is a supervised send-back, which is INSPECT_FAIL);
 *   the wire builder supports it for completeness.
 */
object OutboundMapper {

  fun map(entityType: SyncEntityType, entityId: String, payloadJson: String): MappedCall {
    if (entityId.isBlank()) return MappedCall.Untranslatable("missing-entity-id")
    return when (entityType) {
      SyncEntityType.CHECKLIST -> MappedCall.LocalResolve
      SyncEntityType.TICKET_STATUS -> MappedCall.TicketDone(entityId)
      SyncEntityType.UNIT_STATUS -> mapUnitStatus(entityId, payloadJson)
      SyncEntityType.TICKET -> mapTicket(entityId, payloadJson)
    }
  }

  private fun mapUnitStatus(entityId: String, payloadJson: String): MappedCall {
    val obj = parseOrNull(payloadJson) ?: return MappedCall.Untranslatable("bad-payload")
    val localState =
      try {
        HousekeepingState.valueOf(obj.optString("localState", ""))
      } catch (e: IllegalArgumentException) {
        return MappedCall.Untranslatable("bad-local-state")
      }
    return when (localState) {
      HousekeepingState.CLEANING -> MappedCall.LocalResolve
      HousekeepingState.CLEANED ->
        MappedCall.RoomStatus(
          unitId = entityId,
          expected = PmsHousekeeping.DIRTY,
          action = StatusAction.MARK_CLEAN,
          note = noteOf(obj),
        )
      HousekeepingState.READY ->
        MappedCall.RoomStatus(
          unitId = entityId,
          expected = PmsHousekeeping.CLEAN,
          action = StatusAction.INSPECT_PASS,
          note = null,
        )
      HousekeepingState.DIRTY -> {
        val note = noteOf(obj)
        if (note.isNullOrBlank()) {
          MappedCall.Untranslatable("send-back-without-reason")
        } else {
          MappedCall.RoomStatus(
            unitId = entityId,
            expected = PmsHousekeeping.CLEAN,
            action = StatusAction.INSPECT_FAIL,
            note = note,
          )
        }
      }
    }
  }

  private fun mapTicket(entityId: String, payloadJson: String): MappedCall {
    val obj = parseOrNull(payloadJson) ?: return MappedCall.Untranslatable("bad-payload")
    val unitId = obj.optStringOrNull("unitId")
    if (unitId.isNullOrBlank()) return MappedCall.Untranslatable("missing-unit-id")
    return MappedCall.TicketCreate(
      localId = entityId,
      unitId = unitId,
      title = TicketTitles.wireTitle(obj.optStringOrNull("category")),
      description = obj.optStringOrNull("note"),
    )
  }

  /** Current `note` field first, legacy `skipReason` second. */
  private fun noteOf(obj: JSONObject): String? =
    obj.optStringOrNull("note") ?: obj.optStringOrNull("skipReason")

  private fun parseOrNull(payloadJson: String): JSONObject? =
    try {
      JSONObject(payloadJson)
    } catch (e: Exception) {
      null
    }

  private fun JSONObject.optStringOrNull(name: String): String? {
    if (isNull(name)) return null
    return optString(name, null)?.ifBlank { null }?.trim()
  }
}
