package com.example.addnevnik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.addnevnik.data.repository.SettingsRepository
import com.example.addnevnik.model.SettingsUiState
import com.example.addnevnik.navigation.AppNavigation
import com.example.addnevnik.ui.theme.ADDnevnikTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Передаём управление insets приложению — клавиатура сдвигает контент, не перекрывает
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val app = application as AppDnevnikApplication
        val repository = SettingsRepository.getInstance(applicationContext, app.database.appDao())

        checkAndRequestNotificationPermission(repository)

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