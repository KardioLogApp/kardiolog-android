package com.example.addnevnik.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.addnevnik.util.BpOcrParser
import com.example.addnevnik.util.CameraOcrAnalyzer
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onResult: (systolic: Int, diastolic: Int, pulse: Int?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var ocrResult by remember { mutableStateOf<BpOcrParser.OcrResult?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    // Ручной ввод — открывается если пользователь нажал "Ввести вручную"
    var showManualForm by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Нет доступа к камере", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Камера ──────────────────────────────────────────────
        if (hasCameraPermission && !showManualForm) {
            val executor = remember { Executors.newSingleThreadExecutor() }
            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().also {
                                it.setAnalyzer(executor, CameraOcrAnalyzer { result ->
                                    if (isScanning) {
                                        isScanning = false
                                        ocrResult = result
                                    }
                                })
                            }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analyzer
                            )
                        } catch (e: Exception) {
                            Log.e("ScanScreen", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!showManualForm) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // ── Оверлей сканирования ─────────────────────────────────
        if (isScanning && !showManualForm) {
            ScannerOverlay()
        }

        // ── Верхняя панель: крестик ──────────────────────────────
        if (!showManualForm) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
            }
        }

        // ── Кнопка "Ввести вручную" — видна только во время сканирования ──
        if (isScanning && !showManualForm) {
            Button(
                onClick = {
                    // Открываем форму с пустыми полями
                    ocrResult = BpOcrParser.OcrResult(null, null, null)
                    showManualForm = true
                    isScanning = false
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Ввести вручную", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // ── Форма подтверждения после OCR ────────────────────────
        val result = ocrResult
        if ((!isScanning || showManualForm) && result != null) {
            // Затемнение фона
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                BpConfirmationForm(
                    initial = result,
                    onConfirm = { sys, dia, pulse ->
                        onResult(sys, dia, pulse)
                    },
                    onRetry = {
                        ocrResult = null
                        showManualForm = false
                        isScanning = true
                    }
                )
            }
        }
    }
}