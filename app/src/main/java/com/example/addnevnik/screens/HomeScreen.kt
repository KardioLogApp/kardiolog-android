package com.example.addnevnik.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.domain.BpCategory
import com.example.addnevnik.domain.BpStats
import com.example.addnevnik.model.HomeUiState
import com.example.addnevnik.ui.dialogs.AddPressureDialog
import com.example.addnevnik.ui.theme.ADDnevnikTheme
import com.example.addnevnik.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onShowChart: () -> Unit,
    onScanRequest: () -> Unit,
    onShowReport: () -> Unit,
    scanResult: String?
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val latestPressure by viewModel.latestPressure.collectAsStateWithLifecycle()
    val bpStats by viewModel.bpStats.collectAsStateWithLifecycle()
    val bpCategory by viewModel.bpCategory.collectAsStateWithLifecycle()

    HomeScreenContent(
        state = state,
        latestPressure = latestPressure,
        bpStats = bpStats,
        bpCategory = bpCategory,
        showAddDialog = showAddDialog,
        scanResult = scanResult,
        onActionClick = viewModel::onActionClick,
        onDismissDialog = viewModel::dismissDialog,
        onConfirmPressure = viewModel::savePressure,
        onShowChart = onShowChart,
        onScanRequest = onScanRequest,
        onShowReport = onShowReport
    )
}

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    latestPressure: BloodPressureEntity?,
    bpStats: BpStats,
    bpCategory: BpCategory?,
    showAddDialog: Boolean,
    scanResult: String?,
    onActionClick: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmPressure: (Int, Int, Int, String?) -> Unit,
    onShowChart: () -> Unit,
    onScanRequest: () -> Unit,
    onShowReport: () -> Unit
) {
    if (showAddDialog) {
        AddPressureDialog(
            initialValues = scanResult,
            onScanRequest = onScanRequest,
            onDismiss = onDismissDialog,
            onConfirm = onConfirmPressure
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onShowReport) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Отчет",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onShowChart) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "График",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (latestPressure == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Нет измерений. Нажмите кнопку ниже.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = bpCategory?.color?.copy(alpha = 0.1f) ?: MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Последний замер",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${latestPressure.systolic}/${latestPressure.diastolic}",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bpCategory?.color ?: MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "  ${latestPressure.pulse}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Text(
                            text = getCategoryName(bpCategory),
                            style = MaterialTheme.typography.titleMedium,
                            color = bpCategory?.color ?: MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (latestPressure.tag != null) {
                            Text(
                                text = "#${latestPressure.tag}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Text(
                            text = formatTimestamp(latestPressure.timestamp_ms),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Средние значения", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    StatsRow(label = "7 дней", systolic = bpStats.avgSystolic7, diastolic = bpStats.avgDiastolic7)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    StatsRow(label = "14 дней", systolic = bpStats.avgSystolic14, diastolic = bpStats.avgDiastolic14)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    StatsRow(label = "30 дней", systolic = bpStats.avgSystolic30, diastolic = bpStats.avgDiastolic30)
                }
            }
        }

        item {
            Text(text = "История замеров", style = MaterialTheme.typography.titleLarge)
        }

        items(state.allPressure.take(5)) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${entry.systolic}/${entry.diastolic}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (entry.tag != null) {
                            Text(
                                text = "#${entry.tag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Пульс: ${entry.pulse}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatTimestamp(entry.timestamp_ms),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(text = "Быстрые действия", style = MaterialTheme.typography.titleLarge)
        }

        items(state.quickActions) { action ->
            Button(
                onClick = { onActionClick(action.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = action.label)
            }
        }

        item {
            Text(
                text = "Приложение носит информационный характер и не заменяет консультацию врача.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StatsRow(label: String, systolic: Int?, diastolic: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (systolic != null && diastolic != null) "$systolic/$diastolic" else "—",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getCategoryName(category: BpCategory?): String {
    return when (category) {
        is BpCategory.Low -> "Низкое"
        is BpCategory.Optimal -> "Оптимальное"
        is BpCategory.Normal -> "Норма"
        is BpCategory.Elevated -> "Повышенное"
        is BpCategory.High -> "Высокое"
        is BpCategory.VeryHigh -> "Очень высокое"
        is BpCategory.CriticallyHigh -> "Критически высокое"
        is BpCategory.Emergency -> "Срочно обратитесь к врачу"
        null -> ""
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ADDnevnikTheme {
        HomeScreenContent(
            state = HomeUiState(),
            latestPressure = null,
            bpStats = BpStats(null, null, null, null, null, null, null, null, null, 0),
            bpCategory = null,
            showAddDialog = false,
            scanResult = null,
            onActionClick = {},
            onDismissDialog = {},
            onConfirmPressure = { _, _, _, _ -> },
            onShowChart = {},
            onScanRequest = {},
            onShowReport = {}
        )
    }
}
