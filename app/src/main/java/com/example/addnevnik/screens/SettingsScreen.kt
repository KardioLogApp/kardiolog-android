package com.example.addnevnik.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addnevnik.model.SettingsUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onNotificationsToggle: (Boolean) -> Unit,
    onThemeToggle: (Boolean) -> Unit,
    onMorningReminderToggle: (Boolean, Int, Int) -> Unit,
    onEveningReminderToggle: (Boolean, Int, Int) -> Unit,
    onEditClick: () -> Unit,
    onUpdateName: (String) -> Unit = {},
    onUpdateGender: (String) -> Unit = {},
    onUpdateBirthDate: (String) -> Unit = {},
    onLoadTestData: () -> Unit = {},
    onActivatePromo: (String) -> Boolean = { false }
) {
    var showMorningPicker by remember { mutableStateOf(false) }
    var showEveningPicker by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }
    var promoError by remember { mutableStateOf(false) }
    var promoSuccess by remember { mutableStateOf(false) }

    if (showPromoDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPromoDialog = false
                promoError = false
                promoCode = ""
            },
            title = { Text("Активация промокода") },
            text = {
                Column {
                    OutlinedTextField(
                        value = promoCode,
                        onValueChange = { 
                            promoCode = it
                            promoError = false
                        },
                        label = { Text("Введите код") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = promoError,
                        supportingText = {
                            if (promoError) Text("Неверный промокод", color = MaterialTheme.colorScheme.error)
                        }
                    )
                    if (promoSuccess) {
                        Text(
                            "Pro версия активирована ✓",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onActivatePromo(promoCode)) {
                        promoSuccess = true
                        promoError = false
                    } else {
                        promoError = true
                    }
                }) { Text("Активировать", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false }) { Text("Закрыть") }
            }
        )
    }

    if (showProfileDialog) {
        var tempName by remember { mutableStateOf(state.profileName) }
        var tempGender by remember { mutableStateOf(state.gender) }
        var tempBirthDate by remember { mutableStateOf(state.birthDate) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Данные пациента") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("ФИО") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempGender,
                        onValueChange = { tempGender = it },
                        label = { Text("Пол") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempBirthDate,
                        onValueChange = { tempBirthDate = it },
                        label = { Text("Дата рождения") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateName(tempName)
                    onUpdateGender(tempGender)
                    onUpdateBirthDate(tempBirthDate)
                    showProfileDialog = false
                }) { Text("Сохранить", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showMorningPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.morningReminderHour,
            initialMinute = state.morningReminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showMorningPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onMorningReminderToggle(state.morningReminderEnabled, timePickerState.hour, timePickerState.minute)
                    showMorningPicker = false
                }) { Text("ОК", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showMorningPicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (showEveningPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.eveningReminderHour,
            initialMinute = state.eveningReminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEveningPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEveningReminderToggle(state.eveningReminderEnabled, timePickerState.hour, timePickerState.minute)
                    showEveningPicker = false
                }) { Text("ОК", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showEveningPicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Профиль пациента",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clickable { showProfileDialog = true }
                        .padding(16.dp)
                ) {
                    Text(text = state.profileName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "${state.gender}, ${state.birthDate}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(
                        text = "Нажмите, чтобы изменить данные",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Text(
                text = "Напоминания",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ReminderRow(
                        title = "Утреннее напоминание",
                        hour = state.morningReminderHour,
                        minute = state.morningReminderMinute,
                        enabled = state.morningReminderEnabled,
                        onToggle = { onMorningReminderToggle(it, state.morningReminderHour, state.morningReminderMinute) },
                        onClick = { showMorningPicker = true },
                        primaryColor = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    ReminderRow(
                        title = "Вечернее напоминание",
                        hour = state.eveningReminderHour,
                        minute = state.eveningReminderMinute,
                        enabled = state.eveningReminderEnabled,
                        onToggle = { onEveningReminderToggle(it, state.eveningReminderHour, state.eveningReminderMinute) },
                        onClick = { showEveningPicker = true },
                        primaryColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Внешний вид",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Тёмная тема", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = state.darkThemeEnabled,
                        onCheckedChange = onThemeToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Text(
                text = "Инструменты",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clickable { onLoadTestData() }
                        .padding(16.dp)
                ) {
                    Text(text = "Загрузить тестовые данные", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Заполнит дневник за 14 дней для проверки",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CardioLog v1.0 · ${if (state.isPremium) "Pro версия" else "Бесплатная версия"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!state.isPremium) {
                    OutlinedButton(
                        onClick = { showPromoDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Активировать промокод", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    title: String,
    hour: Int,
    minute: Int,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                style = MaterialTheme.typography.bodyMedium,
                color = primaryColor,
                fontWeight = FontWeight.Medium
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
        )
    }
}
