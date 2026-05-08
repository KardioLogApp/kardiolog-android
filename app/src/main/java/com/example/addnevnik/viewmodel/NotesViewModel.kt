package com.example.addnevnik.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addnevnik.data.repository.NotesRepository
import com.example.addnevnik.model.NoteItem
import com.example.addnevnik.model.NotesUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val notesRepository: NotesRepository) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    val state: StateFlow<NotesUiState> = notesRepository.notes
        .map { notes ->
            NotesUiState(notes = notes.toImmutableList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotesUiState()
        )

    fun onAddNoteClick() {
        _showAddDialog.value = true
    }

    fun dismissDialog() {
        _showAddDialog.value = false
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            notesRepository.addNote(title, content)
            _showAddDialog.value = false
        }
    }
}
