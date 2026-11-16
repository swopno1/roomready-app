package com.vivereply.roomready.domain.state

import com.vivereply.roomready.domain.model.TicketStatus

/** The only legal ticket move is OPEN -> DONE. */
sealed interface TicketTransitionError {
  data class IllegalTransition(val from: TicketStatus, val to: TicketStatus) : TicketTransitionError
}

class TicketTransitionException(val error: TicketTransitionError) :
  IllegalStateException(error.toString())

object TicketLifecycle {
  fun evaluate(from: TicketStatus, to: TicketStatus): Result<TicketStatus> =
    if (from == TicketStatus.OPEN && to == TicketStatus.DONE) {
      Result.success(to)
    } else {
      Result.failure(TicketTransitionException(TicketTransitionError.IllegalTransition(from, to)))
    }

  fun requireTransition(from: TicketStatus, to: TicketStatus): TicketStatus =
    evaluate(from, to).getOrThrow()
}
