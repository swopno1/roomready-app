package com.example.data.repository

import com.example.data.Clock
import com.example.data.local.ActivityDao
import com.example.data.local.ActivityEntity
import com.example.data.local.RoomUnitDao
import com.example.data.local.SyncMutationEntity
import com.example.data.local.SyncQueueDao
import com.example.data.local.TicketDao
import com.example.data.local.TicketEntity
import com.example.data.sync.SyncEntityType
import com.example.data.sync.SyncOp
import com.example.data.sync.SyncPayloads
import com.example.domain.model.ActivityType
import com.example.domain.model.StaffMember
import com.example.domain.model.Ticket
import com.example.domain.model.TicketCategory
import com.example.domain.model.TicketStatus
import com.example.domain.state.TicketLifecycle
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
}
