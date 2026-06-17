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
import com.example.addnevnik.util.BpOcrParser
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG_SCAN = "BP_SCAN"

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
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var ocrResult by remember { mutableStateOf<ScanOcrResult?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var showManualForm by remember { mutableStateOf(false) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var overlayRectPx by remember { mutableStateOf<Rect?>(null) }

    val isScanningRef = remember { mutableStateOf(true) }
    val overlayRectRef = remember { mutableStateOf<Rect?>(null) }
    val previewSizeRef = remember { mutableStateOf(IntSize.Zero) }
    val analyzerRef = remember { mutableStateOf<BpOcrParser?>(null) }

    LaunchedEffect(isScanning) {
        isScanningRef.value = isScanning
        Log.d(TAG_SCAN, "isScanning=$isScanning")
    }

    LaunchedEffect(overlayRectPx) {
        overlayRectRef.value = overlayRectPx
        analyzerRef.value = null
        Log.d(TAG_SCAN, "overlay updated=$overlayRectPx, analyzer reset")
    }

    LaunchedEffect(previewSize) {
        previewSizeRef.value = previewSize
        if (previewSize != IntSize.Zero) {
            Log.d(TAG_SCAN, "previewSize=${previewSize.width}x${previewSize.height}")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        Log.d(TAG_SCAN, "camera permission granted=$granted")
        if (!granted) {
            Toast.makeText(context, "Нет доступа к камере", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            Log.d(TAG_SCAN, "requesting camera permission")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            Log.d(TAG_SCAN, "camera permission already granted")
        }
    }

    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG_SCAN, "executor shutdown")
            executor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        try {
            if (!OpenCVLoader.initDebug()) {
                Log.e(TAG_SCAN, "OpenCV init failed")
                Toast.makeText(context, "Не удалось инициализировать OpenCV", Toast.LENGTH_LONG).show()
            } else {
                Log.d(TAG_SCAN, "OpenCV init success")
            }
        } catch (e: Throwable) {
            Log.e(TAG_SCAN, "OpenCV init exception", e)
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        Log.d(TAG_SCAN, "binding camera use cases")

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
                            Log.d(
                                TAG_SCAN,
                                "imageRect=null frame=${imageProxy.width}x${imageProxy.height} " +
                                    "preview=${pSize.width}x${pSize.height} overlay=$overlayRect"
                            )
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        Log.d(
                            TAG_SCAN,
                            "frame=${imageProxy.width}x${imageProxy.height}, " +
                                "preview=${pSize.width}x${pSize.height}, " +
                                "overlay=[l=${overlayRect.left}, t=${overlayRect.top}, " +
                                "r=${overlayRect.right}, b=${overlayRect.bottom}], " +
                                "imageRect=[l=${imageRect.left}, t=${imageRect.top}, " +
                                "r=${imageRect.right}, b=${imageRect.bottom}]"
                        )

                        Log.d(
                            TAG_SCAN,
                            "analyzerRef before=${analyzerRef.value?.javaClass?.name}"
                        )

                        val analyzer = analyzerRef.value
                            ?: BpOcrParser().also {
                                analyzerRef.value = it
                                Log.d(
                                    TAG_SCAN,
                                    "BpOcrParser created, class=${it.javaClass.name}"
                                )
                            }

                        val result = analyzer.analyze(imageProxy, imageRect)

                        if (result == null) {
                            Log.d(TAG_SCAN, "analyze -> null")
                        } else {
                            Log.d(
                                TAG_SCAN,
                                "analyze -> sys=${result.result.sys}, " +
                                    "dia=${result.result.dia}, " +
                                    "pulse=${result.result.pulse}, " +
                                    "confidence=${result.result.confidence}, " +
                                    "rows=${result.rows.size}"
                            )
                        }

                        val accepted =
                            result != null &&
                                isScanningRef.value &&
                                result.result.confidence >= 0.5f &&
                                result.result.sys != null &&
                                result.result.dia != null

                        if (accepted) {
                            val scanResult = ScanOcrResult(
                                systolic = result!!.result.sys,
                                diastolic = result.result.dia,
                                pulse = result.result.pulse
                            )
                            Log.d(TAG_SCAN, "result accepted -> $scanResult")

                            mainHandler.post {
                                ocrResult = scanResult
                                isScanning = false
                                isScanningRef.value = false
                                Log.d(TAG_SCAN, "scan stopped, showing confirmation form")
                            }
                        } else if (result != null) {
                            Log.d(TAG_SCAN, "result rejected by gate")
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG_SCAN, "Analyzer error", e)
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
                Log.d(TAG_SCAN, "camera bound successfully")
            } catch (e: Exception) {
                Log.e(TAG_SCAN, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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
                        val newRect = Rect(
                            left = left,
                            top = top,
                            right = left + rectWidth,
                            bottom = top + rectHeight
                        )
                        if (overlayRectPx != newRect) {
                            overlayRectPx = newRect
                        }
                    }
            ) {
                ScannerOverlay()
            }
        }

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

        if (isScanning && !showManualForm) {
            Button(
                onClick = {
                    Log.d(TAG_SCAN, "manual entry requested")
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
                    onConfirm = { sys, dia, pulse ->
                        Log.d(TAG_SCAN, "confirm result sys=$sys dia=$dia pulse=$pulse")
                        onResult(sys, dia, pulse)
                    },
                    onRetry = {
                        Log.d(TAG_SCAN, "retry scan, analyzer reset")
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