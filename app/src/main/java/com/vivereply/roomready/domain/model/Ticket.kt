package com.vivereply.roomready.domain.model

import androidx.annotation.StringRes
import com.vivereply.roomready.R

/** Maintenance ticket category: exactly the five raise-sheet chips. */
enum class TicketCategory(@param:StringRes val labelRes: Int) {
  PLUMBING(R.string.ticket_category_plumbing),
  ELECTRICAL(R.string.ticket_category_electrical),
  AIR_CONDITIONING(R.string.ticket_category_ac),
  FURNITURE(R.string.ticket_category_furniture),
  OTHER(R.string.ticket_category_other),
}

/** Ticket lifecycle state: open -> done, one way. */
enum class TicketStatus {
  OPEN,
  DONE,
}

/**
 * A maintenance/housekeeping ticket raised against a room unit.
 *
 * [photoRef] is an opaque local reference (file name/URI) captured offline;
 * the photo bytes sync later through the PMS channel.
 */
data class Ticket(
  val id: String,
  val unitId: String,
  val roomNumber: String,
  val category: TicketCategory,
  val note: String,
  val photoRef: String?,
  val status: TicketStatus,
  val createdBy: String,
  val createdAtEpochMillis: Long,
  val updatedAtEpochMillis: Long,
)
