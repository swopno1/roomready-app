package com.vivereply.roomready.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Offline-first local store: units, checklist, tickets, activity, sync queue. */
@Database(
  entities = [
    RoomTypeEntity::class,
    RoomUnitEntity::class,
    ChecklistStepEntity::class,
    TicketEntity::class,
    ActivityEntity::class,
    SyncMutationEntity::class,
    StaffRosterEntity::class,
    CleaningEvidenceEntity::class,
  ],
  version = 3,
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

  abstract fun staffRosterDao(): StaffRosterDao

  abstract fun cleaningEvidenceDao(): CleaningEvidenceDao

  companion object {
    const val DATABASE_NAME: String = "roomready.db"

    /**
     * v1 -> v2: PMS housekeeping timestamps on units, refusal reasons on
     * queued entries, and the staff roster table. Queued mutations survive.
     */
    val MIGRATION_1_2: Migration =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE room_units ADD COLUMN housekeepingAt INTEGER")
          db.execSQL("ALTER TABLE sync_queue ADD COLUMN lastError TEXT")
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `staff_roster` (`name` TEXT NOT NULL, " +
              "`role` TEXT NOT NULL, PRIMARY KEY(`name`))",
          )
        }
      }

    /** v2 -> v3: cleaning evidence table for finish photo/skip-reason proof. */
    val MIGRATION_2_3: Migration =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cleaning_evidence` (`unitId` TEXT NOT NULL, " +
              "`photoRef` TEXT, `skipReason` TEXT, `finishedBy` TEXT NOT NULL, " +
              "`finishedAt` INTEGER NOT NULL, PRIMARY KEY(`unitId`))",
          )
        }
      }

    fun create(context: Context): RoomReadyDatabase =
      Room.databaseBuilder(context, RoomReadyDatabase::class.java, DATABASE_NAME)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
  }
}
