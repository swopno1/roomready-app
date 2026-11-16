package com.vivereply.roomready.domain.state

import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.StaffRole

/**
 * Typed reason a housekeeping transition was rejected.
 * Carries data only (no user-facing copy); UI maps these to keyed strings.
 */
sealed interface HkTransitionError {
  /** The (from, to, role) triple is not a legal move. */
  data class IllegalTransition(
    val from: HousekeepingState,
    val to: HousekeepingState,
    val role: StaffRole,
  ) : HkTransitionError

  /** Finishing requires every checklist step done. */
  data class ChecklistIncomplete(val missingSteps: Int) : HkTransitionError

  /** Sending back to Dirty requires a supervisor reason. */
  data class ReasonRequired(
    val from: HousekeepingState,
    val to: HousekeepingState,
  ) : HkTransitionError
}

/** Thrown by [HousekeepingStateMachine.requireTransition] on illegal moves. */
class HkTransitionException(val error: HkTransitionError) : IllegalStateException(error.toString())

/**
 * The single authority for housekeeping state changes.
 *
 * Legal moves:
 * - HOUSEKEEPER: DIRTY -> CLEANING (start cleaning)
 * - HOUSEKEEPER: CLEANING -> CLEANED (finish; all checklist steps done)
 * - SUPERVISOR: CLEANED -> READY (inspect)
 * - SUPERVISOR: CLEANED -> DIRTY (send back; non-blank reason required)
 *
 * Every other from x to x role combination is rejected. Repositories must
 * route all state changes through [evaluate]; there is no direct setter path.
 */
object HousekeepingStateMachine {

  fun evaluate(
    from: HousekeepingState,
    to: HousekeepingState,
    role: StaffRole,
    checklistDone: Int = 0,
    checklistTotal: Int = 0,
    reason: String? = null,
  ): Result<HousekeepingState> {
    if (from == to) {
      return Result.failure(HkTransitionException(HkTransitionError.IllegalTransition(from, to, role)))
    }
    val allowed =
      when {
        role == StaffRole.HOUSEKEEPER && from == HousekeepingState.DIRTY &&
          to == HousekeepingState.CLEANING -> true
        role == StaffRole.HOUSEKEEPER && from == HousekeepingState.CLEANING &&
          to == HousekeepingState.CLEANED -> checklistTotal > 0 && checklistDone >= checklistTotal
        role == StaffRole.SUPERVISOR && from == HousekeepingState.CLEANED &&
          to == HousekeepingState.READY -> true
        role == StaffRole.SUPERVISOR && from == HousekeepingState.CLEANED &&
          to == HousekeepingState.DIRTY -> !reason.isNullOrBlank()
        else -> false
      }
    if (allowed) return Result.success(to)

    val error =
      when {
        role == StaffRole.HOUSEKEEPER && from == HousekeepingState.CLEANING &&
          to == HousekeepingState.CLEANED ->
          HkTransitionError.ChecklistIncomplete(
            missingSteps = (checklistTotal - checklistDone).coerceAtLeast(0),
          )
        role == StaffRole.SUPERVISOR && from == HousekeepingState.CLEANED &&
          to == HousekeepingState.DIRTY ->
          HkTransitionError.ReasonRequired(from, to)
        else -> HkTransitionError.IllegalTransition(from, to, role)
      }
    return Result.failure(HkTransitionException(error))
  }

  /** Throwing variant for call sites that treat illegal moves as bugs. */
  fun requireTransition(
    from: HousekeepingState,
    to: HousekeepingState,
    role: StaffRole,
    checklistDone: Int = 0,
    checklistTotal: Int = 0,
    reason: String? = null,
  ): HousekeepingState = evaluate(from, to, role, checklistDone, checklistTotal, reason).getOrThrow()

  /**
   * Structurally reachable targets for UI enablement. The send-back target
   * (CLEANED -> DIRTY) is listed for supervisors; the reason is still
   * validated at transition time.
   */
  fun allowedTargets(
    from: HousekeepingState,
    role: StaffRole,
    checklistDone: Int = 0,
    checklistTotal: Int = 0,
  ): Set<HousekeepingState> =
    HousekeepingState.entries
      .filter { to ->
        if (to == from) {
          false
        } else if (from == HousekeepingState.CLEANED && to == HousekeepingState.DIRTY) {
          role == StaffRole.SUPERVISOR
        } else {
          evaluate(from, to, role, checklistDone, checklistTotal, reason = null).isSuccess
        }
      }.toSet()
}
