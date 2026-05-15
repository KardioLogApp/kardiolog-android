package com.example.addnevnik.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addnevnik.data.local.BloodPressureEntity
import com.patrykandpatrick.vico.compose.axis.axisLabelComponent
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        customRange = start to end
                        selectedPeriod = "Период"
                    }
                    showDateRangePicker = false
                }) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                title = {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Выберите период",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
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

    val filteredData = remember(data, selectedPeriod, customRange) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val startTime = when (selectedPeriod) {
            "Неделя" -> calendar.apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -7)
            }.timeInMillis

            "Месяц" -> calendar.apply {
                timeInMillis = now
                add(Calendar.MONTH, -1)
            }.timeInMillis

            "3 месяца" -> calendar.apply {
                timeInMillis = now
                add(Calendar.MONTH, -3)
            }.timeInMillis

            "Год" -> calendar.apply {
                timeInMillis = now
                add(Calendar.YEAR, -1)
            }.timeInMillis

            "Период" -> customRange?.first ?: (now - 30L * 24 * 60 * 60 * 1000L)
            else -> now - 30L * 24 * 60 * 60 * 1000L
        }

        val endTime = if (selectedPeriod == "Период") customRange?.second ?: now else now

        data.asSequence()
            .filter { it.timestamp_ms in startTime..endTime }
            .sortedBy { it.timestamp_ms }
            .toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Динамика давления",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            val labelText = if (period == "Период" && customRange != null) {
                                "${sdf.format(Date(customRange!!.first))}–${sdf.format(Date(customRange!!.second))}"
                            } else {
                                period
                            }
                            Text(
                                text = labelText,
                                fontSize = 13.sp,
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

            if (filteredData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Нет данных за этот период",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            } else {
                val dayMs = 24L * 60L * 60L * 1000L

                val firstDayStart = remember(filteredData) {
                    Calendar.getInstance().apply {
                        timeInMillis = filteredData.first().timestamp_ms
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }

                val diffDays = remember(filteredData) {
                    ((filteredData.last().timestamp_ms - filteredData.first().timestamp_ms) / dayMs).toInt()
                }

                val dateFormatter = remember(diffDays) {
                    when {
                        diffDays > 180 -> SimpleDateFormat("MM.yy", Locale("ru"))
                        else -> SimpleDateFormat("dd.MM", Locale("ru"))
                    }
                }

                val debugFormatter = remember {
                    SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
                }

                val systolicEntries = remember(filteredData, firstDayStart) {
                    filteredData.map { entity ->
                        val x = ((entity.timestamp_ms - firstDayStart) / dayMs).toFloat()
                        entryOf(x, entity.systolic.toFloat())
                    }
                }

                val diastolicEntries = remember(filteredData, firstDayStart) {
                    filteredData.map { entity ->
                        val x = ((entity.timestamp_ms - firstDayStart) / dayMs).toFloat()
                        entryOf(x, entity.diastolic.toFloat())
                    }
                }

                val chartEntryModelProducer = remember(
                    selectedPeriod,
                    customRange,
                    filteredData.size,
                    filteredData.firstOrNull()?.timestamp_ms,
                    filteredData.lastOrNull()?.timestamp_ms
                ) {
                    ChartEntryModelProducer(listOf(systolicEntries, diastolicEntries))
                }

                val xDateMap = remember(filteredData, firstDayStart) {
                    linkedMapOf<Float, Date>().apply {
                        filteredData.forEach { entity ->
                            val x = ((entity.timestamp_ms - firstDayStart) / dayMs).toFloat()
                            put(x, Date(firstDayStart + x.toLong() * dayMs))
                        }
                    }
                }

                val maxX = remember(systolicEntries, diastolicEntries) {
                    maxOf(
                        systolicEntries.maxOfOrNull { it.x } ?: 0f,
                        diastolicEntries.maxOfOrNull { it.x } ?: 0f,
                        1f
                    )
                }

                val minVal = remember(filteredData) {
                    minOf(
                        filteredData.minOf { it.diastolic },
                        filteredData.minOf { it.systolic }
                    ).toFloat()
                }

                val maxVal = remember(filteredData) {
                    maxOf(
                        filteredData.maxOf { it.diastolic },
                        filteredData.maxOf { it.systolic }
                    ).toFloat()
                }

                val horizontalSpacing = when {
                    diffDays <= 7 -> 1
                    diffDays <= 14 -> 2
                    diffDays <= 31 -> 5
                    diffDays <= 62 -> 7
                    diffDays <= 120 -> 14
                    else -> 30
                }

                val axisValuesOverrider = remember(maxX, minVal, maxVal) {
                    AxisValuesOverrider.fixed(
                        minX = -0.5f,
                        maxX = maxX + 0.5f,
                        minY = (minVal - 10f).coerceAtLeast(40f),
                        maxY = maxVal + 10f
                    )
                }

                Text(
                    text = "Точек: ${filteredData.size}   " +
                        "От: ${debugFormatter.format(Date(filteredData.first().timestamp_ms))}   " +
                        "До: ${debugFormatter.format(Date(filteredData.last().timestamp_ms))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                key(
                    selectedPeriod,
                    customRange,
                    filteredData.size,
                    filteredData.first().timestamp_ms,
                    filteredData.last().timestamp_ms
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
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
                                                    arrayOf(
                                                        softRed.copy(alpha = 0.10f),
                                                        Color.Transparent
                                                    )
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
                                                    arrayOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                                        Color.Transparent
                                                    )
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
                                        valueFormatter = { value, _ -> value.roundToInt().toString() },
                                        label = axisLabelComponent(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textSize = 10.sp
                                        )
                                    ),
                                    bottomAxis = rememberBottomAxis(
                                        valueFormatter = { value, _ ->
                                            val roundedX = value.roundToInt().toFloat()
                                            xDateMap[roundedX]?.let { dateFormatter.format(it) } ?: ""
                                        },
                                        labelRotationDegrees = if (diffDays > 20) -45f else 0f,
                                        tickLength = 0.dp,
                                        itemPlacer = AxisItemPlacer.Horizontal.default(
                                            spacing = horizontalSpacing,
                                            addExtremeLabelPadding = true
                                        ),
                                        label = axisLabelComponent(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textSize = if (diffDays > 31) 8.sp else 9.sp
                                        )
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalLayout = HorizontalLayout.FullWidth(),
                                    chartScrollSpec = rememberChartScrollSpec(
                                        isScrollEnabled = filteredData.size > 10,
                                        initialScroll = InitialScroll.End
                                    )
                                )
                            }
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
                color = MaterialTheme.colorScheme.onSurface
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

                val isHigh = filteredData.any { it.systolic >= 135 || it.diastolic >= 85 }
                StatusCard(isHigh)
            }
        }
    }
}

@Composable
private fun AverageCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
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
    val backgroundColor = if (isHigh) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    }
    val textColor = if (isHigh) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

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