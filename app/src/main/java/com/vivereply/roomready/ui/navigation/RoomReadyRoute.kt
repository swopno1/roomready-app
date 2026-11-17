package com.vivereply.roomready.ui.navigation

/** Non-tab destinations: setup, QR pairing, room detail, inspection, tickets. */
object RoomReadyRoute {
  const val SETUP: String = "setup"
  const val QR_SCAN: String = "qr_scan"
  const val ROOM_PATTERN: String = "room/{unitId}"
  const val PHOTO_PATTERN: String = "room/{unitId}/photo"
  const val INSPECT_PATTERN: String = "room/{unitId}/inspect"
  const val TICKET_FORM_PATTERN: String = "ticket/new?unitId={unitId}"
  const val TICKET_PHOTO: String = "ticket/photo"

  fun room(unitId: String): String = "room/$unitId"

  fun photo(unitId: String): String = "room/$unitId/photo"

  fun inspect(unitId: String): String = "room/$unitId/inspect"

  fun ticketForm(unitId: String?): String =
    if (unitId == null) "ticket/new" else "ticket/new?unitId=$unitId"
}
