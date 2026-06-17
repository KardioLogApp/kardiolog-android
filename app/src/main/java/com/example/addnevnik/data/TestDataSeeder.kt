package com.example.addnevnik.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.addnevnik.data.local.AppDatabase
import com.example.addnevnik.data.local.BloodPressureEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object TestDataSeeder {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SEED_DONE = "is_test_data_seeded"

    /**
     * Автоматическое заполнение при первом запуске.
     * НЕ удаляет данные пользователя.
     */
    suspend fun seedIfNecessary(context: Context, database: AppDatabase) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEED_DONE, false)) return

        withContext(Dispatchers.IO) {
            val dao = database.appDao()
            val existingData = dao.getAllBloodPressureOnce()

            if (existingData.isNotEmpty()) {
                Log.d("TestDataSeeder", "Database has data. Skipping auto-seed.")
                prefs.edit().putBoolean(KEY_SEED_DONE, true).apply()
                return@withContext
            }

            Log.d("TestDataSeeder", "Database empty. Seeding test data...")
            fillDatabase(dao)
            
            prefs.edit().putBoolean(KEY_SEED_DONE, true).apply()
        }
    }

    /**
     * Ручная загрузка через настройки.
     * Очищает текущие данные и заполняет новые.
     */
    suspend fun loadTestData(database: AppDatabase) {
        withContext(Dispatchers.IO) {
            Log.d("TestDataSeeder", "Manual test data load requested.")
            val dao = database.appDao()
            dao.clearAllBloodPressure() 
            fillDatabase(dao)
            Log.d("TestDataSeeder", "✅ Manual test data load complete.")
        }
    }

    private suspend fun fillDatabase(dao: com.example.addnevnik.data.local.AppDao) {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 3600 * 1000L
        val morningHour = 8L * 3600 * 1000  // 08:00
        val eveningHour = 20L * 3600 * 1000  // 20:00

        // Получаем начало сегодняшнего дня (00:00)
        val todayStart = (now / dayMs) * dayMs

        for (i in 1..365) {
            val dayStart = todayStart - (i * dayMs)
            val sys = 120 + Random.nextInt(-25, 35)
            val dia = 80 + Random.nextInt(-15, 25)
            val pulse = 65 + Random.nextInt(-10, 25)

            // Утренняя запись (08:00)
            dao.insertBloodPressure(
                BloodPressureEntity(
                    id = 0,
                    timestamp_ms = dayStart + morningHour,
                    systolic = sys,
                    diastolic = dia,
                    pulse = pulse,
                    tag = "TestSeed",
                    isPrimary = true
                )
            )

            // Вечерняя запись (20:00) — немного другие значения
            dao.insertBloodPressure(
                BloodPressureEntity(
                    id = 0,
                    timestamp_ms = dayStart + eveningHour,
                    systolic = sys + Random.nextInt(-5, 5),
                    diastolic = dia + Random.nextInt(-3, 3),
                    pulse = pulse + Random.nextInt(-5, 5),
                    tag = "TestSeed",
                    isPrimary = true
                )
            )
        }
    }
}