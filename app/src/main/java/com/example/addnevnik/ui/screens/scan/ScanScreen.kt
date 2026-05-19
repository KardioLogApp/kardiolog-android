package com.example.addnevnik.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.ScaleType
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.addnevnik.util.BpOcrAnalyzer
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onResult: (systolic: Int, diastolic: Int, pulse: Int?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var ocrResult by remember { mutableStateOf<ScanOcrResult?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var showManualForm by remember { mutableStateOf(false) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var overlayRectPx by remember { mutableStateOf<Rect?>(null) }

    // Refs для доступа из фонового потока без recompose
    val isScanningRef = remember { mutableStateOf(true) }
    val overlayRectRef = remember { mutableStateOf<Rect?>(null) }
    val previewSizeRef = remember { mutableStateOf(IntSize.Zero) }
    val analyzerRef = remember { mutableStateOf<BpOcrAnalyzer?>(null) }

    // Синхронизируем refs при изменении state
    LaunchedEffect(isScanning) { isScanningRef.value = isScanning }
    LaunchedEffect(overlayRectPx) {
        overlayRectRef.value = overlayRectPx
        analyzerRef.value = null // сбросить analyzer при изменении rect
    }
    LaunchedEffect(previewSize) { previewSizeRef.value = previewSize }

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

    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    LaunchedEffect(Unit) {
        try {
            if (!OpenCVLoader.initDebug()) {
                Log.e("ScanScreen", "OpenCV init failed")
                Toast.makeText(context, "Не удалось инициализировать OpenCV", Toast.LENGTH_LONG).show()
            }
        } catch (e: Throwable) {
            Log.e("ScanScreen", "OpenCV init exception", e)
        }
    }

    // PreviewView создаётся один раз и сохраняется
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    // Камера привязывается ОДИН РАЗ через LaunchedEffect
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    try {
                        val overlayRect = overlayRectRef.value
                        val pSize = previewSizeRef.value

                        if (!isScanningRef.value || overlayRect == null || pSize == IntSize.Zero) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val imageRect = mapPreviewRectToImageRect(
                            overlayRect = overlayRect,
                            previewSize = pSize,
                            imageWidth = imageProxy.width,
                            imageHeight = imageProxy.height
                        )

                        if (imageRect == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        // Создаём analyzer один раз
                        val analyzer = analyzerRef.value
                            ?: BpOcrAnalyzer(imageRect).also { analyzerRef.value = it }

                        val result = analyzer.analyze(imageProxy)

                        if (
                            result != null &&
                            isScanningRef.value &&
                            result.result.confidence >= 0.5f &&
                            result.result.sys != null &&
                            result.result.dia != null
                        ) {
                            val scanResult = ScanOcrResult(
                                systolic = result.result.sys,
                                diastolic = result.result.dia,
                                pulse = result.result.pulse
                            )
                            mainHandler.post {
                                ocrResult = scanResult
                                isScanning = false
                                isScanningRef.value = false
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e("ScanScreen", "Analyzer error", e)
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("ScanScreen", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // PreviewView — всегда в дереве, не пересоздаётся
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        previewSize = coordinates.size
                    }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Рамка сканирования
        if (isScanning && !showManualForm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        val w = coordinates.size.width.toFloat()
                        val h = coordinates.size.height.toFloat()
                        val rectWidth = w * 0.78f
                        val rectHeight = h * 0.28f
                        val left = (w - rectWidth) / 2f
                        val top = (h - rectHeight) / 2.8f
                        overlayRectPx = Rect(
                            left = left,
                            top = top,
                            right = left + rectWidth,
                            bottom = top + rectHeight
                        )
                    }
            ) {
                ScannerOverlay()
            }
        }

        // Кнопка закрыть
        if (!showManualForm) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White
                )
            }
        }

        // Кнопка "Ввести вручную"
        if (isScanning && !showManualForm) {
            Button(
                onClick = {
                    ocrResult = ScanOcrResult(null, null, null)
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
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Ввести вручную",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Форма подтверждения
        val result = ocrResult
        if ((!isScanning || showManualForm) && result != null) {
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
                    onConfirm = { sys, dia, pulse -> onResult(sys, dia, pulse) },
                    onRetry = {
                        ocrResult = null
                        showManualForm = false
                        isScanning = true
                        isScanningRef.value = true
                        analyzerRef.value = null
                    }
                )
            }
        }
    }
}

private fun mapPreviewRectToImageRect(
    overlayRect: Rect,
    previewSize: IntSize,
    imageWidth: Int,
    imageHeight: Int
): RectF? {
    if (previewSize.width <= 0 || previewSize.height <= 0 ||
        imageWidth <= 0 || imageHeight <= 0
    ) return null

    val previewW = previewSize.width.toFloat()
    val previewH = previewSize.height.toFloat()
    val imageW = imageWidth.toFloat()
    val imageH = imageHeight.toFloat()

    // FILL_CENTER: масштаб по максимуму (без letterbox)
    val scale = maxOf(previewW / imageW, previewH / imageH)
    val fittedWidth = imageW * scale
    val fittedHeight = imageH * scale
    val dx = (previewW - fittedWidth) / 2f
    val dy = (previewH - fittedHeight) / 2f

    val left = ((overlayRect.left - dx) / scale).coerceIn(0f, imageW)
    val top = ((overlayRect.top - dy) / scale).coerceIn(0f, imageH)
    val right = ((overlayRect.right - dx) / scale).coerceIn(0f, imageW)
    val bottom = ((overlayRect.bottom - dy) / scale).coerceIn(0f, imageH)

    if (right - left < 20f || bottom - top < 20f) return null

    return RectF(left, top, right, bottom)
}