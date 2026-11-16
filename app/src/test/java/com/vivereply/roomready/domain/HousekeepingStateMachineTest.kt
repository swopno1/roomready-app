package com.vivereply.roomready.domain

import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.state.HkTransitionError
import com.vivereply.roomready.domain.state.HkTransitionException
import com.vivereply.roomready.domain.state.HousekeepingStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive from x to x role matrix for the housekeeping state machine.
 * Legal moves (with favorable guards: checklist 8/8, reason present):
 * - HOUSEKEEPER: DIRTY -> CLEANING, CLEANING -> CLEANED
 * - SUPERVISOR: CLEANED -> READY, CLEANED -> DIRTY
 * All other 28 combinations are illegal.
 */
class HousekeepingStateMachineTest {

  private val legalMoves =
    setOf(
      Triple(HousekeepingState.DIRTY, HousekeepingState.CLEANING, StaffRole.HOUSEKEEPER),
      Triple(HousekeepingState.CLEANING, HousekeepingState.CLEANED, StaffRole.HOUSEKEEPER),
      Triple(HousekeepingState.CLEANED, HousekeepingState.READY, StaffRole.SUPERVISOR),
      Triple(HousekeepingState.CLEANED, HousekeepingState.DIRTY, StaffRole.SUPERVISOR),
    )

  @Test
  fun `exhaustive matrix - every from x to x role combination behaves per contract`() {
    var checked = 0
    for (from in HousekeepingState.entries) {
      for (to in HousekeepingState.entries) {
        for (role in StaffRole.entries) {
          checked++
          val result =
            HousekeepingStateMachine.evaluate(
              from = from,
              to = to,
              role = role,
              checklistDone = 8,
              checklistTotal = 8,
              reason = "streaks on mirror",
            )
          val legal = Triple(from, to, role) in legalMoves
          if (legal) {
            assertTrue(
              "Expected $from -> $to by $role to succeed, got $result",
              result.isSuccess,
            )
            assertEquals(to, result.getOrNull())
          } else {
            assertTrue(
              "Expected $from -> $to by $role to fail, got $result",
              result.isFailure,
            )
            val error = result.exceptionOrNull() as? HkTransitionException
            assertTrue(
              "Expected HkTransitionException for $from -> $to by $role",
              error != null,
            )
            assertTrue(
              "Expected IllegalTransition for $from -> $to by $role, got ${error?.error}",
              error?.error is HkTransitionError.IllegalTransition,
            )
          }
        }
      }
    }
    assertEquals(32, checked)
  }

  @Test
  fun `finish requires every checklist step done`() {
    val incomplete =
      HousekeepingStateMachine.evaluate(
        from = HousekeepingState.CLEANING,
        to = HousekeepingState.CLEANED,
        role = StaffRole.HOUSEKEEPER,
        checklistDone = 7,
        checklistTotal = 8,
      )
    assertTrue(incomplete.isFailure)
    val error = incomplete.exceptionOrNull() as HkTransitionException
    assertEquals(HkTransitionError.ChecklistIncomplete(missingSteps = 1), error.error)

    val none =
      HousekeepingStateMachine.evaluate(
        from = HousekeepingState.CLEANING,
        to = HousekeepingState.CLEANED,
        role = StaffRole.HOUSEKEEPER,
        checklistDone = 0,
        checklistTotal = 8,
      )
    val noneError = (none.exceptionOrNull() as HkTransitionException).error
    assertEquals(HkTransitionError.ChecklistIncomplete(missingSteps = 8), noneError)
  }

  @Test
  fun `finish with zero-step checklist definition is rejected`() {
    val result =
      HousekeepingStateMachine.evaluate(
        from = HousekeepingState.CLEANING,
        to = HousekeepingState.CLEANED,
        role = StaffRole.HOUSEKEEPER,
        checklistDone = 0,
        checklistTotal = 0,
      )
    assertTrue(result.isFailure)
  }

  @Test
  fun `send back without a reason is rejected for every blank form`() {
    listOf(null, "", "   ").forEach { reason ->
      val result =
        HousekeepingStateMachine.evaluate(
          from = HousekeepingState.CLEANED,
          to = HousekeepingState.DIRTY,
          role = StaffRole.SUPERVISOR,
          reason = reason,
        )
      assertTrue("reason=$reason should fail", result.isFailure)
      val error = result.exceptionOrNull() as HkTransitionException
      assertTrue(error.error is HkTransitionError.ReasonRequired)
    }
  }

  @Test
  fun `supervisor cannot perform housekeeper moves and vice versa`() {
    val supervisorStart =
      HousekeepingStateMachine.evaluate(
        HousekeepingState.DIRTY,
        HousekeepingState.CLEANING,
        StaffRole.SUPERVISOR,
      )
    assertTrue(supervisorStart.isFailure)

    val supervisorFinish =
      HousekeepingStateMachine.evaluate(
        HousekeepingState.CLEANING,
        HousekeepingState.CLEANED,
        StaffRole.SUPERVISOR,
        checklistDone = 8,
        checklistTotal = 8,
      )
    assertTrue(supervisorFinish.isFailure)

    val housekeeperInspect =
      HousekeepingStateMachine.evaluate(
        HousekeepingState.CLEANED,
        HousekeepingState.READY,
        StaffRole.HOUSEKEEPER,
      )
    assertTrue(housekeeperInspect.isFailure)

    val housekeeperSendBack =
      HousekeepingStateMachine.evaluate(
        HousekeepingState.CLEANED,
        HousekeepingState.DIRTY,
        StaffRole.HOUSEKEEPER,
        reason = "streaks",
      )
    assertTrue(housekeeperSendBack.isFailure)
  }

  @Test
  fun `no self transitions for any state or role`() {
    for (state in HousekeepingState.entries) {
      for (role in StaffRole.entries) {
        val result = HousekeepingStateMachine.evaluate(state, state, role)
        assertTrue("$state -> $state by $role must fail", result.isFailure)
      }
    }
  }

  @Test
  fun `ready is terminal and dirty cannot skip ahead`() {
    for (role in StaffRole.entries) {
      for (to in HousekeepingState.entries) {
        if (to != HousekeepingState.READY) {
          assertTrue(
            "READY -> $to by $role must fail",
            HousekeepingStateMachine.evaluate(HousekeepingState.READY, to, role).isFailure,
          )
        }
      }
    }
    assertTrue(
      HousekeepingStateMachine.evaluate(
        HousekeepingState.DIRTY,
        HousekeepingState.CLEANED,
        StaffRole.HOUSEKEEPER,
        checklistDone = 8,
        checklistTotal = 8,
      ).isFailure,
    )
    assertTrue(
      HousekeepingStateMachine.evaluate(
        HousekeepingState.DIRTY,
        HousekeepingState.READY,
        StaffRole.SUPERVISOR,
      ).isFailure,
    )
  }

  @Test
  fun `requireTransition returns target on legal moves and throws typed errors`() {
    assertEquals(
      HousekeepingState.CLEANING,
      HousekeepingStateMachine.requireTransition(
        HousekeepingState.DIRTY,
        HousekeepingState.CLEANING,
        StaffRole.HOUSEKEEPER,
      ),
    )
    try {
      HousekeepingStateMachine.requireTransition(
        HousekeepingState.DIRTY,
        HousekeepingState.READY,
        StaffRole.HOUSEKEEPER,
      )
      throw AssertionError("Expected HkTransitionException")
    } catch (e: HkTransitionException) {
      assertTrue(e.error is HkTransitionError.IllegalTransition)
    }
  }

  @Test
  fun `allowedTargets matches the matrix for every state and role`() {
    val hk = StaffRole.HOUSEKEEPER
    val sup = StaffRole.SUPERVISOR
    assertEquals(
      setOf(HousekeepingState.CLEANING),
      HousekeepingStateMachine.allowedTargets(HousekeepingState.DIRTY, hk),
    )
    assertEquals(
      emptySet<HousekeepingState>(),
      HousekeepingStateMachine.allowedTargets(HousekeepingState.DIRTY, sup),
    )
    assertEquals(
      setOf(HousekeepingState.CLEANED),
      HousekeepingStateMachine.allowedTargets(
        HousekeepingState.CLEANING,
        hk,
        checklistDone = 8,
        checklistTotal = 8,
      ),
    )
    assertEquals(
      emptySet<HousekeepingState>(),
      HousekeepingStateMachine.allowedTargets(
        HousekeepingState.CLEANING,
        hk,
        checklistDone = 7,
        checklistTotal = 8,
      ),
    )
    assertEquals(
      emptySet<HousekeepingState>(),
      HousekeepingStateMachine.allowedTargets(HousekeepingState.CLEANING, sup),
    )
    assertEquals(
      emptySet<HousekeepingState>(),
      HousekeepingStateMachine.allowedTargets(HousekeepingState.CLEANED, hk),
    )
    assertEquals(
      setOf(HousekeepingState.READY, HousekeepingState.DIRTY),
      HousekeepingStateMachine.allowedTargets(HousekeepingState.CLEANED, sup),
    )
    assertEquals(
      emptySet<HousekeepingState>(),
      HousekeepingStateMachine.allowedTargets(HousekeepingState.READY, hk),
    )
    assertEquals(
      emptySet<HousekeepingState>(),
      HousekeepingStateMachine.allowedTargets(HousekeepingState.READY, sup),
    )
  }
}
