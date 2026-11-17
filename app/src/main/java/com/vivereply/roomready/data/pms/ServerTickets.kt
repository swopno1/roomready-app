package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import org.json.JSONArray

/** One ticket as read back from GET /tickets?status=OPEN|DONE. */
data class ServerTicket(
  val id: String,
  val unitId: String?,
  val title: String?,
  val category: TicketCategory,
  val description: String?,
  val status: TicketStatus,
  val doneBy: String?,
  val doneAt: Long?,
)

/**
 * Parses the bare ticket arrays. Per-item problems (missing id, unknown
 * status) skip that item; a malformed top level throws for the caller to
 * treat as a failed fetch. Unknown titles degrade to Other.
 */
object ServerTickets {

  fun parseArray(json: String): List<ServerTicket> {
    val array = JSONArray(json)
    val out = mutableListOf<ServerTicket>()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      val id = obj.optString("id", "").trim()
      if (id.isEmpty()) continue
      val status =
        try {
          TicketStatus.valueOf(obj.optString("status", ""))
        } catch (e: IllegalArgumentException) {
          continue
        }
      val title = obj.optString("title", null)?.ifBlank { null }
      out +=
        ServerTicket(
          id = id,
          unitId = obj.optString("unitId", null)?.ifBlank { null },
          title = title,
          category = TicketTitles.parseCategory(title),
          description = obj.optString("description", null)?.ifBlank { null },
          status = status,
          doneBy = obj.optString("doneBy", null)?.ifBlank { null },
          doneAt = LenientTimestamps.parseAny(obj.opt("doneAt")),
        )
    }
    return out
  }
}
