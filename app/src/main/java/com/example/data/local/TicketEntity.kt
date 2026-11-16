package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Ticket
import com.example.domain.model.TicketCategory
import com.example.domain.model.TicketStatus

@Entity(tableName = "tickets", indices = [Index("unitId"), Index("status")])
data class TicketEntity(
  @PrimaryKey val id: String,
  val unitId: String,
  val roomNumber: String,
  val category: TicketCategory,
  val note: String,
  val photoRef: String?,
  val status: TicketStatus,
  val createdBy: String,
  val createdAt: Long,
  val updatedAt: Long,
) {
  fun toDomain(): Ticket =
    Ticket(
      id = id,
      unitId = unitId,
      roomNumber = roomNumber,
      category = category,
      note = note,
      photoRef = photoRef,
      status = status,
      createdBy = createdBy,
      createdAtEpochMillis = createdAt,
      updatedAtEpochMillis = updatedAt,
    )

  companion object {
    fun fromDomain(ticket: Ticket): TicketEntity =
      TicketEntity(
        id = ticket.id,
        unitId = ticket.unitId,
        roomNumber = ticket.roomNumber,
        category = ticket.category,
        note = ticket.note,
        photoRef = ticket.photoRef,
        status = ticket.status,
        createdBy = ticket.createdBy,
        createdAt = ticket.createdAtEpochMillis,
        updatedAt = ticket.updatedAtEpochMillis,
      )
  }
}
