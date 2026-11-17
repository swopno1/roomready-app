package com.vivereply.roomready.data.repository

import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.data.local.RoomUnitDao
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.local.TicketDao
import com.vivereply.roomready.data.local.TicketEntity
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.data.sync.SyncPayloads
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.Ticket
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.domain.state.TicketLifecycle
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ticket raise/complete errors (typed data, no user copy). */
sealed interface TicketActionError {
  data class UnknownUnit(val unitId: String) : TicketActionError

  data class UnknownTicket(val ticketId: String) : TicketActionError

  data object BlankNote : TicketActionError
}

class TicketActionException(val error: TicketActionError) : IllegalStateException(error.toString())

/**
 * Maintenance tickets: raise against a room, then open -> done.
 * Fully offline: every change is stored locally and queued for PMS sync.
 */
class TicketRepository(
  private val units: RoomUnitDao,
  private val tickets: TicketDao,
  private val activity: ActivityDao,
  private val queue: SyncQueueDao,
  private val clock: Clock,
) {

  fun observeOpenTickets(): Flow<List<Ticket>> =
    tickets.observeOpen().map { list -> list.map(TicketEntity::toDomain) }

  suspend fun getTicket(ticketId: String): Ticket? = tickets.getById(ticketId)?.toDomain()

  suspend fun raiseTicket(
    unitId: String,
    category: TicketCategory,
    note: String,
    photoRef: String?,
    actor: StaffMember,
  ): Result<Ticket> {
    val unit = units.getById(unitId)
      ?: return Result.failure(TicketActionException(TicketActionError.UnknownUnit(unitId)))
    val trimmedNote = note.trim()
    if (trimmedNote.isEmpty()) {
      return Result.failure(TicketActionException(TicketActionError.BlankNote))
    }
    val now = clock.nowEpochMillis()
    val ticket =
      Ticket(
        id = UUID.randomUUID().toString(),
        unitId = unitId,
        roomNumber = unit.number,
        category = category,
        note = trimmedNote,
        photoRef = photoRef?.trim()?.ifEmpty { null },
        status = TicketStatus.OPEN,
        createdBy = actor.name,
        createdAtEpochMillis = now,
        updatedAtEpochMillis = now,
      )
    tickets.insert(TicketEntity.fromDomain(ticket))
    activity.insert(
      ActivityEntity(
        type = ActivityType.TICKET_RAISED,
        actorName = actor.name,
        role = actor.role,
        unitId = unitId,
        roomNumber = unit.number,
        detail = category.name,
        createdAt = now,
      ),
    )
    queue.enqueue(
      SyncMutationEntity(
        entityType = SyncEntityType.TICKET,
        entityId = ticket.id,
        op = SyncOp.UPSERT,
        payloadJson = SyncPayloads.ticket(ticket),
        createdAt = now,
        dueAt = now,
        updatedAt = now,
      ),
    )
    return Result.success(ticket)
  }

  suspend fun markDone(ticketId: String, actor: StaffMember): Result<Ticket> {
    val ticket = tickets.getById(ticketId)
      ?: return Result.failure(TicketActionException(TicketActionError.UnknownTicket(ticketId)))
    TicketLifecycle.evaluate(ticket.status, TicketStatus.DONE).getOrElse {
      return Result.failure(it)
    }
    val now = clock.nowEpochMillis()
    tickets.updateStatus(ticketId, TicketStatus.DONE, now)
    activity.insert(
      ActivityEntity(
        type = ActivityType.TICKET_DONE,
        actorName = actor.name,
        role = actor.role,
        unitId = ticket.unitId,
        roomNumber = ticket.roomNumber,
        detail = null,
        createdAt = now,
      ),
    )
    queue.enqueue(
      SyncMutationEntity(
        entityType = SyncEntityType.TICKET_STATUS,
        entityId = ticketId,
        op = SyncOp.UPSERT,
        payloadJson = SyncPayloads.ticketStatus(ticketId, TicketStatus.DONE, actor.name, now),
        createdAt = now,
        dueAt = now,
        updatedAt = now,
      ),
    )
    return Result.success(ticket.toDomain().copy(status = TicketStatus.DONE, updatedAtEpochMillis = now))
  }

  /**
   * Applies a DONE discovered on the PMS during list reconciliation: the
   * server's status and done timestamp win. Queues nothing — the server
   * already knows. Already-DONE rows are a quiet success.
   */
  suspend fun applyServerDone(ticketId: String, doneBy: String, doneAt: Long): Result<Ticket> {
    val ticket = tickets.getById(ticketId)
      ?: return Result.failure(TicketActionException(TicketActionError.UnknownTicket(ticketId)))
    if (ticket.status == TicketStatus.DONE) return Result.success(ticket.toDomain())
    TicketLifecycle.evaluate(ticket.status, TicketStatus.DONE).getOrElse {
      return Result.failure(it)
    }
    tickets.updateStatus(ticketId, TicketStatus.DONE, doneAt)
    activity.insert(
      ActivityEntity(
        type = ActivityType.TICKET_DONE,
        actorName = doneBy,
        role = null,
        unitId = ticket.unitId,
        roomNumber = ticket.roomNumber,
        detail = null,
        createdAt = doneAt,
      ),
    )
    return Result.success(ticket.toDomain().copy(status = TicketStatus.DONE, updatedAtEpochMillis = doneAt))
  }
}
