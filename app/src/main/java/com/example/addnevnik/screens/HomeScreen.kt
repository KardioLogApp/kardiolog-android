package com.example.addnevnik.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.ui.dialogs.AddPressureDialog
import com.example.addnevnik.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onShowChart: () -> Unit,
    onScanRequest: () -> Unit,
    onShowReport: () -> Unit,
    scanResult: String?
) {
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val latestPressure by viewModel.latestPressure.collectAsStateWithLifecycle()
    val allPressure by viewModel.allPressure.collectAsStateWithLifecycle()
    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()

    if (showAddDialog) {
        AddPressureDialog(
            viewModel = viewModel,
            initialValues = scanResult,
            onScanRequest = onScanRequest,
            onDismiss = viewModel::dismissDialog,
            onConfirm = viewModel::savePressure
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("КардиоЛог", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* Избранное */ }) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2) Карточка "Последний замер"
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Последний замер",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (latestPressure == null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Нет данных. Добавьте первый замер.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${latestPressure!!.systolic} / ${latestPressure!!.diastolic}",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Пульс: ${latestPressure!!.pulse}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusChip(latestPressure!!.systolic, latestPressure!!.diastolic)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = formatTimestamp(latestPressure!!.timestamp_ms),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3) Кнопка "Добавить замер"
            item {
                Button(
                    onClick = { viewModel.onActionClick("pressure") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить замер", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 4) Кнопка "Распознать по фото"
            item {
                OutlinedButton(
                    onClick = onScanRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Распознать по фото", color = MaterialTheme.colorScheme.primary)
                }
            }

            // 5) Подзаголовок "Последние записи"
            item {
                Text(
                    text = "Последние записи",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 6) Список последних 3 замеров
            val lastEntries = allPressure.take(3)
            if (lastEntries.isEmpty() && latestPressure == null) {
                item {
                    Text(
                        text = "Здесь появятся ваши записи",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(lastEntries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entry.systolic} / ${entry.diastolic}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!entry.tag.isNullOrBlank()) {
                                    Text(
                                        text = entry.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                }
                                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.timestamp_ms))
                                val dailyNote = allDailyNotes.find { it.dateKey == dateKey }
                                if (dailyNote != null && dailyNote.medication.isNotBlank()) {
                                    Text(
                                        text = "Доп. препарат: ${dailyNote.medication}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "❤️ ${entry.pulse}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatTimestampShort(entry.timestamp_ms),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 7) Кнопка "Смотреть всю историю"
            item {
                TextButton(
                    onClick = onShowChart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Смотреть всю историю", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(systolic: Int, diastolic: Int) {
    val (status, color) = when {
        systolic < 90 || diastolic < 60 -> "Низкое" to MaterialTheme.colorScheme.secondary
        systolic <= 120 && diastolic <= 80 -> "Норма" to Color(0xFF00796B)
        systolic <= 140 && diastolic <= 90 -> "Повышенное" to Color(0xFFB45309)
        else -> "Высокое" to Color(0xFFB91C1C)
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMMM, HH:mm", Locale("ru"))
    return sdf.format(Date(timestamp))
}

private fun formatTimestampShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM, HH:mm", Locale("ru"))
    return sdf.format(Date(timestamp))
}
