package com.example.data.sync

import com.example.domain.model.HousekeepingState
import com.example.domain.model.PmsHousekeeping
import com.example.domain.model.Ticket
import com.example.domain.model.TicketStatus

/**
 * Builds the JSON bodies stored in the sync queue.
 *
 * These builders take no credential parameters, so the API key cannot leak
 * into a queued payload by construction. Auth is applied at send time by
 * the transport implementation.
 */
object SyncPayloads {

  fun unitStatus(
    unitId: String,
    hkState: HousekeepingState,
    actorName: String,
    atEpochMillis: Long,
  ): String =
    buildJson(
      "unitId" to unitId,
      "housekeeping" to PmsHousekeeping.fromLocal(hkState).code,
      "localState" to hkState.name,
      "actor" to actorName,
      "at" to atEpochMillis,
    )

  fun checklist(
    unitId: String,
    doneKeys: List<String>,
    isComplete: Boolean,
    actorName: String,
    atEpochMillis: Long,
  ): String =
    buildJson(
      "unitId" to unitId,
      "done" to doneKeys,
      "complete" to isComplete,
      "actor" to actorName,
      "at" to atEpochMillis,
    )

  fun ticket(ticket: Ticket): String =
    buildJson(
      "ticketId" to ticket.id,
      "unitId" to ticket.unitId,
      "roomNumber" to ticket.roomNumber,
      "category" to ticket.category.name,
      "note" to ticket.note,
      "photoRef" to ticket.photoRef,
      "status" to ticket.status.name,
      "createdBy" to ticket.createdBy,
      "at" to ticket.createdAtEpochMillis,
    )

  fun ticketStatus(
    ticketId: String,
    status: TicketStatus,
    actorName: String,
    atEpochMillis: Long,
  ): String =
    buildJson(
      "ticketId" to ticketId,
      "status" to status.name,
      "actor" to actorName,
      "at" to atEpochMillis,
    )

  private fun buildJson(vararg fields: Pair<String, Any?>): String {
    val body =
      fields.joinToString(",") { (key, value) ->
        "\"${escape(key)}\":${encodeValue(value)}"
      }
    return "{$body}"
  }

  private fun encodeValue(value: Any?): String =
    when (value) {
      null -> "null"
      is String -> "\"${escape(value)}\""
      is Number, is Boolean -> value.toString()
      is List<*> -> value.joinToString(",", "[", "]") { encodeValue(it) }
      else -> "\"${escape(value.toString())}\""
    }

  /** Minimal JSON string escaper for payload fields. */
  fun escape(value: String): String {
    val out = StringBuilder(value.length + 8)
    for (ch in value) {
      when (ch) {
        '"' -> out.append("\\\"")
        '\\' -> out.append("\\\\")
        '\n' -> out.append("\\n")
        '\r' -> out.append("\\r")
        '\t' -> out.append("\\t")
        else ->
          if (ch < ' ') {
            out.append("\\u%04x".format(ch.code))
          } else {
            out.append(ch)
          }
      }
    }
    return out.toString()
  }
}
