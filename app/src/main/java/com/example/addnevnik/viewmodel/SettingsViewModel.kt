package com.example.addnevnik.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            // 1. Очистка
            pressureRepository.deleteAll()
            
            // 2. Настройка профиля
            settingsRepository.updateProfileName("Иванов Иван Иванович")
            settingsRepository.updateGender("Мужской")
            settingsRepository.updateBirthDate("15.03.1958")
            
            // 3. Генерация данных (14 дней)
            val entries = mutableListOf<com.example.addnevnik.data.local.BloodPressureEntity>()
            val comments = listOf("после кофе", "стресс", "после прогулки", null, null, null)
            
            for (i in 0 until 14) {
                val currentDay = Calendar.getInstance()
                currentDay.add(Calendar.DAY_OF_YEAR, -i)
                
                // Утро (1-2 замера)
                val morningCount = (1..2).random()
                for (j in 0 until morningCount) {
                    val entryCal = currentDay.clone() as Calendar
                    entryCal.set(Calendar.HOUR_OF_DAY, (7..9).random())
                    entryCal.set(Calendar.MINUTE, (0..59).random())
                    entries.add(generateRandomEntry(entryCal.timeInMillis, comments.random()))
                }
                
                // Вечер (0-1 замер)
                if ((0..10).random() > 2) { // 80% шанс наличия вечернего замера
                    val entryCal = currentDay.clone() as Calendar
                    entryCal.set(Calendar.HOUR_OF_DAY, (19..22).random())
                    entryCal.set(Calendar.MINUTE, (0..59).random())
                    entries.add(generateRandomEntry(entryCal.timeInMillis, comments.random()))
                }
            }
            
            pressureRepository.insertAll(entries)
            onComplete()
        }
    }

    private fun generateRandomEntry(timestamp: Long, tag: String?): com.example.addnevnik.data.local.BloodPressureEntity {
        return com.example.addnevnik.data.local.BloodPressureEntity(
            systolic = (115..145).random(),
            diastolic = (75..95).random(),
            pulse = (62..88).random(),
            timestamp_ms = timestamp,
            tag = tag
        )
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
