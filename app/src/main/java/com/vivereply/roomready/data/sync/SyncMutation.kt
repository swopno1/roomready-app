package com.vivereply.roomready.data.sync

/** Entity kinds that can sit in the outbound sync queue. */
enum class SyncEntityType {
  UNIT_STATUS,
  CHECKLIST,
  TICKET,
  TICKET_STATUS,
}

/** Mutation operation carried by a queue entry. */
enum class SyncOp {
  UPSERT,
}

/** Lifecycle of one queued mutation. */
enum class SyncEntryStatus {
  PENDING,
  IN_FLIGHT,
  FAILED,
}

/**
 * One mutation handed to the PMS transport.
 * By construction it carries no credentials: auth is applied at the
 * transport layer from [com.vivereply.roomready.security.SecureCredentialStore].
 */
data class OutgoingMutation(
  val queueId: Long,
  val entityType: SyncEntityType,
  val entityId: String,
  val op: SyncOp,
  val payloadJson: String,
)
