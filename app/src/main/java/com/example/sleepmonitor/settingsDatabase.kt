package com.example.sleepmonitor
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Settings::class], version = 2) // Updated from version 1 to 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "settings_database"
                )
                    .addMigrations(MIGRATION_1_2) // Add migration for schema change
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 (threshold as Double) to version 2 (threshold as Int)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table with the updated schema (threshold as INTEGER)
                database.execSQL("""
                    CREATE TABLE settings_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        threshold INTEGER NOT NULL, -- Changed from REAL to INTEGER
                        mobileNumber TEXT NOT NULL,
                        alarmDuration INTEGER NOT NULL
                    )
                """.trimIndent())

                // Copy data from old table to new table, converting threshold to INTEGER
                database.execSQL("""
                    INSERT INTO settings_new (id, threshold, mobileNumber, alarmDuration)
                    SELECT id, CAST(threshold AS INTEGER), mobileNumber, alarmDuration
                    FROM settings
                """.trimIndent())

                // Drop the old table
                database.execSQL("DROP TABLE settings")

                // Rename the new table to the original name
                database.execSQL("ALTER TABLE settings_new RENAME TO settings")
            }
        }
    }
}