package com.example.addnevnik

import android.app.Application
import com.example.addnevnik.data.local.AppDatabase

class AppDnevnikApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
