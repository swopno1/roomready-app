package com.vivereply.roomready.ui.state

import androidx.annotation.StringRes
import com.vivereply.roomready.R
import com.vivereply.roomready.data.repository.RoomActionError
import com.vivereply.roomready.data.repository.RoomActionException
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.state.HkTransitionError
import com.vivereply.roomready.domain.state.HkTransitionException

/** Role-gated primary card actions. Send-back stays a secondary supervisor move. */
enum class UnitAction(@StringRes val labelRes: Int) {
  START_CLEANING(R.string.action_start_cleaning),
  FINISH(R.string.action_finish),
  INSPECT(R.string.action_inspect),
}

/**
 * Which primary button a card shows, if any. Mirrors the domain state
 * machine's role rules; the repository still validates every tap.
 */
fun primaryActionFor(state: HousekeepingState, role: StaffRole?): UnitAction? =
  when (role) {
    StaffRole.HOUSEKEEPER ->
      when (state) {
        HousekeepingState.DIRTY -> UnitAction.START_CLEANING
        HousekeepingState.CLEANING -> UnitAction.FINISH
        HousekeepingState.CLEANED, HousekeepingState.READY -> null
      }
    StaffRole.SUPERVISOR ->
      when (state) {
        HousekeepingState.CLEANED -> UnitAction.INSPECT
        HousekeepingState.DIRTY, HousekeepingState.CLEANING, HousekeepingState.READY -> null
      }
    null -> null
  }

/** Supervisors get a secondary send-back move on Cleaned cards. */
fun canSendBack(state: HousekeepingState, role: StaffRole?): Boolean =
  role == StaffRole.SUPERVISOR && state == HousekeepingState.CLEANED

/** Maps a rejected action to its plain-language card message. */
@StringRes
fun actionErrorRes(error: Throwable): Int {
  val cause =
    (error as? HkTransitionException)?.error ?: (error as? RoomActionException)?.error
  return when (cause) {
    is HkTransitionError.ChecklistIncomplete -> R.string.action_err_checklist
    is HkTransitionError.IllegalTransition,
    is RoomActionError.StepsLocked,
    -> R.string.action_err_not_allowed
    else -> R.string.action_err_failed
  }
}
