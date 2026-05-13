package com.example.addnevnik.ui.screens.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addnevnik.data.repository.PressureRepository
import com.example.addnevnik.data.repository.SettingsRepository
import com.example.addnevnik.util.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.util.MeasurementRow
import com.example.addnevnik.util.PatientInfo

class ReportViewModel(
    private val repository: PressureRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun generateReport(context: Context) {
        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            try {
                val data = repository.getAllPressureOnce()
                val settings = settingsRepository.settings.first()
                if (data.isEmpty()) {
                    _uiState.value = ReportUiState.Error("Нет данных для отчета")
                    return@launch
                }
                
                val avgSys = data.map { it.systolic }.average().toInt()
                val avgDia = data.map { it.diastolic }.average().toInt()
                val avgPulse = data.map { it.pulse }.average().toInt()

                val patient = PatientInfo(
                    name = settings.profileName,
                    gender = settings.gender,
                    birthDate = settings.birthDate,
                    avgSystolic = avgSys,
                    avgDiastolic = avgDia,
                    avgHr = avgPulse
                )

                val rows = groupMeasurementsByDay(data)
                
                val file = PdfReportGenerator.generate(context, patient, rows)
                
                _uiState.value = ReportUiState.Success(file)
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private fun groupMeasurementsByDay(data: List<BloodPressureEntity>): List<MeasurementRow> {
        val calendar = Calendar.getInstance()
        
        val rawGroups = data.groupBy { 
            calendar.timeInMillis = it.timestamp_ms
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        
        return rawGroups.map { (dateKey, entries) ->
            val morningEntry = entries.filter {
                calendar.timeInMillis = it.timestamp_ms
                calendar.get(Calendar.HOUR_OF_DAY) in 5..11
            }.let { list -> list.find { it.isPrimary } ?: list.firstOrNull() }

            val eveningEntry = entries.filter {
                calendar.timeInMillis = it.timestamp_ms
                calendar.get(Calendar.HOUR_OF_DAY) in 17..23
            }.let { list -> list.find { it.isPrimary } ?: list.firstOrNull() }

            MeasurementRow(
                date = dateKey,
                morningAd = morningEntry?.let { "${it.systolic}/${it.diastolic}" },
                morningHr = morningEntry?.pulse,
                eveningAd = eveningEntry?.let { "${it.systolic}/${it.diastolic}" },
                eveningHr = eveningEntry?.pulse,
                note = listOfNotNull(morningEntry?.tag, eveningEntry?.tag).firstOrNull { it.isNotBlank() }
            )
        }.sortedByDescending { it.date }
    }

    fun shareReport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться отчетом"))
    }

    fun openReport(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}

sealed class ReportUiState {
    data object Idle : ReportUiState()
    data object Loading : ReportUiState()
    data class Success(val file: File) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}
