package com.example.domain

import com.example.domain.model.TicketStatus
import com.example.domain.state.TicketLifecycle
import com.example.domain.state.TicketTransitionError
import com.example.domain.state.TicketTransitionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Full 2x2 ticket lifecycle matrix: only OPEN -> DONE succeeds. */
class TicketLifecycleTest {

  @Test
  fun `open ticket can be marked done`() {
    val result = TicketLifecycle.evaluate(TicketStatus.OPEN, TicketStatus.DONE)
    assertTrue(result.isSuccess)
    assertEquals(TicketStatus.DONE, result.getOrNull())
  }

  @Test
  fun `done ticket cannot move again`() {
    val again = TicketLifecycle.evaluate(TicketStatus.DONE, TicketStatus.DONE)
    assertTrue(again.isFailure)
    assertTrue((again.exceptionOrNull() as TicketTransitionException).error is TicketTransitionError.IllegalTransition)

    val reopen = TicketLifecycle.evaluate(TicketStatus.DONE, TicketStatus.OPEN)
    assertTrue(reopen.isFailure)
  }

  @Test
  fun `open to open is not a transition`() {
    val result = TicketLifecycle.evaluate(TicketStatus.OPEN, TicketStatus.OPEN)
    assertTrue(result.isFailure)
  }

  @Test
  fun `requireTransition throws typed error on illegal moves`() {
    assertEquals(
      TicketStatus.DONE,
      TicketLifecycle.requireTransition(TicketStatus.OPEN, TicketStatus.DONE),
    )
    try {
      TicketLifecycle.requireTransition(TicketStatus.DONE, TicketStatus.OPEN)
      throw AssertionError("Expected TicketTransitionException")
    } catch (e: TicketTransitionException) {
      assertTrue(e.error is TicketTransitionError.IllegalTransition)
    }
  }
}
