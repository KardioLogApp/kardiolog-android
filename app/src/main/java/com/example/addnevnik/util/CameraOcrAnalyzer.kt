package com.example.addnevnik.util

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class CameraOcrAnalyzer(
    private val onResult: (BpOcrParser.OcrResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val THROTTLE_MS = 800L
    private var lastProcessedTime = 0L

    // Stabilization: require same result N times in a row before reporting
    private val STABLE_FRAMES_REQUIRED = 3
    private var lastResult: BpOcrParser.OcrResult? = null
    private var stableCount = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastProcessedTime < THROTTLE_MS) {
            imageProxy.close()
            return
        }
        lastProcessedTime = now

        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Use textBlocks with bounding boxes sorted by vertical position (top → bottom)
                // This gives correct order: systolic (top) → diastolic (middle) → pulse (bottom)
                val result = BpOcrParser.parseBlocks(visionText.textBlocks)

                if (result.systolic != null || result.diastolic != null) {
                    if (result == lastResult) {
                        stableCount++
                        if (stableCount >= STABLE_FRAMES_REQUIRED) {
                            stableCount = 0
                            lastResult = null
                            onResult(result)
                        }
                    } else {
                        lastResult = result
                        stableCount = 1
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
