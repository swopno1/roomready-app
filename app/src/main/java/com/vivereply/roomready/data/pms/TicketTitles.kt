package com.vivereply.roomready.data.pms

import com.vivereply.roomready.domain.model.TicketCategory

/**
 * Ticket category titles on the wire. The PMS speaks fixed English labels;
 * anything else (retired or unknown values, either direction) degrades to
 * Other. Protocol constants, deliberately not localized.
 */
object TicketTitles {
  const val PLUMBING: String = "Plumbing"
  const val ELECTRICAL: String = "Electrical"
  const val AIR_CONDITIONING: String = "AC"
  const val FURNITURE: String = "Furniture"
  const val OTHER: String = "Other"

  /** Stored category name -> wire title for ticket create. */
  fun wireTitle(storedName: String?): String =
    when (storedName?.trim()?.uppercase()) {
      TicketCategory.PLUMBING.name -> PLUMBING
      TicketCategory.ELECTRICAL.name -> ELECTRICAL
      TicketCategory.AIR_CONDITIONING.name -> AIR_CONDITIONING
      TicketCategory.FURNITURE.name -> FURNITURE
      else -> OTHER
    }

  /** Server ticket title -> local category, degrading to Other. */
  fun parseCategory(title: String?): TicketCategory =
    when (title?.trim()) {
      PLUMBING -> TicketCategory.PLUMBING
      ELECTRICAL -> TicketCategory.ELECTRICAL
      AIR_CONDITIONING -> TicketCategory.AIR_CONDITIONING
      FURNITURE -> TicketCategory.FURNITURE
      else -> TicketCategory.OTHER
    }
}
