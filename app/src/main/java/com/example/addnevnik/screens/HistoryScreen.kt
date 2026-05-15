package com.example.addnevnik.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.data.local.DailyNoteEntity
import com.example.addnevnik.domain.BpClassifier
import com.example.addnevnik.domain.BpStats
import com.example.addnevnik.util.MeasurementRow
import com.example.addnevnik.util.PatientInfo
import com.example.addnevnik.util.PdfReportGenerator
import com.example.addnevnik.viewmodel.HomeViewModel
import com.example.addnevnik.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HomeViewModel, settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val allPressure by viewModel.allPressure.collectAsStateWithLifecycle()
    val allDailyNotes by viewModel.allDailyNotes.collectAsStateWithLifecycle()
    val bpStats by viewModel.bpStats.collectAsStateWithLifecycle()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()

    var displayMode by remember { mutableStateOf("Бланк") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedDayRecord by remember { mutableStateOf<Pair<String, List<BloodPressureEntity>>?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet && selectedDayRecord != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            EditDayBottomSheetContent(
                date = selectedDayRecord!!.first,
                entries = selectedDayRecord!!.second,
                onDelete = { entry ->
                    viewModel.deletePressure(entry)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Запись удалена",
                            actionLabel = "Отменить",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                },
                onTogglePrimary = { viewModel.togglePrimary(it) },
                onManualSave = { s, d, p, isMorning ->
                    val cal = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
                    val date = dateFormat.parse(selectedDayRecord!!.first)
                    if (date != null) {
                        cal.time = date
                        cal.set(Calendar.HOUR_OF_DAY, if (isMorning) 8 else 20)
                        cal.set(Calendar.MINUTE, 0)
                        viewModel.savePressure(
                            s, d, p,
                            tag = null,
                            isPrimary = true,
                            isManual = true,
                            timestamp = cal.timeInMillis
                        )
                    }
                },
                onDone = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Дневник",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.weight(1f)
                ) {
                    SegmentedButton(
                        selected = displayMode == "Карточки",
                        onClick = { displayMode = "Карточки" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Карточки")
                    }

                    SegmentedButton(
                        selected = displayMode == "Бланк",
                        onClick = { displayMode = "Бланк" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Бланк")
                    }
                }

                Button(
                    onClick = {
                        if (allPressure.isNotEmpty()) {
                            val avgSys = allPressure.map { it.systolic }.average().toInt()
                            val avgDia = allPressure.map { it.diastolic }.average().toInt()
                            val avgPulse = allPressure.map { it.pulse }.average().toInt()

                            val patient = PatientInfo(
                                name = settings.profileName,
                                gender = settings.gender,
                                birthDate = settings.birthDate,
                                avgSystolic = avgSys,
                                avgDiastolic = avgDia,
                                avgHr = avgPulse
                            )

                            val calendar = Calendar.getInstance()
                            val dateKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

                            val rows = allPressure.groupBy {
                                calendar.timeInMillis = it.timestamp_ms
                                calendar.set(Calendar.HOUR_OF_DAY, 0)
                                calendar.set(Calendar.MINUTE, 0)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                                calendar.timeInMillis
                            }.map { (dateMs, entries) ->
                                val morningEntry = entries.filter {
                                    calendar.timeInMillis = it.timestamp_ms
                                    calendar.get(Calendar.HOUR_OF_DAY) in 6..10
                                }.let { list -> list.find { it.isPrimary } ?: list.firstOrNull() }

                                val eveningEntry = entries.filter {
                                    calendar.timeInMillis = it.timestamp_ms
                                    calendar.get(Calendar.HOUR_OF_DAY) in 18..23
                                }.let { list -> list.find { it.isPrimary } ?: list.firstOrNull() }

                                val allOtherCount = entries.count {
                                    calendar.timeInMillis = it.timestamp_ms
                                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                                    hour !in 6..10 && hour !in 18..23
                                }

                                val dailyNoteKey = dateKeyFmt.format(Date(dateMs))
                                val dailyNote = allDailyNotes.find { it.dateKey == dailyNoteKey }

                                MeasurementRow(
                                    date = dateMs,
                                    morningTime = morningEntry?.let { timeFmt.format(Date(it.timestamp_ms)) },
                                    eveningTime = eveningEntry?.let { timeFmt.format(Date(it.timestamp_ms)) },
                                    morningAd = morningEntry?.let { "${it.systolic}/${it.diastolic}" },
                                    morningHr = morningEntry?.pulse,
                                    eveningAd = eveningEntry?.let { "${it.systolic}/${it.diastolic}" },
                                    eveningHr = eveningEntry?.pulse,
                                    note = listOfNotNull(morningEntry?.tag, eveningEntry?.tag)
                                        .firstOrNull { it.isNotBlank() },
                                    medication = dailyNote?.medication,
                                    wellbeing = dailyNote?.wellbeing,
                                    outOfRangeCount = allOtherCount
                                )
                            }.sortedByDescending { it.date }

                            val file = PdfReportGenerator.generate(context, patient, rows)

                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                            context.startActivity(
                                Intent.createChooser(intent, "Поделиться дневником")
                            )
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("PDF")
                }
            }

            if (displayMode == "Бланк" && allPressure.isNotEmpty()) {
                StatsSection(bpStats)
            }

            if (allPressure.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Дневник пуст",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 17.sp
                    )
                }
            } else {
                if (displayMode == "Карточки") {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allPressure) { entry ->
                            HistoryItem(entry, allDailyNotes)
                        }
                    }
                } else {
                    MedicalDiaryView(allPressure, allDailyNotes) { date, entries ->
                        selectedDayRecord = date to entries
                        showBottomSheet = true
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    entry: BloodPressureEntity,
    allDailyNotes: List<DailyNoteEntity>
) {
    val dateFmt = remember { SimpleDateFormat("dd MMMM, HH:mm", Locale("ru")) }
    val category = remember(entry.systolic, entry.diastolic) {
        BpClassifier.classify(entry.systolic, entry.diastolic)
    }

    val dateKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val entryDateKey = dateKeyFmt.format(Date(entry.timestamp_ms))
    val dailyNote = allDailyNotes.find { it.dateKey == entryDateKey }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFmt.format(Date(entry.timestamp_ms)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (entry.isPrimary) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${entry.systolic}/${entry.diastolic}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = category.color
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "❤️ ${entry.pulse}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = category.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, category.color.copy(alpha = 0.45f))
            ) {
                Text(
                    text = category.shortLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = category.color,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!entry.tag.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            if (dailyNote != null && (dailyNote.medication.isNotBlank() || dailyNote.wellbeing.isNotBlank())) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                if (dailyNote.medication.isNotBlank()) {
                    Text(
                        text = "Доп. препарат: ${dailyNote.medication}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (dailyNote.wellbeing.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Самочувствие: ${dailyNote.wellbeing}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicalDiaryView(
    data: List<BloodPressureEntity>,
    allDailyNotes: List<DailyNoteEntity>,
    onEditDay: (String, List<BloodPressureEntity>) -> Unit
) {
    val groupedData = remember(data) { groupMeasurementsByDay(data) }
    val dateKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateParseFmt = remember { SimpleDateFormat("dd.MM.yyyy", Locale("ru")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(groupedData.toList()) { (date, record) ->
                val dateObj = dateParseFmt.parse(date)
                val dailyNote = if (dateObj != null) {
                    val key = dateKeyFmt.format(dateObj)
                    allDailyNotes.find { it.dateKey == key }
                } else null

                DiaryDayBlock(
                    date = date,
                    record = record,
                    dailyNote = dailyNote,
                    onEdit = { onEditDay(date, record.allEntries) }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DiaryDayBlock(
    date: String,
    record: DayRecord,
    dailyNote: DailyNoteEntity?,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RectangleShape
            )
            .clickable { onEdit() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.75.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "УТРО",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                MeasurementInfo(record.morningList)
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.75.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "ВЕЧЕР",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                MeasurementInfo(record.eveningList)
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.75.dp
            )

            Column(
                modifier = Modifier
                    .weight(1.45f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "ПРИМЕЧАНИЕ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                val note = record.allEntries.firstOrNull { it.tag?.isNotBlank() == true }?.tag
                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (dailyNote != null) {
                    if (dailyNote.medication.isNotBlank()) {
                        Text(
                            text = "Доп. препарат: ${dailyNote.medication}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    if (dailyNote.wellbeing.isNotBlank()) {
                        Text(
                            text = "Самочувствие: ${dailyNote.wellbeing}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementInfo(entries: List<BloodPressureEntity>) {
    if (entries.isEmpty()) {
        Text(
            text = "АД: —",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Пульс: —",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline
        )
        return
    }

    val primary = entries.find { it.isPrimary } ?: entries.first()
    val category = BpClassifier.classify(primary.systolic, primary.diastolic)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "АД: ${primary.systolic}/${primary.diastolic}",
            fontSize = 14.sp,
            color = category.color,
            fontWeight = FontWeight.Bold
        )
        if (primary.isPrimary) {
            Text(
                text = " ★",
                fontSize = 11.sp,
                color = androidx.compose.ui.graphics.Color(0xFFFFB300)
            )
        }
        if (primary.isManual) {
            Text(
                text = " ✎",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Text(
        text = category.shortLabel,
        fontSize = 12.sp,
        color = category.color,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
        text = "Пульс: ${primary.pulse}",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun EditDayBottomSheetContent(
    date: String,
    entries: List<BloodPressureEntity>,
    onDelete: (BloodPressureEntity) -> Unit,
    onTogglePrimary: (BloodPressureEntity) -> Unit,
    onManualSave: (Int, Int, Int, Boolean) -> Unit,
    onDone: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    var showManualForm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var entryToDelete by remember { mutableStateOf<BloodPressureEntity?>(null) }

    if (entryToDelete != null) {
        DeleteConfirmationDialog(
            entry = entryToDelete!!,
            onConfirm = {
                onDelete(entryToDelete!!)
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }

    var primaryId by remember(entries) {
        mutableStateOf(entries.find { it.isPrimary }?.id)
    }

    val calendar = Calendar.getInstance()
    val morningEntries = entries.filter {
        calendar.timeInMillis = it.timestamp_ms
        calendar.get(Calendar.HOUR_OF_DAY) in 6..10
    }.sortedByDescending { it.timestamp_ms }

    val eveningEntries = entries.filter {
        calendar.timeInMillis = it.timestamp_ms
        calendar.get(Calendar.HOUR_OF_DAY) in 18..23
    }.sortedByDescending { it.timestamp_ms }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Замеры за $date",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            item {
                if (!showManualForm) {
                    OutlinedButton(
                        onClick = {
                            showManualForm = true
                            scope.launch {
                                delay(200)
                                listState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Добавить вручную")
                    }
                } else {
                    ManualEntryForm(
                        onSave = { s, d, p, isMorning ->
                            onManualSave(s, d, p, isMorning)
                            showManualForm = false
                        },
                        onCancel = { showManualForm = false }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (morningEntries.isNotEmpty()) {
                item { SlotHeader("УТРО", "до 12:00") }
                items(morningEntries) { entry ->
                    EntryRow(
                        entry = entry,
                        isPrimaryOptimistic = entry.id == primaryId,
                        timeFormat = timeFormat,
                        onTogglePrimary = {
                            primaryId = if (primaryId == entry.id) null else entry.id
                            onTogglePrimary(entry)
                        },
                        onDelete = { entryToDelete = it }
                    )
                }
            }

            if (eveningEntries.isNotEmpty()) {
                item { SlotHeader("ВЕЧЕР", "с 12:00") }
                items(eveningEntries) { entry ->
                    EntryRow(
                        entry = entry,
                        isPrimaryOptimistic = entry.id == primaryId,
                        timeFormat = timeFormat,
                        onTogglePrimary = {
                            primaryId = if (primaryId == entry.id) null else entry.id
                            onTogglePrimary(entry)
                        },
                        onDelete = { entryToDelete = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Готово", fontSize = 16.sp)
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    entry: BloodPressureEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val category = remember(entry.systolic, entry.diastolic) {
        BpClassifier.classify(entry.systolic, entry.diastolic)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить замер?", fontSize = 20.sp) },
        text = {
            Column {
                Text(
                    "Вы действительно хотите удалить этот замер из истории?",
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeFormat.format(Date(entry.timestamp_ms)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${entry.systolic}/${entry.diastolic}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = category.color
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "❤️ ${entry.pulse}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = category.shortLabel,
                            color = category.color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить", fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontSize = 14.sp)
            }
        }
    )
}

@Composable
private fun SlotHeader(title: String, hint: String) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EntryRow(
    entry: BloodPressureEntity,
    isPrimaryOptimistic: Boolean,
    timeFormat: SimpleDateFormat,
    onTogglePrimary: (BloodPressureEntity) -> Unit,
    onDelete: (BloodPressureEntity) -> Unit,
    canBePrimary: Boolean = true
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val maxDrag = with(density) { 80.dp.toPx() }
    val scope = rememberCoroutineScope()
    val category = remember(entry.systolic, entry.diastolic) {
        BpClassifier.classify(entry.systolic, entry.diastolic)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                .padding(end = 24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить",
                tint = MaterialTheme.colorScheme.onError
            )
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newValue = (offsetX.value + delta).coerceIn(-maxDrag, 0f)
                            offsetX.snapTo(newValue)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (offsetX.value <= -maxDrag * 0.7f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete(entry)
                        }
                        scope.launch {
                            offsetX.animateTo(0f)
                        }
                    }
                )
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormat.format(Date(entry.timestamp_ms)),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )

                    if (entry.isManual) {
                        Text(
                            text = " (вручную)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${entry.systolic}/${entry.diastolic}",
                        color = category.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "❤️ ${entry.pulse}",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = category.shortLabel,
                    fontSize = 13.sp,
                    color = category.color,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                if (!entry.tag.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.tag,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            if (canBePrimary) {
                IconButton(
                    onClick = { onTogglePrimary(entry) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPrimaryOptimistic) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Основной",
                        tint = if (isPrimaryOptimistic) androidx.compose.ui.graphics.Color(0xFFFFB300)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            IconButton(
                onClick = { onDelete(entry) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    HorizontalDivider(
        thickness = 0.75.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun ManualEntryForm(
    onSave: (Int, Int, Int, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var sys by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var isMorning by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            "Новая запись вручную",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = sys,
                onValueChange = { sys = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Сист") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = dia,
                onValueChange = { dia = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Диаст") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = pulse,
                onValueChange = { pulse = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Пульс") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isMorning) {
                Button(
                    onClick = { isMorning = true },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Утро")
                }
            } else {
                OutlinedButton(
                    onClick = { isMorning = true },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Утро", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isMorning) {
                Button(
                    onClick = { isMorning = false },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Вечер")
                }
            } else {
                OutlinedButton(
                    onClick = { isMorning = false },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Вечер", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Отмена", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    val s = sys.toIntOrNull() ?: 120
                    val d = dia.toIntOrNull() ?: 80
                    val p = pulse.toIntOrNull() ?: 70
                    onSave(s, d, p, isMorning)
                },
                enabled = sys.isNotEmpty() && dia.isNotEmpty()
            ) {
                Text("Сохранить", fontSize = 14.sp)
            }
        }
    }
}

private class DayRecord {
    val morningList = mutableListOf<BloodPressureEntity>()
    val eveningList = mutableListOf<BloodPressureEntity>()
    val allEntries = mutableListOf<BloodPressureEntity>()
}

private fun groupMeasurementsByDay(data: List<BloodPressureEntity>): Map<String, DayRecord> {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val calendar = Calendar.getInstance()
    val grouped = mutableMapOf<String, DayRecord>()

    data.sortedByDescending { it.timestamp_ms }.forEach { entry ->
        val dateKey = dateFormat.format(Date(entry.timestamp_ms))
        val record = grouped.getOrPut(dateKey) { DayRecord() }
        record.allEntries.add(entry)

        calendar.timeInMillis = entry.timestamp_ms
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        if (hour in 6..10) {
            record.morningList.add(entry)
        } else if (hour in 18..23) {
            record.eveningList.add(entry)
        }
    }

    return grouped
}

@Composable
private fun StatsSection(stats: BpStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Статистика за период",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn("Мин. АД", "${stats.minSystolic ?: "--"}/${stats.minDiastolic ?: "--"}")
                StatColumn("Макс. АД", "${stats.maxSystolic ?: "--"}/${stats.maxDiastolic ?: "--"}")
                StatColumn("Среднее (30д)", "${stats.avgSystolic30 ?: "--"}/${stats.avgDiastolic30 ?: "--"}")
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
