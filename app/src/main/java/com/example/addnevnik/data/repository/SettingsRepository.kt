package com.example.addnevnik.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.addnevnik.data.local.AppDao
import com.example.addnevnik.data.local.ProfileEntity
import com.example.addnevnik.model.SettingsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context, private val appDao: AppDao) {

    private object PreferencesKeys {
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

    val settings: Flow<SettingsUiState> = combine(
        context.dataStore.data,
        appDao.getProfile()
    ) { preferences, profile ->
        val p = profile ?: ProfileEntity()
        SettingsUiState(
            profileName = p.name,
            gender = p.gender,
            birthDate = p.birthDate,
            profileStatus = p.status,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            darkThemeEnabled = preferences[PreferencesKeys.DARK_THEME_ENABLED] ?: false,
            morningReminderEnabled = preferences[PreferencesKeys.MORNING_REMINDER_ENABLED] ?: false,
            morningReminderHour = preferences[PreferencesKeys.MORNING_REMINDER_HOUR] ?: 8,
            morningReminderMinute = preferences[PreferencesKeys.MORNING_REMINDER_MINUTE] ?: 0,
            eveningReminderEnabled = preferences[PreferencesKeys.EVENING_REMINDER_ENABLED] ?: false,
            eveningReminderHour = preferences[PreferencesKeys.EVENING_REMINDER_HOUR] ?: 20,
            eveningReminderMinute = preferences[PreferencesKeys.EVENING_REMINDER_MINUTE] ?: 0,
            hasRequestedNotificationPermission = preferences[PreferencesKeys.HAS_REQUESTED_NOTIFICATION_PERMISSION] ?: false,
            isPremium = p.isPremium,
            promoCode = p.promoCode,
            premiumActivatedAt = p.premiumActivatedAt
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

    private suspend fun updateProfile(transform: (ProfileEntity) -> ProfileEntity) {
        val current = appDao.getProfile().first() ?: ProfileEntity()
        appDao.insertProfile(transform(current))
    }

    suspend fun updateProfileName(name: String) {
        updateProfile { it.copy(name = name) }
    }

    suspend fun updateGender(gender: String) {
        updateProfile { it.copy(gender = gender) }
    }

    suspend fun updateBirthDate(date: String) {
        updateProfile { it.copy(birthDate = date) }
    }

    suspend fun updateProfileStatus(status: String) {
        updateProfile { it.copy(status = status) }
    }

    suspend fun activatePremium(promoCode: String) {
        updateProfile {
            it.copy(
                isPremium = true,
                promoCode = promoCode,
                premiumActivatedAt = System.currentTimeMillis()
            )
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context, appDao: AppDao): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context, appDao).also { instance = it }
            }
        }

        fun getInstance(context: Context): SettingsRepository {
            val app = context.applicationContext as com.example.addnevnik.AppDnevnikApplication
            return getInstance(context, app.database.appDao())
        }
    }
}
