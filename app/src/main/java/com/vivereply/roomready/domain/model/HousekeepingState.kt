package com.vivereply.roomready.domain.model

/**
 * Local housekeeping workflow state for a room unit.
 *
 * Flow: Dirty -> Cleaning -> Cleaned -> Ready.
 * Transitions are validated by [com.vivereply.roomready.domain.state.HousekeepingStateMachine];
 * no other code path may change this value for a local action.
 */
enum class HousekeepingState {
  DIRTY,
  CLEANING,
  CLEANED,
  READY,
}
