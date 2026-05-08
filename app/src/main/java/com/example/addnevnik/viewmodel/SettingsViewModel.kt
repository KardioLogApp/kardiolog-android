package com.example.addnevnik.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addnevnik.data.repository.SettingsRepository
import com.example.addnevnik.model.SettingsUiState
import com.example.addnevnik.util.ReminderScheduler
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    val state: StateFlow<SettingsUiState> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

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

    fun enterEditMode() {
        viewModelScope.launch {
            settingsRepository.updateProfileStatus("Режим редактирования...")
        }
    }
}
