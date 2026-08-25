package com.daycare.sleepcheck.log.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FacilityEntity::class, RoomEntity::class, StaffEntity::class, ChildEntity::class,
        SleepSessionEntity::class, CheckRecordEntity::class, CorrectionAuditEntity::class,
        PlaygroundChecklistEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun facilityDao(): FacilityDao
    abstract fun peopleDao(): PeopleDao
    abstract fun sleepDao(): SleepDao
    abstract fun auditDao(): AuditDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "daycare_sleep_check_log.db",
        ).addMigrations(MIGRATION_1_2).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE facilities SET intervalMinutes = 0 " +
                        "WHERE jurisdiction = 'ONTARIO' AND intervalMinutes = 15",
                )
            }
        }
    }
}
