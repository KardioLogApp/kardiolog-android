package com.example.addnevnik

import android.app.Application
import com.example.addnevnik.data.TestDataSeeder
import com.example.addnevnik.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppDnevnikApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем базу данных перед вызовом сидера
        val db = database
        
        // Запускаем заполнение базы в фоне один раз
        CoroutineScope(Dispatchers.IO).launch {
            TestDataSeeder.seedIfNecessary(this@AppDnevnikApplication, db)
        }
    }
}
