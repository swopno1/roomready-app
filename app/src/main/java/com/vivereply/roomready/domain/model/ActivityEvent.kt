package com.vivereply.roomready.domain.model

/** Kinds of offline activity recorded in the activity log. */
enum class ActivityType {
  STATUS_CHANGED,
  CHECKLIST_COMPLETED,
  TICKET_RAISED,
  TICKET_DONE,
  SENT_BACK,
  BOOTSTRAP_IMPORTED,
  SYNC_COMPLETED,
}

/** One immutable activity-log entry. */
data class ActivityEvent(
  val id: Long = 0L,
  val type: ActivityType,
  val actorName: String,
  val role: StaffRole?,
  val unitId: String?,
  val roomNumber: String?,
  val detail: String?,
  val createdAtEpochMillis: Long,
)
