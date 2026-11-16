package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.sync.SyncEntityType
import com.example.data.sync.SyncEntryStatus
import com.example.data.sync.SyncOp

/** One pending outbound PMS mutation. Payloads never contain credentials. */
@Entity(tableName = "sync_queue", indices = [Index("entityId"), Index("status"), Index("dueAt")])
data class SyncMutationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val entityType: SyncEntityType,
  val entityId: String,
  val op: SyncOp,
  val payloadJson: String,
  val createdAt: Long,
  val attempts: Int = 0,
  val dueAt: Long,
  val status: SyncEntryStatus = SyncEntryStatus.PENDING,
  val updatedAt: Long,
)
