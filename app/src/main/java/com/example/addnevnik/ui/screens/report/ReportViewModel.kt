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
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.data.local.DailyNoteEntity
import com.example.addnevnik.util.BpDataMapper
import com.example.addnevnik.util.MeasurementRow
import com.example.addnevnik.util.PatientInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
                val dailyNotes = repository.allDailyNotes.first()
                if (data.isEmpty()) {
                    _uiState.value = ReportUiState.Error("Нет данных для отчета")
                    return@launch
                }
                
                // Используем единый Mapper
                val rows = BpDataMapper.toMeasurementRows(data, dailyNotes)

                // Averages computed only from entries shown in the table (morning + evening primary)
                // to ensure consistency between summary numbers and table values
                val tableEntries = rows.flatMap { row ->
                    listOfNotNull(
                        row.morningAd?.let { ad ->
                            val p = ad.split("/")
                            Triple(p.getOrNull(0)?.toIntOrNull(), p.getOrNull(1)?.toIntOrNull(), row.morningHr)
                        },
                        row.eveningAd?.let { ad ->
                            val p = ad.split("/")
                            Triple(p.getOrNull(0)?.toIntOrNull(), p.getOrNull(1)?.toIntOrNull(), row.eveningHr)
                        }
                    )
                }
                val avgSys   = if (tableEntries.isNotEmpty()) tableEntries.mapNotNull { it.first }.average().toInt() else data.map { it.systolic }.average().toInt()
                val avgDia   = if (tableEntries.isNotEmpty()) tableEntries.mapNotNull { it.second }.average().toInt() else data.map { it.diastolic }.average().toInt()
                val avgPulse = if (tableEntries.isNotEmpty()) tableEntries.mapNotNull { it.third }.average().toInt() else data.map { it.pulse }.average().toInt()

                val patient = PatientInfo(
                    name = settings.profileName,
                    gender = settings.gender,
                    birthDate = settings.birthDate,
                    avgSystolic = avgSys,
                    avgDiastolic = avgDia,
                    avgHr = avgPulse
                )
                
                val file = PdfReportGenerator.generate(context, patient, rows)
                
                _uiState.value = ReportUiState.Success(file)
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
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
