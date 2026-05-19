package com.example.addnevnik.ui.screens.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ScanOcrResult(
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int?
)

@Composable
fun BpConfirmationForm(
    initial: ScanOcrResult,
    onConfirm: (Int, Int, Int?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var systolicText by remember(initial) {
        mutableStateOf(initial.systolic?.toString() ?: "")
    }
    var diastolicText by remember(initial) {
        mutableStateOf(initial.diastolic?.toString() ?: "")
    }
    var pulseText by remember(initial) {
        mutableStateOf(initial.pulse?.toString() ?: "")
    }

    val sysInt = systolicText.toIntOrNull()
    val diaInt = diastolicText.toIntOrNull()
    val suspiciousResult = sysInt != null && diaInt != null && sysInt <= diaInt

    Surface(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initial.systolic == null && initial.diastolic == null)
                        "Ввести вручную"
                    else
                        "Проверьте показания",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (initial.systolic != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Считано с экрана",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BpField(
                    label = "СИСТ",
                    unit = "мм рт.ст.",
                    value = systolicText,
                    onValueChange = { if (it.length <= 3) systolicText = it },
                    modifier = Modifier.weight(1f),
                    isError = sysInt != null && sysInt !in 60..250
                )
                BpField(
                    label = "ДИАСТ",
                    unit = "мм рт.ст.",
                    value = diastolicText,
                    onValueChange = { if (it.length <= 3) diastolicText = it },
                    modifier = Modifier.weight(1f),
                    isError = diaInt != null && diaInt !in 40..150
                )
                BpField(
                    label = "ПУЛЬС",
                    unit = "уд/мин",
                    value = pulseText,
                    onValueChange = { if (it.length <= 3) pulseText = it },
                    modifier = Modifier.weight(1f),
                    isError = false
                )
            }

            if (suspiciousResult) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Систола должна быть больше диастолы. Проверьте значения.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (initial.systolic == null) "Отмена" else "← К камере")
                }
                Button(
                    onClick = {
                        val sys = systolicText.toIntOrNull() ?: return@Button
                        val dia = diastolicText.toIntOrNull() ?: return@Button
                        val pulse = pulseText.toIntOrNull()
                        onConfirm(sys, dia, pulse)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = systolicText.toIntOrNull() != null
                            && diastolicText.toIntOrNull() != null
                            && !suspiciousResult
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@Composable
private fun BpField(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            isError = isError,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}