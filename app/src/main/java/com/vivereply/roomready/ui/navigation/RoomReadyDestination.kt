package com.vivereply.roomready.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.vivereply.roomready.R
import com.vivereply.roomready.ui.components.RoomReadyIcons

enum class RoomReadyDestination(
  val route: String,
  @param:StringRes val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  @param:StringRes val emptyTitleRes: Int,
  @param:StringRes val emptyCopyRes: Int,
  @param:StringRes val emptyActionRes: Int,
  @param:StringRes val emptyArtworkCdRes: Int,
) {
  Home(
    route = "home",
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    emptyTitleRes = R.string.empty_home_title,
    emptyCopyRes = R.string.empty_home_copy,
    emptyActionRes = R.string.empty_home_action,
    emptyArtworkCdRes = R.string.empty_home_artwork_cd,
  ),
  Rooms(
    route = "rooms",
    labelRes = R.string.nav_rooms,
    selectedIcon = RoomReadyIcons.MeetingRoomFilled,
    unselectedIcon = RoomReadyIcons.MeetingRoomOutlined,
    emptyTitleRes = R.string.empty_rooms_title,
    emptyCopyRes = R.string.empty_rooms_copy,
    emptyActionRes = R.string.empty_rooms_action,
    emptyArtworkCdRes = R.string.empty_rooms_artwork_cd,
  ),
  Tickets(
    route = "tickets",
    labelRes = R.string.nav_tickets,
    selectedIcon = RoomReadyIcons.ConfirmationNumberFilled,
    unselectedIcon = RoomReadyIcons.ConfirmationNumberOutlined,
    emptyTitleRes = R.string.empty_tickets_title,
    emptyCopyRes = R.string.empty_tickets_copy,
    emptyActionRes = R.string.empty_tickets_action,
    emptyArtworkCdRes = R.string.empty_tickets_artwork_cd,
  ),
  Activity(
    route = "activity",
    labelRes = R.string.nav_activity,
    selectedIcon = RoomReadyIcons.History,
    unselectedIcon = RoomReadyIcons.History,
    emptyTitleRes = R.string.empty_activity_title,
    emptyCopyRes = R.string.empty_activity_copy,
    emptyActionRes = R.string.empty_activity_action,
    emptyArtworkCdRes = R.string.empty_activity_artwork_cd,
  ),
  Profile(
    route = "profile",
    labelRes = R.string.nav_profile,
    selectedIcon = Icons.Filled.Person,
    unselectedIcon = Icons.Outlined.Person,
    emptyTitleRes = R.string.empty_profile_title,
    emptyCopyRes = R.string.empty_profile_copy,
    emptyActionRes = R.string.empty_profile_action,
    emptyArtworkCdRes = R.string.empty_profile_artwork_cd,
  ),
}
