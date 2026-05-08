package com.example.addnevnik.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.addnevnik.model.SettingsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_STATUS = stringPreferencesKey("profile_status")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val MORNING_REMINDER_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
        val MORNING_REMINDER_HOUR = intPreferencesKey("morning_reminder_hour")
        val MORNING_REMINDER_MINUTE = intPreferencesKey("morning_reminder_minute")
        val EVENING_REMINDER_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
        val EVENING_REMINDER_HOUR = intPreferencesKey("evening_reminder_hour")
        val EVENING_REMINDER_MINUTE = intPreferencesKey("evening_reminder_minute")
        val HAS_REQUESTED_NOTIFICATION_PERMISSION = booleanPreferencesKey("has_requested_notification_permission")
    }

    val settings: Flow<SettingsUiState> = context.dataStore.data
        .map { preferences ->
            SettingsUiState(
                profileName = preferences[PreferencesKeys.PROFILE_NAME] ?: "Иван Иванов",
                profileStatus = preferences[PreferencesKeys.PROFILE_STATUS] ?: "",
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                darkThemeEnabled = preferences[PreferencesKeys.DARK_THEME_ENABLED] ?: false,
                morningReminderEnabled = preferences[PreferencesKeys.MORNING_REMINDER_ENABLED] ?: false,
                morningReminderHour = preferences[PreferencesKeys.MORNING_REMINDER_HOUR] ?: 8,
                morningReminderMinute = preferences[PreferencesKeys.MORNING_REMINDER_MINUTE] ?: 0,
                eveningReminderEnabled = preferences[PreferencesKeys.EVENING_REMINDER_ENABLED] ?: false,
                eveningReminderHour = preferences[PreferencesKeys.EVENING_REMINDER_HOUR] ?: 20,
                eveningReminderMinute = preferences[PreferencesKeys.EVENING_REMINDER_MINUTE] ?: 0,
                hasRequestedNotificationPermission = preferences[PreferencesKeys.HAS_REQUESTED_NOTIFICATION_PERMISSION] ?: false
            )
        }

    suspend fun setNotificationPermissionRequested() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_REQUESTED_NOTIFICATION_PERMISSION] = true
        }
    }

    suspend fun updateMorningReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MORNING_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.MORNING_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.MORNING_REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateEveningReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EVENING_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.EVENING_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.EVENING_REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME_ENABLED] = enabled
        }
    }

    suspend fun updateProfileStatus(status: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_STATUS] = status
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null
        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
        }
    }
}
