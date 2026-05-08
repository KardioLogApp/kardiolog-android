package com.example.addnevnik.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.domain.BpCategory
import com.example.addnevnik.domain.BpClassifier
import com.example.addnevnik.domain.BpStats
import com.example.addnevnik.data.repository.PressureRepository
import com.example.addnevnik.model.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val pressureRepository: PressureRepository) : ViewModel() {
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    val latestPressure: StateFlow<BloodPressureEntity?> = pressureRepository.latestPressure
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val bpStats: StateFlow<BpStats> = pressureRepository.getStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BpStats(null, null, null, null, null, null, null, null, null, 0)
        )

    val bpCategory: StateFlow<BpCategory?> = latestPressure
        .map { it?.let { BpClassifier.classify(it.systolic, it.diastolic) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allPressure: StateFlow<List<BloodPressureEntity>> = pressureRepository.allPressure
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val state: StateFlow<HomeUiState> = combine(
        pressureRepository.lastPressure,
        pressureRepository.statusSummary,
        allPressure
    ) { pressure, summary, all ->
        HomeUiState(
            lastPressure = pressure,
            statusSummary = summary,
            allPressure = all
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onActionClick(actionId: String) {
        when (actionId) {
            "pressure" -> _showAddDialog.value = true
            "meds" -> pressureRepository.updateStatus("Лекарства приняты")
            "note" -> pressureRepository.updateStatus("Заметка добавлена")
        }
    }

    fun dismissDialog() {
        _showAddDialog.value = false
    }

    fun savePressure(systolic: Int, diastolic: Int, pulse: Int, tag: String? = null) {
        viewModelScope.launch {
            pressureRepository.updatePressure(systolic, diastolic, pulse, tag)
            _showAddDialog.value = false
        }
    }
}
