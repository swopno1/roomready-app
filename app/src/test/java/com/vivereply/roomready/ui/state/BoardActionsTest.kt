package com.vivereply.roomready.ui.state

import com.vivereply.roomready.R
import com.vivereply.roomready.data.repository.RoomActionError
import com.vivereply.roomready.data.repository.RoomActionException
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.Occupancy
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.state.HkTransitionError
import com.vivereply.roomready.domain.state.HkTransitionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardActionsTest {

  @Test
  fun `housekeeper sees start on dirty and finish on cleaning`() {
    assertEquals(
      UnitAction.START_CLEANING,
      primaryActionFor(HousekeepingState.DIRTY, StaffRole.HOUSEKEEPER),
    )
    assertEquals(
      UnitAction.FINISH,
      primaryActionFor(HousekeepingState.CLEANING, StaffRole.HOUSEKEEPER),
    )
    assertNull(primaryActionFor(HousekeepingState.CLEANED, StaffRole.HOUSEKEEPER))
    assertNull(primaryActionFor(HousekeepingState.READY, StaffRole.HOUSEKEEPER))
  }

  @Test
  fun `supervisor sees inspect only on cleaned`() {
    assertNull(primaryActionFor(HousekeepingState.DIRTY, StaffRole.SUPERVISOR))
    assertNull(primaryActionFor(HousekeepingState.CLEANING, StaffRole.SUPERVISOR))
    assertEquals(
      UnitAction.INSPECT,
      primaryActionFor(HousekeepingState.CLEANED, StaffRole.SUPERVISOR),
    )
    assertNull(primaryActionFor(HousekeepingState.READY, StaffRole.SUPERVISOR))
  }

  @Test
  fun `no role means no actions`() {
    HousekeepingState.entries.forEach { assertNull(primaryActionFor(it, null)) }
  }

  @Test
  fun `send back is a supervisor move on cleaned only`() {
    assertTrue(canSendBack(HousekeepingState.CLEANED, StaffRole.SUPERVISOR))
    assertFalse(canSendBack(HousekeepingState.DIRTY, StaffRole.SUPERVISOR))
    assertFalse(canSendBack(HousekeepingState.CLEANED, StaffRole.HOUSEKEEPER))
    assertFalse(canSendBack(HousekeepingState.CLEANED, null))
  }

  @Test
  fun `action failures map to plain-language messages`() {
    assertEquals(
      R.string.action_err_checklist,
      actionErrorRes(HkTransitionException(HkTransitionError.ChecklistIncomplete(2))),
    )
    assertEquals(
      R.string.action_err_not_allowed,
      actionErrorRes(
        HkTransitionException(
          HkTransitionError.IllegalTransition(
            HousekeepingState.DIRTY,
            HousekeepingState.READY,
            StaffRole.HOUSEKEEPER,
          ),
        ),
      ),
    )
    assertEquals(
      R.string.action_err_not_allowed,
      actionErrorRes(RoomActionException(RoomActionError.StepsLocked(HousekeepingState.DIRTY))),
    )
    assertEquals(
      R.string.action_err_failed,
      actionErrorRes(RoomActionException(RoomActionError.UnknownUnit("u-1"))),
    )
    assertEquals(R.string.action_err_failed, actionErrorRes(RuntimeException("boom")))
  }

  @Test
  fun `filters match exactly one state plus all`() {
    HousekeepingState.entries.forEach { assertTrue(BoardFilter.ALL.matches(it)) }
    assertTrue(BoardFilter.DIRTY.matches(HousekeepingState.DIRTY))
    assertFalse(BoardFilter.DIRTY.matches(HousekeepingState.CLEANED))
    assertTrue(BoardFilter.CLEANING.matches(HousekeepingState.CLEANING))
    assertTrue(BoardFilter.CLEANED.matches(HousekeepingState.CLEANED))
    assertTrue(BoardFilter.READY.matches(HousekeepingState.READY))
  }

  @Test
  fun `counts group units by state`() {
    val units =
      listOf(
        unit("u-1", HousekeepingState.DIRTY),
        unit("u-2", HousekeepingState.DIRTY),
        unit("u-3", HousekeepingState.READY),
      )
    assertEquals(2, units.countByState(HousekeepingState.DIRTY))
    assertEquals(0, units.countByState(HousekeepingState.CLEANING))
    assertEquals(1, units.countByState(HousekeepingState.READY))
  }

  private fun unit(id: String, state: HousekeepingState): RoomUnit =
    RoomUnit(id, "1", "rt-t", null, Occupancy.VACANT, state, 1L)
}
