package com.example.addnevnik.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.addnevnik.data.repository.NotesRepository
import com.example.addnevnik.data.repository.PressureRepository
import com.example.addnevnik.data.repository.SettingsRepository

class AppViewModelFactory(
    private val application: Application,
    private val pressureRepository: PressureRepository,
    private val notesRepository: NotesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(pressureRepository) as T
            modelClass.isAssignableFrom(NotesViewModel::class.java) ->
                NotesViewModel(notesRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(application, settingsRepository, pressureRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
