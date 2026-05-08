package com.example.addnevnik.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPressureDialog(
    initialValues: String? = null,
    onScanRequest: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (systolic: Int, diastolic: Int, pulse: Int, tag: String?) -> Unit
) {
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val tags = listOf("Покой", "После еды", "После лекарства", "Стресс", "Физнагрузка")

    LaunchedEffect(initialValues) {
        initialValues?.split(",")?.let {
            if (it.size == 3) {
                systolic = it[0]
                diastolic = it[1]
                pulse = it[2]
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Добавить замер", modifier = Modifier.weight(1f))
                IconButton(onClick = onScanRequest) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Scan")
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = systolic,
                    onValueChange = { if (it.all { char -> char.isDigit() }) systolic = it },
                    label = { Text("Систолическое (верхнее)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = diastolic,
                    onValueChange = { if (it.all { char -> char.isDigit() }) diastolic = it },
                    label = { Text("Диастолическое (нижнее)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pulse,
                    onValueChange = { if (it.all { char -> char.isDigit() }) pulse = it },
                    label = { Text("Пульс") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Тег (необязательно)", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = {
                                selectedTag = if (selectedTag == tag) null else tag
                            },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sys = systolic.toIntOrNull()
                    val dia = diastolic.toIntOrNull()
                    val pls = pulse.toIntOrNull()
                    if (sys != null && dia != null && pls != null) {
                        onConfirm(sys, dia, pls, selectedTag)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
