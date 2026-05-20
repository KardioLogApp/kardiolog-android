package com.example.addnevnik.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    onClearData: () -> Unit = {},
    onActivatePromo: (String) -> Boolean = { false }
) {
    var showMorningPicker by remember { mutableStateOf(false) }
    var showEveningPicker by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }
    var promoError by remember { mutableStateOf(false) }
    var promoSuccess by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = primaryColor,
        checkedBorderColor = primaryColor,
        checkedIconColor = primaryColor,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = Color(0xFFE0E0E0),
        uncheckedBorderColor = Color(0xFFBDBDBD),
        uncheckedIconColor = Color.White,
        disabledCheckedThumbColor = Color.White.copy(alpha = 0.7f),
        disabledCheckedTrackColor = primaryColor.copy(alpha = 0.5f),
        disabledUncheckedThumbColor = Color.White.copy(alpha = 0.7f),
        disabledUncheckedTrackColor = Color(0xFFE0E0E0).copy(alpha = 0.6f)
    )

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Очистка данных", fontWeight = FontWeight.Bold) },
            text = { Text("Вы уверены? Это удалит всю историю измерений безвозвратно.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showPromoDialog) {
        AlertDialog(
            onDismissRequest = {
                showPromoDialog = false
                promoError = false
                promoCode = ""
                promoSuccess = false
            },
            title = {
                Text(
                    "Активация промокода",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
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
                            if (promoError) {
                                Text(
                                    "Неверный промокод",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    if (promoSuccess) {
                        Text(
                            "Pro версия активирована ✓",
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onActivatePromo(promoCode)) {
                            promoSuccess = true
                            promoError = false
                        } else {
                            promoError = true
                            promoSuccess = false
                        }
                    }
                ) {
                    Text("Активировать", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (showProfileDialog) {
        var tempName by remember { mutableStateOf(state.profileName) }
        var tempGender by remember { mutableStateOf(state.gender) }
        var tempBirthDate by remember { mutableStateOf(state.birthDate) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    "Данные пациента",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("ФИО") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempGender,
                        onValueChange = { tempGender = it },
                        label = { Text("Пол") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempBirthDate,
                        onValueChange = { tempBirthDate = it },
                        label = { Text("Дата рождения") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateName(tempName)
                        onUpdateGender(tempGender)
                        onUpdateBirthDate(tempBirthDate)
                        showProfileDialog = false
                    }
                ) {
                    Text("Сохранить", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Отмена")
                }
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
                TextButton(
                    onClick = {
                        onMorningReminderToggle(
                            state.morningReminderEnabled,
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showMorningPicker = false
                    }
                ) {
                    Text("ОК", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMorningPicker = false }) {
                    Text("Отмена")
                }
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
                TextButton(
                    onClick = {
                        onEveningReminderToggle(
                            state.eveningReminderEnabled,
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showEveningPicker = false
                    }
                ) {
                    Text("ОК", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEveningPicker = false }) {
                    Text("Отмена")
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "Настройки",
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    fontSize = 22.sp
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("Профиль пациента")

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
                    Text(
                        text = state.profileName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.gender}, ${state.birthDate}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Нажмите, чтобы изменить данные",
                        color = primaryColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            SectionTitle("Напоминания")

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
                        onToggle = {
                            onMorningReminderToggle(
                                it,
                                state.morningReminderHour,
                                state.morningReminderMinute
                            )
                        },
                        onClick = { showMorningPicker = true },
                        primaryColor = primaryColor,
                        switchColors = switchColors
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    ReminderRow(
                        title = "Вечернее напоминание",
                        hour = state.eveningReminderHour,
                        minute = state.eveningReminderMinute,
                        enabled = state.eveningReminderEnabled,
                        onToggle = {
                            onEveningReminderToggle(
                                it,
                                state.eveningReminderHour,
                                state.eveningReminderMinute
                            )
                        },
                        onClick = { showEveningPicker = true },
                        primaryColor = primaryColor,
                        switchColors = switchColors
                    )
                }
            }

            SectionTitle("Внешний вид", topPadding = 8.dp)

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
                    Text(
                        "Тёмная тема",
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = state.darkThemeEnabled,
                        onCheckedChange = onThemeToggle,
                        colors = switchColors
                    )
                }
            }

            SectionTitle("Управление данными", topPadding = 8.dp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Кнопка загрузки тестовых данных
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLoadTestData() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Загрузить тестовые данные",
                                fontSize = 17.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Заполнит дневник за 30 дней",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Кнопка очистки
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearDataDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Очистить все данные",
                                fontSize = 17.sp,
                                color = Color(0xFFD32F2F), // Красный цвет для опасности
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Удалит всю историю измерений",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Информация о бекапе
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "Автосохранение данных",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ваша история и настройки автоматически сохраняются в Google Аккаунт. При переустановке приложения данные восстановятся.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CardioLog v1.0 · ${if (state.isPremium) "Pro версия" else "Бесплатная версия"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!state.isPremium) {
                    OutlinedButton(
                        onClick = { showPromoDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, primaryColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryColor
                        )
                    ) {
                        Text("Активировать промокод", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    topPadding: Dp = 0.dp
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = topPadding)
    )
}

@Composable
private fun ReminderRow(
    title: String,
    hour: Int,
    minute: Int,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    primaryColor: Color,
    switchColors: SwitchColors
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
            Text(
                text = title,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                color = primaryColor,
                fontWeight = FontWeight.Medium
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = switchColors
        )
    }
}