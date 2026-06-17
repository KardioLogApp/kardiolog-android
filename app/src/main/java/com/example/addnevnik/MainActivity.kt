package com.example.addnevnik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.addnevnik.data.local.AppDatabase
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.data.repository.SettingsRepository
import com.example.addnevnik.model.SettingsUiState
import com.example.addnevnik.navigation.AppNavigation
import com.example.addnevnik.ui.theme.ADDnevnikTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val app = application as AppDnevnikApplication
        val repository = SettingsRepository.getInstance(applicationContext, app.database.appDao())

        checkAndRequestNotificationPermission(repository)
        
        fillTestData(app)

        setContent {
            val settings by repository.settings.collectAsState(initial = SettingsUiState())
            ADDnevnikTheme(darkTheme = settings.darkThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun fillTestData(app: AppDnevnikApplication) {
        lifecycleScope.launch {
            val dao = app.database.appDao()
            
            val allData = dao.getAllBloodPressureOnce()
            if (allData.isNotEmpty()) {
                allData.forEach { dao.deleteBloodPressure(it) }
                Log.d("DB_FILL", "Cleared ${allData.size} old records")
            }
            
            val now = System.currentTimeMillis()
            // Генерируем 52 точки
            for (i in 0..51) {
                // Ставим точку каждые 3 дня, чтобы в "Неделю" попадало 2-3 точки
                val timestamp = now - (i * 3L * 24 * 3600 * 1000)
                
                // Генерируем "зубчатые" данные
                val baseSys = 120
                val baseDia = 80
                val randomSys = Random.nextInt(-20, 30) // Разброс -20..+30
                val randomDia = Random.nextInt(-15, 20) // Разброс -15..+20
                
                val sys = baseSys + randomSys
                val dia = baseDia + randomDia
                val pulse = 60 + Random.nextInt(0, 30)
                
                dao.insertBloodPressure(
                    BloodPressureEntity(
                        id = 0,
                        timestamp_ms = timestamp,
                        systolic = sys,
                        diastolic = dia,
                        pulse = pulse,
                        tag = "Test"
                    )
                )
            }
            Log.d("DB_FILL", "✅ Added 52 jagged test records. Latest is NOW.")
        }
    }

    private fun checkAndRequestNotificationPermission(repository: SettingsRepository) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                val settings = repository.settings.first()
                if (!settings.hasRequestedNotificationPermission) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    repository.setNotificationPermissionRequested()
                }
            }
        }
    }
}