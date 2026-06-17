package com.example.addnevnik.model

import com.example.addnevnik.data.local.BloodPressureEntity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class HomeUiState(
    val greeting: String = "Добро пожаловать в КардиоЛог",
    val statusSummary: String = "Сегодня ваше состояние стабильное",
    val lastPressure: String = "120/80 мм рт. ст.",
    val quickActions: ImmutableList<QuickAction> = persistentListOf(
        QuickAction("Добавить замер", "pressure")
    ),
    val allPressure: List<BloodPressureEntity> = emptyList()
)

data class QuickAction(val label: String, val id: String)

data class NotesUiState(
    val notes: ImmutableList<NoteItem> = persistentListOf(
        NoteItem(1, "Утренняя прогулка", "Чувствую себя бодрее после 20 минут ходьбы."),
        NoteItem(2, "Реакция на кофе", "После второй чашки пульс участился до 90.")
    )
)

data class NoteItem(val id: Int, val title: String, val content: String)

data class SettingsUiState(
    val profileName: String = "Иван Иванов",
    val gender: String = "Мужской",
    val birthDate: String = "01.01.1980",
    val profileStatus: String = "",
    val notificationsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = false,
    val appVersion: String = "0.1.0",
    val morningReminderEnabled: Boolean = false,
    val morningReminderHour: Int = 8,
    val morningReminderMinute: Int = 0,
    val eveningReminderEnabled: Boolean = false,
    val eveningReminderHour: Int = 20,
    val eveningReminderMinute: Int = 0,
    val hasRequestedNotificationPermission: Boolean = false,
    val isPremium: Boolean = false,
    val promoCode: String? = null,
    val premiumActivatedAt: Long? = null
)
