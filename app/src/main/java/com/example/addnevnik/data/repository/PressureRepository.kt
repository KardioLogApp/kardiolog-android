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

    suspend fun updatePressure(systolic: Int, diastolic: Int, pulse: Int, tag: String? = null) {
        appDao.insertBloodPressure(
            BloodPressureEntity(
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                timestamp_ms = System.currentTimeMillis(),
                tag = tag
            )
        )
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
            appDao.getBloodPressureCount()
        ) { list7, list14, list30, count ->
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
    }
}
