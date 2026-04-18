package com.vigilante.shiftsalaryplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vigilante.shiftsalaryplanner.settings.AppProfileStore
import com.vigilante.shiftsalaryplanner.settings.scopedDatabaseName

@Database(
    entities = [
        ShiftDayEntity::class,
        ShiftTemplateEntity::class,
        HolidayEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shiftDayDao(): ShiftDayDao
    abstract fun shiftTemplateDao(): ShiftTemplateDao
    abstract fun holidayDao(): HolidayDao

    companion object {
        @Volatile
        private var INSTANCES: MutableMap<String, AppDatabase> = mutableMapOf()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shift_templates (
                        code TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        totalHours REAL NOT NULL,
                        breakHours REAL NOT NULL,
                        nightHours REAL NOT NULL,
                        colorHex TEXT NOT NULL,
                        isWeekendPaid INTEGER NOT NULL,
                        active INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE shift_templates
                    ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'TEXT'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS holiday_days (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        title TEXT NOT NULL,
                        scopeCode TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        isNonWorking INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(
            context: Context,
            profileId: String = AppProfileStore.resolveActiveProfileId(context)
        ): AppDatabase {
            val dbName = scopedDatabaseName(profileId)
            return INSTANCES[dbName] ?: synchronized(this) {
                INSTANCES[dbName] ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { built ->
                        INSTANCES[dbName] = built
                    }
            }
        }
    }
}
