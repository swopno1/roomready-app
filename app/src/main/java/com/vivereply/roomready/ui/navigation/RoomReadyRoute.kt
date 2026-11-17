package com.vivereply.roomready.ui.navigation

/** Non-tab destinations: first-connect setup, QR pairing, room detail, finish photo. */
object RoomReadyRoute {
  const val SETUP: String = "setup"
  const val QR_SCAN: String = "qr_scan"
  const val ROOM_PATTERN: String = "room/{unitId}"
  const val PHOTO_PATTERN: String = "room/{unitId}/photo"

  fun room(unitId: String): String = "room/$unitId"

  fun photo(unitId: String): String = "room/$unitId/photo"
}
