package com.vivereply.roomready.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vivereply.roomready.domain.model.TicketStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
  @Query("SELECT * FROM tickets WHERE status = 'OPEN' ORDER BY createdAt ASC, id ASC")
  fun observeOpen(): Flow<List<TicketEntity>>

  @Query("SELECT * FROM tickets WHERE id = :id")
  suspend fun getById(id: String): TicketEntity?

  @Insert
  suspend fun insert(ticket: TicketEntity)

  @Query("UPDATE tickets SET status = :status, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateStatus(id: String, status: TicketStatus, updatedAt: Long)
}
