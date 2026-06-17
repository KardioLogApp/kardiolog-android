package com.example.addnevnik.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addnevnik.data.TestDataSeeder
import com.example.addnevnik.data.repository.SettingsRepository
import com.example.addnevnik.data.repository.PressureRepository
import com.example.addnevnik.model.SettingsUiState
import com.example.addnevnik.util.ReminderScheduler
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class SettingsViewModel(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val pressureRepository: PressureRepository
) : AndroidViewModel(application) {

    val state: StateFlow<SettingsUiState> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    fun loadTestData(onComplete: () -> Unit) {
        viewModelScope.launch {
            val app = getApplication<com.example.addnevnik.AppDnevnikApplication>()
            TestDataSeeder.loadTestData(app.database)
            onComplete()
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            val app = getApplication<com.example.addnevnik.AppDnevnikApplication>()
            app.database.appDao().clearAllBloodPressure()
            onComplete()
        }
    }

    fun updateMorningReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateMorningReminder(enabled, hour, minute)
            if (enabled) {
                ReminderScheduler.scheduleReminder(application, "morning", hour, minute)
            } else {
                ReminderScheduler.cancelReminder(application, "morning")
            }
        }
    }

    fun updateEveningReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateEveningReminder(enabled, hour, minute)
            if (enabled) {
                ReminderScheduler.scheduleReminder(application, "evening", hour, minute)
            } else {
                ReminderScheduler.cancelReminder(application, "evening")
            }
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotifications(enabled)
        }
    }

    fun updateTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTheme(enabled)
        }
    }

    fun updateProfileName(name: String) {
        viewModelScope.launch {
            settingsRepository.updateProfileName(name)
        }
    }

    fun updateGender(gender: String) {
        viewModelScope.launch {
            settingsRepository.updateGender(gender)
        }
    }

    fun updateBirthDate(date: String) {
        viewModelScope.launch {
            settingsRepository.updateBirthDate(date)
        }
    }

    fun enterEditMode() {
        viewModelScope.launch {
            settingsRepository.updateProfileStatus("Режим редактирования...")
        }
    }

    fun activatePremium(code: String): Boolean {
        val validCodes = listOf("CARDIO2026", "BETAUSER")
        return if (code.uppercase() in validCodes) {
            viewModelScope.launch {
                settingsRepository.activatePremium(code.uppercase())
            }
            true
        } else {
            false
        }
    }
}