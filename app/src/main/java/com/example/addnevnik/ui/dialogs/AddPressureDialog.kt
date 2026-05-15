package com.example.addnevnik.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addnevnik.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPressureDialog(
    viewModel: HomeViewModel,
    initialValues: String? = null,
    onScanRequest: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (systolic: Int, diastolic: Int, pulse: Int, tag: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var systolic by remember(initialValues) { mutableStateOf("") }
    var diastolic by remember(initialValues) { mutableStateOf("") }
    var pulse by remember(initialValues) { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var customTag by remember { mutableStateOf("") }

    var medication by remember { mutableStateOf("") }
    var wellbeing by remember { mutableStateOf("") }

    val tealPrimary = Color(0xFF00796B)
    val tags = listOf("Покой", "После еды", "После лекарства", "Стресс", "Физнагрузка")

    // Заполняем поля только если реально пришёл результат OCR.
    // Если initialValues == null или пустой — поля очищаются.
    LaunchedEffect(initialValues) {
        if (!initialValues.isNullOrBlank()) {
            val parts = initialValues.split(",")
            if (parts.size == 3) {
                systolic = parts[0].takeIf { it != "0" } ?: ""
                diastolic = parts[1].takeIf { it != "0" } ?: ""
                pulse = parts[2].takeIf { it != "0" } ?: ""
            } else {
                systolic = ""
                diastolic = ""
                pulse = ""
            }
        } else {
            systolic = ""
            diastolic = ""
            pulse = ""
        }
    }

    LaunchedEffect(Unit) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModel.getDailyNoteForDate(dateKey)?.let {
            medication = it.medication
            wellbeing = it.wellbeing
        }
    }

    val sysInt = systolic.toIntOrNull()
    val diaInt = diastolic.toIntOrNull()
    val plsInt = pulse.toIntOrNull()

    val sysWarn = sysInt != null && (sysInt < 60 || sysInt > 250)
    val diaWarn = diaInt != null && (diaInt < 40 || diaInt > 150)
    val plsWarn = plsInt != null && (plsInt < 30 || plsInt > 200)
    val hasWarning = sysWarn || diaWarn || plsWarn

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Новый замер",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                OutlinedButton(
                    onClick = onScanRequest,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tealPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = tealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Заполнить по фото", color = tealPrimary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InputField(
                    value = systolic,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) systolic = it },
                    label = "СИСТ",
                    placeholder = "120",
                    modifier = Modifier.weight(1f)
                )
                InputField(
                    value = diastolic,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) diastolic = it },
                    label = "ДИАС",
                    placeholder = "80",
                    modifier = Modifier.weight(1f)
                )
                InputField(
                    value = pulse,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) pulse = it },
                    label = "ПУЛЬС",
                    placeholder = "70",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (hasWarning) {
                val warnParts = buildList {
                    if (sysWarn) add("систола ($systolic)")
                    if (diaWarn) add("диастола ($diastolic)")
                    if (plsWarn) add("пульс ($pulse)")
                }
                val warnText =
                    "Проверьте значения: ${warnParts.joinToString(", ")} — вне физиологического диапазона"

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = warnText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7B4F00)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Состояние",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.take(3).forEach { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = selectedTag == tag,
                            onClick = { selectedTag = if (selectedTag == tag) null else tag },
                            tealPrimary = tealPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tags.drop(3).forEach { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = selectedTag == tag,
                            onClick = { selectedTag = if (selectedTag == tag) null else tag },
                            tealPrimary = tealPrimary
                        )
                    }

                    TagChip(
                        tag = "+ Своё",
                        isSelected = selectedTag == "своё",
                        onClick = { selectedTag = if (selectedTag == "своё") null else "своё" },
                        tealPrimary = tealPrimary
                    )
                }

                if (selectedTag == "своё") {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        label = { Text("Опишите состояние") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(16.dp),
                        colors = appOutlinedTextFieldColors(tealPrimary)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = medication,
                    onValueChange = { medication = it },
                    label = { Text("Доп. препарат") },
                    placeholder = { Text("экстренный приём") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = appOutlinedTextFieldColors(tealPrimary)
                )

                OutlinedTextField(
                    value = wellbeing,
                    onValueChange = { wellbeing = it },
                    label = { Text("Самочувствие") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.FavoriteBorder, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = appOutlinedTextFieldColors(tealPrimary)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Отмена", color = Color.Gray, fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        val sys = systolic.toIntOrNull()
                        val dia = diastolic.toIntOrNull()
                        val pls = pulse.toIntOrNull()

                        if (sys != null && dia != null && pls != null) {
                            val finalTag = if (selectedTag == "своё") customTag.trim() else selectedTag
                            onConfirm(sys, dia, pls, finalTag)

                            if (medication.isNotBlank() || wellbeing.isNotBlank()) {
                                val timestampMs = System.currentTimeMillis()
                                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Date(timestampMs))
                                viewModel.saveDailyNote(dateKey, medication, wellbeing)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tealPrimary),
                    enabled = systolic.isNotEmpty() && diastolic.isNotEmpty() && pulse.isNotEmpty()
                ) {
                    Text("Сохранить", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tealPrimary: Color
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(tag) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = tealPrimary.copy(alpha = 0.1f),
            selectedLabelColor = tealPrimary,
            labelColor = Color.Gray
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = Color.LightGray,
            selectedBorderColor = tealPrimary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}

@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val tealPrimary = Color(0xFF00796B)

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = appOutlinedTextFieldColors(tealPrimary)
        )
    }
}

@Composable
private fun appOutlinedTextFieldColors(tealPrimary: Color): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = tealPrimary,
        unfocusedBorderColor = Color.LightGray,
        cursorColor = tealPrimary,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Gray,
        focusedLabelColor = tealPrimary,
        unfocusedLabelColor = Color.Gray,
        focusedPlaceholderColor = Color.LightGray,
        unfocusedPlaceholderColor = Color.LightGray,
        focusedLeadingIconColor = tealPrimary,
        unfocusedLeadingIconColor = Color.Gray,
        errorTextColor = MaterialTheme.colorScheme.error,
        errorBorderColor = MaterialTheme.colorScheme.error
    )
}