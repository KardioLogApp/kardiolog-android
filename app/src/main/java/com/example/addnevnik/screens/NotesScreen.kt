package com.example.addnevnik.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.addnevnik.model.NotesUiState
import com.example.addnevnik.ui.dialogs.AddNoteDialog
import com.example.addnevnik.ui.theme.ADDnevnikTheme
import com.example.addnevnik.viewmodel.NotesViewModel

@Composable
fun NotesScreen(
    viewModel: NotesViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()

    NotesScreenContent(
        state = state,
        showAddDialog = showAddDialog,
        onAddNoteClick = viewModel::onAddNoteClick,
        onDismissDialog = viewModel::dismissDialog,
        onConfirmNote = viewModel::addNote
    )
}

@Composable
fun NotesScreenContent(
    state: NotesUiState,
    showAddDialog: Boolean,
    onAddNoteClick: () -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmNote: (String, String) -> Unit
) {
    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = onDismissDialog,
            onConfirm = onConfirmNote
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNoteClick) {
                Icon(Icons.Default.Add, contentDescription = "Добавить заметку")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Ваши заметки",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.notes, key = { it.id }) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesScreenPreview() {
    ADDnevnikTheme {
        NotesScreenContent(
            state = NotesUiState(),
            showAddDialog = false,
            onAddNoteClick = {},
            onDismissDialog = {},
            onConfirmNote = { _, _ -> }
        )
    }
}
