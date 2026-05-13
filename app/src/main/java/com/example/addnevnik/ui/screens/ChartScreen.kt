package com.example.addnevnik.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addnevnik.data.local.BloodPressureEntity
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.component.shape.Shapes

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    data: List<BloodPressureEntity>,
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("Месяц") }
    val periods = listOf("Неделя", "Месяц", "3 месяца", "Год", "Период")
    val softRed = Color(0xFFB91C1C)
    
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    var customRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    if (dateRangePickerState.selectedStartDateMillis != null && 
                        dateRangePickerState.selectedEndDateMillis != null) {
                        customRange = dateRangePickerState.selectedStartDateMillis!! to dateRangePickerState.selectedEndDateMillis!!
                        selectedPeriod = "Период"
                    }
                    showDateRangePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Отмена") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.fillMaxWidth().height(500.dp),
                title = { 
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text("Выберите период", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                headline = {
                    val sdf = SimpleDateFormat("dd.MM", Locale("ru"))
                    val start = dateRangePickerState.selectedStartDateMillis?.let { sdf.format(Date(it)) } ?: "..."
                    val end = dateRangePickerState.selectedEndDateMillis?.let { sdf.format(Date(it)) } ?: "..."
                    Text(
                        text = "$start — $end",
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                showModeToggle = false
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Динамика давления", fontWeight = FontWeight.Bold, fontSize = 20.sp) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            val rangeText = if (selectedPeriod == "Период" && customRange != null) {
                val sdf = SimpleDateFormat("dd.MM", Locale("ru"))
                "Период: ${sdf.format(Date(customRange!!.first))} — ${sdf.format(Date(customRange!!.second))}"
            } else {
                "Следите за изменениями за ${selectedPeriod.lowercase()}"
            }
            Text(
                text = rangeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                periods.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { 
                            if (period == "Период") {
                                showDateRangePicker = true
                            } else {
                                selectedPeriod = period 
                            }
                        },
                        label = { 
                            val sdf = SimpleDateFormat("dd.MM", Locale("ru"))
                            val labelText = if (period == "Период") {
                                if (customRange != null) {
                                    "${sdf.format(Date(customRange!!.first))}-${sdf.format(Date(customRange!!.second))}"
                                } else {
                                    "Период"
                                }
                            } else {
                                period
                            }
                            Text(
                                text = labelText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredData = remember(data, selectedPeriod, customRange) {
                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                val startTime = when (selectedPeriod) {
                    "Неделя" -> {
                        calendar.timeInMillis = now
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        calendar.timeInMillis
                    }
                    "Месяц" -> {
                        calendar.timeInMillis = now
                        calendar.add(Calendar.MONTH, -1)
                        calendar.timeInMillis
                    }
                    "3 месяца" -> {
                        calendar.timeInMillis = now
                        calendar.add(Calendar.MONTH, -3)
                        calendar.timeInMillis
                    }
                    "Год" -> {
                        calendar.timeInMillis = now
                        calendar.add(Calendar.YEAR, -1)
                        calendar.timeInMillis
                    }
                    "Период" -> customRange?.first ?: (now - 30 * 24 * 3600000L)
                    else -> now - 30 * 24 * 3600000L
                }
                val endTime = if (selectedPeriod == "Период") customRange?.second ?: now else now
                
                val filtered = data.filter { it.timestamp_ms in startTime..endTime }
                    .sortedBy { it.timestamp_ms }
                
                filtered
            }

            if (filteredData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет данных за этот период", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            } else {
                val sdfX = SimpleDateFormat("dd.MM", Locale("ru"))
                
                val baseTime = remember(filteredData) { filteredData.firstOrNull()?.timestamp_ms ?: 0L }
                
                // Временный лог для отладки точности координат
                filteredData.forEach { entity ->
                    val computedX = (entity.timestamp_ms - baseTime).toFloat()
                    val formattedDate = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entity.timestamp_ms))
                    android.util.Log.d("CHART_DEBUG", "Original: ${entity.timestamp_ms}, Date: $formattedDate, ComputedX: $computedX, Sys: ${entity.systolic}, Dia: ${entity.diastolic}")
                }

                val systolicEntries = filteredData.map { entity ->
                    entryOf((entity.timestamp_ms - baseTime).toFloat(), entity.systolic.toFloat())
                }
                val diastolicEntries = filteredData.map { entity ->
                    entryOf((entity.timestamp_ms - baseTime).toFloat(), entity.diastolic.toFloat())
                }

                val chartEntryModelProducer = remember(systolicEntries, diastolicEntries) {
                    ChartEntryModelProducer(listOf(systolicEntries, diastolicEntries))
                }

                val axisValuesOverrider = remember(filteredData) {
                    if (filteredData.isNotEmpty()) {
                        val minSys = filteredData.minOf { it.systolic }
                        val maxSys = filteredData.maxOf { it.systolic }
                        val minDia = filteredData.minOf { it.diastolic }
                        val maxDia = filteredData.maxOf { it.diastolic }
                        
                        val absoluteMin = minOf(minSys, minDia).toFloat()
                        val absoluteMax = maxOf(maxSys, maxDia).toFloat()
                        
                        // Добавляем отступы сверху и снизу, чтобы линия не прилипала к краям
                        AxisValuesOverrider.fixed(
                            minY = (absoluteMin - 10f).coerceAtLeast(0f),
                            maxY = absoluteMax + 10f
                        )
                    } else {
                        AxisValuesOverrider.fixed(minY = 0f, maxY = 200f)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ProvideChartStyle {
                            Chart(
                                chart = lineChart(
                                    lines = listOf(
                                        LineChart.LineSpec(
                                            lineColor = softRed.toArgb(),
                                            lineBackgroundShader = verticalGradient(
                                                arrayOf(softRed.copy(alpha = 0.1f), Color.Transparent)
                                            ),
                                            point = shapeComponent(
                                                shape = Shapes.pillShape,
                                                color = softRed
                                            ),
                                            pointSizeDp = 4f
                                        ),
                                        LineChart.LineSpec(
                                            lineColor = MaterialTheme.colorScheme.primary.toArgb(),
                                            lineBackgroundShader = verticalGradient(
                                                arrayOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), Color.Transparent)
                                            ),
                                            point = shapeComponent(
                                                shape = Shapes.pillShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            pointSizeDp = 4f
                                        )
                                    ),
                                    axisValuesOverrider = axisValuesOverrider
                                ),
                                chartModelProducer = chartEntryModelProducer,
                                startAxis = rememberStartAxis(
                                    itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 6),
                                    valueFormatter = { value, _ -> value.toInt().toString() }
                                ),
                                bottomAxis = rememberBottomAxis(
                                    valueFormatter = { value, _ ->
                                        val sdf = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(baseTime + value.toLong()))
                                    },
                                    labelRotationDegrees = -45f,
                                    tickLength = 0.dp,
                                    itemPlacer = remember { AxisItemPlacer.Horizontal.default(spacing = 3) }
                                ),
                                modifier = Modifier.fillMaxSize(),
                                horizontalLayout = HorizontalLayout.FullWidth(),
                                chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = true)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Средние значения за период",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )

            if (filteredData.isNotEmpty()) {
                val avgSys = filteredData.map { it.systolic }.average().toInt()
                val avgDia = filteredData.map { it.diastolic }.average().toInt()
                val avgPulse = filteredData.map { it.pulse }.average().toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AverageCard("СИСТ", "$avgSys", softRed, Modifier.weight(1f))
                    AverageCard("ДИАС", "$avgDia", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    AverageCard("ПУЛЬС", "$avgPulse", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                val isHigh = filteredData.any { it.systolic > 140 || it.diastolic > 90 }
                StatusCard(isHigh)
            }
        }
    }
}

@Composable
private fun AverageCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun StatusCard(isHigh: Boolean) {
    val backgroundColor = if (isHigh) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    val textColor = if (isHigh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(textColor, RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isHigh) "Есть повышенные значения" else "Показатели стабильны",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 16.sp
            )
        }
    }
}
