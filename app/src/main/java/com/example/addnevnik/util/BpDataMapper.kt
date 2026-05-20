package com.example.addnevnik.util

import android.util.Log
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.data.local.DailyNoteEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Единый центр преобразования данных из БД в формат для UI/Отчетов.
 * Изолирует логику группировки и маппинга полей.
 */
object BpDataMapper {
    private const val TAG = "BpDataMapper"

    fun toMeasurementRows(
        entities: List<BloodPressureEntity>,
        dailyNotes: List<DailyNoteEntity>
    ): List<MeasurementRow> {
        Log.d(TAG, "toMeasurementRows called with ${entities.size} entities")
        if (entities.isEmpty()) {
            Log.d(TAG, "No entities, returning empty list")
            return emptyList()
        }

        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 1. Группировка по дням. Создаем новый Calendar для каждой записи, чтобы избежать побочных эффектов.
        val rawGroups = entities.groupBy { entity ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = entity.timestamp_ms
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        Log.d(TAG, "Grouped into ${rawGroups.size} days")

        // 2. Преобразование каждой группы в строку
        val result = rawGroups.map { (dateKey, entries) ->
            // Утро: до 12:00
            val morningEntry = entries.filter { entry ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = entry.timestamp_ms
                cal.get(Calendar.HOUR_OF_DAY) < 12
            }.let { list -> list.find { it.isPrimary } ?: list.firstOrNull() }

            // Вечер: с 12:00
            val eveningEntry = entries.filter { entry ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = entry.timestamp_ms
                cal.get(Calendar.HOUR_OF_DAY) >= 12
            }.let { list -> list.find { it.isPrimary } ?: list.firstOrNull() }

            // Поиск заметки за этот день
            val noteKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateKey))
            val dailyNote = dailyNotes.find { it.dateKey == noteKey }

            // Подсчет замеров (теперь все попадают в утро или вечер)
            val outOfRange = 0

            // === ГЛАВНОЕ: Маппинг полей ===
            MeasurementRow(
                date = dateKey,
                morningTime = morningEntry?.let { timeFmt.format(Date(it.timestamp_ms)) },
                eveningTime = eveningEntry?.let { timeFmt.format(Date(it.timestamp_ms)) },
                morningAd = morningEntry?.let { "${it.systolic}/${it.diastolic}" },
                morningHr = morningEntry?.pulse,
                eveningAd = eveningEntry?.let { "${it.systolic}/${it.diastolic}" },
                eveningHr = eveningEntry?.pulse,
                note = listOfNotNull(morningEntry?.tag, eveningEntry?.tag).firstOrNull { it.isNotBlank() },
                medication = dailyNote?.medication,
                wellbeing = dailyNote?.wellbeing,
                outOfRangeCount = outOfRange
            )
        }.sortedByDescending { it.date }

        Log.d(TAG, "Returning ${result.size} measurement rows")
        return result
    }
}
