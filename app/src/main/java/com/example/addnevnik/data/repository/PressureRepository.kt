package com.example.addnevnik.data.repository

import com.example.addnevnik.data.local.AppDao
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.domain.BpStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PressureRepository private constructor(private val appDao: AppDao) {
    val latestPressure: Flow<BloodPressureEntity?> = appDao.getLatestBloodPressure()
    val allPressure: Flow<List<BloodPressureEntity>> = appDao.getAllBloodPressure()

    suspend fun getAllPressureOnce(): List<BloodPressureEntity> = appDao.getAllBloodPressureOnce()

    val lastPressure: Flow<String> = appDao.getAllBloodPressure().map { list ->
        list.firstOrNull()?.let {
            "${it.systolic}/${it.diastolic} мм рт. ст."
        } ?: "Нет данных"
    }

    private val _statusSummary = MutableStateFlow("Сегодня ваше состояние стабильное")
    val statusSummary: StateFlow<String> = _statusSummary.asStateFlow()

    suspend fun updatePressure(
        systolic: Int,
        diastolic: Int,
        pulse: Int,
        tag: String? = null,
        isPrimary: Boolean = false,
        isManual: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = BloodPressureEntity(
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            timestamp_ms = timestamp,
            tag = tag,
            isPrimary = isPrimary || isManual, // Ручной ввод всегда основной
            isManual = isManual
        )
        if (entry.isPrimary) {
            setPrimaryForEntry(entry)
        } else {
            appDao.insertBloodPressure(entry)
        }
    }

    suspend fun setPrimary(targetEntry: BloodPressureEntity) {
        val newPrimaryStatus = !targetEntry.isPrimary
        if (!newPrimaryStatus) {
            // Снимаем пометку
            appDao.updateBloodPressure(targetEntry.copy(isPrimary = false))
        } else {
            setPrimaryForEntry(targetEntry.copy(isPrimary = true))
        }
    }

    private suspend fun setPrimaryForEntry(targetEntry: BloodPressureEntity) {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = targetEntry.timestamp_ms
        val day = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        // Утро: 06:00–11:59
        // Вечер: 12:00–23:59
        val isMorning = hour in 6..11
        val isEvening = hour in 12..23
        
        if (!isMorning && !isEvening) {
            // Замеры вне слотов не могут быть основными
            if (targetEntry.id == 0) {
                appDao.insertBloodPressure(targetEntry.copy(isPrimary = false))
            } else {
                appDao.updateBloodPressure(targetEntry.copy(isPrimary = false))
            }
            return
        }

        val all = appDao.getAllBloodPressureOnce()
        val sameSlotEntries = all.filter {
            val c = java.util.Calendar.getInstance()
            c.timeInMillis = it.timestamp_ms
            val h = c.get(java.util.Calendar.HOUR_OF_DAY)
            c.get(java.util.Calendar.DAY_OF_YEAR) == day && 
            c.get(java.util.Calendar.YEAR) == year &&
            (if (isMorning) h in 6..11 else h in 12..23)
        }

        // Снимаем isPrimary у всех в этом слоте
        sameSlotEntries.forEach { 
            if (it.id != targetEntry.id) {
                appDao.updateBloodPressure(it.copy(isPrimary = false))
            }
        }
        
        if (targetEntry.id == 0) {
            appDao.insertBloodPressure(targetEntry.copy(isPrimary = true))
        } else {
            appDao.updateBloodPressure(targetEntry.copy(isPrimary = true))
        }
    }

    suspend fun insertAll(entries: List<BloodPressureEntity>) {
        entries.forEach { appDao.insertBloodPressure(it) }
    }

    suspend fun deletePressure(entry: BloodPressureEntity) {
        appDao.deleteBloodPressure(entry)
    }

    suspend fun deleteAll() {
        val all = appDao.getAllBloodPressureOnce()
        all.forEach { appDao.deleteBloodPressure(it) }
    }

    fun updateStatus(newStatus: String) {
        _statusSummary.value = newStatus
    }

    fun getStats(): Flow<BpStats> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        return combine(
            appDao.getBloodPressureAfter(now - 7 * dayMs),
            appDao.getBloodPressureAfter(now - 14 * dayMs),
            appDao.getBloodPressureAfter(now - 30 * dayMs),
            appDao.getBloodPressureCount(),
            allPressure
        ) { list7, list14, list30, count, all ->
            BpStats(
                avgSystolic7 = if (list7.isEmpty()) null else list7.map { it.systolic }.average().toInt(),
                avgDiastolic7 = if (list7.isEmpty()) null else list7.map { it.diastolic }.average().toInt(),
                avgPulse7 = if (list7.isEmpty()) null else list7.map { it.pulse }.average().toInt(),
                avgSystolic14 = if (list14.isEmpty()) null else list14.map { it.systolic }.average().toInt(),
                avgDiastolic14 = if (list14.isEmpty()) null else list14.map { it.diastolic }.average().toInt(),
                avgPulse14 = if (list14.isEmpty()) null else list14.map { it.pulse }.average().toInt(),
                avgSystolic30 = if (list30.isEmpty()) null else list30.map { it.systolic }.average().toInt(),
                avgDiastolic30 = if (list30.isEmpty()) null else list30.map { it.diastolic }.average().toInt(),
                avgPulse30 = if (list30.isEmpty()) null else list30.map { it.pulse }.average().toInt(),
                minSystolic = all.minOfOrNull { it.systolic },
                maxSystolic = all.maxOfOrNull { it.systolic },
                minDiastolic = all.minOfOrNull { it.diastolic },
                maxDiastolic = all.maxOfOrNull { it.diastolic },
                totalCount = count
            )
        }
    }

    companion object {
        private var instance: PressureRepository? = null
        fun getInstance(appDao: AppDao): PressureRepository {
            return instance ?: synchronized(this) {
                instance ?: PressureRepository(appDao).also { instance = it }
            }
        }

        fun getInstance(context: android.content.Context): PressureRepository {
            val app = context.applicationContext as com.example.addnevnik.AppDnevnikApplication
            return getInstance(app.database.appDao())
        }
    }
}
