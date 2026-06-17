package com.example.addnevnik.ui.screens.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Vertical rectangle matching tonometer display aspect ratio (~9:16 portrait)
            val rectW = size.width * 0.55f
            val rectH = size.height * 0.42f
            val rectLeft = (size.width - rectW) / 2f
            val rectTop = (size.height - rectH) / 2f - size.height * 0.04f

            drawRect(color = Color.Black.copy(alpha = 0.55f))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectW, rectH),
                cornerRadius = CornerRadius(12.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            val markerLen = 24.dp.toPx()
            val markerStroke = 3.dp.toPx()
            val markerColor = Color(0xFF01696F)
            val corners = listOf(
                Offset(rectLeft, rectTop) to Pair(1f, 1f),
                Offset(rectLeft + rectW, rectTop) to Pair(-1f, 1f),
                Offset(rectLeft, rectTop + rectH) to Pair(1f, -1f),
                Offset(rectLeft + rectW, rectTop + rectH) to Pair(-1f, -1f)
            )
            corners.forEach { (origin, dir) ->
                drawLine(markerColor, origin,
                    origin.copy(x = origin.x + dir.first * markerLen), markerStroke)
                drawLine(markerColor, origin,
                    origin.copy(y = origin.y + dir.second * markerLen), markerStroke)
            }
        }

        Text(
            text = "Наведите камеру на экран тонометра",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 320.dp)
        )
    }
}
