package com.example.addnevnik.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.addnevnik.data.local.BloodPressureEntity
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.legend.legendItem
import com.patrykandpatrick.vico.compose.legend.verticalLegend
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    data: List<BloodPressureEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("График давления") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет данных для отображения", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val sortedData = remember(data) { data.sortedBy { it.timestamp_ms } }
                val systolicEntries = sortedData.mapIndexed { index, entity ->
                    entryOf(index.toFloat(), entity.systolic.toFloat())
                }
                val diastolicEntries = sortedData.mapIndexed { index, entity ->
                    entryOf(index.toFloat(), entity.diastolic.toFloat())
                }

                val chartEntryModelProducer = remember(systolicEntries, diastolicEntries) {
                    ChartEntryModelProducer(listOf(systolicEntries, diastolicEntries))
                }

                ProvideChartStyle {
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                LineChart.LineSpec(
                                    lineColor = Color.Red.toArgb(),
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(Color.Red.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                ),
                                LineChart.LineSpec(
                                    lineColor = Color.Blue.toArgb(),
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(Color.Blue.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                            )
                        ),
                        chartModelProducer = chartEntryModelProducer,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = { value, _ -> (value.toInt() + 1).toString() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Порядковый номер замера",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
