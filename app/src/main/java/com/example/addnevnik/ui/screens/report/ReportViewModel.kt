package com.example.addnevnik.ui.screens.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addnevnik.data.repository.PressureRepository
import com.example.addnevnik.util.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ReportViewModel(private val repository: PressureRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun generateReport(context: Context) {
        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            try {
                val data = repository.getAllPressureOnce()
                if (data.isEmpty()) {
                    _uiState.value = ReportUiState.Error("Нет данных для отчета")
                    return@launch
                }
                
                val generator = PdfReportGenerator(context)
                val file = generator.generateReport(data)
                
                if (file != null) {
                    _uiState.value = ReportUiState.Success(file)
                } else {
                    _uiState.value = ReportUiState.Error("Ошибка при создании файла")
                }
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
