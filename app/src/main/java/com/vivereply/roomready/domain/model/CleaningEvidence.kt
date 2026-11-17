package com.vivereply.roomready.domain.model

/**
 * Proof attached when a cleaning is finished: a local photo ref, a
 * skip reason, or both. At least one is required to finish; a supervisor
 * send-back clears it so the redo must re-prove.
 */
data class CleaningEvidence(
  val unitId: String,
  val photoRef: String?,
  val skipReason: String?,
  val finishedBy: String,
  val finishedAtEpochMillis: Long,
)
