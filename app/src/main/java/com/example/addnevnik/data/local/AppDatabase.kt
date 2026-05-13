package com.example.addnevnik.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BloodPressureEntity::class, NoteEntity::class, ProfileEntity::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blood_pressure ADD COLUMN isManual INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `profile` (
                        `id` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `gender` TEXT NOT NULL, 
                        `birthDate` TEXT NOT NULL, 
                        `isPremium` INTEGER NOT NULL, 
                        `promoCode` TEXT, 
                        `premiumActivatedAt` INTEGER, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                // Insert default profile if not exists
                db.execSQL("INSERT OR IGNORE INTO profile (id, name, gender, birthDate, isPremium) VALUES (0, 'Иван Иванов', 'Мужской', '01.01.1980', 0)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profile ADD COLUMN status TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
