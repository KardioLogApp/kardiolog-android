package com.example.addnevnik.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.addnevnik.model.SettingsUiState
import com.example.addnevnik.ui.theme.ADDnevnikTheme

import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.clickable
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onNotificationsToggle: (Boolean) -> Unit,
    onThemeToggle: (Boolean) -> Unit,
    onMorningReminderToggle: (Boolean, Int, Int) -> Unit,
    onEveningReminderToggle: (Boolean, Int, Int) -> Unit,
    onEditClick: () -> Unit
) {
    var showMorningTimePicker by remember { mutableStateOf(false) }
    var showEveningTimePicker by remember { mutableStateOf(false) }

    if (showMorningTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.morningReminderHour,
            initialMinute = state.morningReminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showMorningTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onMorningReminderToggle(state.morningReminderEnabled, timePickerState.hour, timePickerState.minute)
                    showMorningTimePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showMorningTimePicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (showEveningTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.eveningReminderHour,
            initialMinute = state.eveningReminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEveningTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEveningReminderToggle(state.eveningReminderEnabled, timePickerState.hour, timePickerState.minute)
                    showEveningTimePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showEveningTimePicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Напоминания
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Напоминания",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Утреннее
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Утреннее напоминание")
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", state.morningReminderHour, state.morningReminderMinute),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { showMorningTimePicker = true }
                        )
                    }
                    Switch(
                        checked = state.morningReminderEnabled,
                        onCheckedChange = { onMorningReminderToggle(it, state.morningReminderHour, state.morningReminderMinute) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Вечернее
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Вечернее напоминание")
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", state.eveningReminderHour, state.eveningReminderMinute),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { showEveningTimePicker = true }
                        )
                    }
                    Switch(
                        checked = state.eveningReminderEnabled,
                        onCheckedChange = { onEveningReminderToggle(it, state.eveningReminderHour, state.eveningReminderMinute) }
                    )
                }
            }
        }

        // Профиль
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Профиль", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = state.profileName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = onEditClick) {
                        Text("Изменить")
                    }
                }
                if (state.profileStatus.isNotEmpty()) {
                    Text(
                        text = state.profileStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Настройки приложения
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Приложение",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Уведомления")
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = onNotificationsToggle
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Тёмная тема")
                    Switch(
                        checked = state.darkThemeEnabled,
                        onCheckedChange = onThemeToggle
                    )
                }
            }
        }

        // О программе
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Версия: ${state.appVersion}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ADDnevnikTheme {
        SettingsScreen(
            state = SettingsUiState(),
            onNotificationsToggle = {},
            onThemeToggle = {},
            onMorningReminderToggle = { _, _, _ -> },
            onEveningReminderToggle = { _, _, _ -> },
            onEditClick = {}
        )
    }
}
