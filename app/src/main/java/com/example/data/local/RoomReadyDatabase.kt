package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** Offline-first local store: units, checklist, tickets, activity, sync queue. */
@Database(
  entities = [
    RoomTypeEntity::class,
    RoomUnitEntity::class,
    ChecklistStepEntity::class,
    TicketEntity::class,
    ActivityEntity::class,
    SyncMutationEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class RoomReadyDatabase : RoomDatabase() {
  abstract fun roomTypeDao(): RoomTypeDao

  abstract fun roomUnitDao(): RoomUnitDao

  abstract fun checklistDao(): ChecklistDao

  abstract fun ticketDao(): TicketDao

  abstract fun activityDao(): ActivityDao

  abstract fun syncQueueDao(): SyncQueueDao

  companion object {
    const val DATABASE_NAME: String = "roomready.db"

    fun create(context: Context): RoomReadyDatabase =
      Room.databaseBuilder(context, RoomReadyDatabase::class.java, DATABASE_NAME).build()
  }
}
