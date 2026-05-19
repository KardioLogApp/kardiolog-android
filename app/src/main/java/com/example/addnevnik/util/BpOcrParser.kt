package com.example.addnevnik.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class BpResult(
    val sys: Int?,
    val dia: Int?,
    val pulse: Int?,
    val confidence: Float
)

data class RowDebugInfo(
    val rect: RectF,
    val value: Int?,
    val rowConfidence: Float
)

data class BpOcrResult(
    val result: BpResult,
    val rows: List<RowDebugInfo>
)

class BpOcrAnalyzer(
    private val guideRect: RectF
) {
    private var frameCount = 0
    private val history = ArrayDeque<BpResult>(5)
    private val clahe = Imgproc.createCLAHE(2.5, Size(8.0, 8.0))

    private val segmentMap: Map<List<Boolean>, Char> = mapOf(
        listOf(true, true, true, true, true, true, false) to '0',
        listOf(false, true, true, false, false, false, false) to '1',
        listOf(true, true, false, true, true, false, true) to '2',
        listOf(true, true, true, true, false, false, true) to '3',
        listOf(false, true, true, false, false, true, true) to '4',
        listOf(true, false, true, true, false, true, true) to '5',
        listOf(true, false, true, true, true, true, true) to '6',
        listOf(true, true, true, false, false, false, false) to '7',
        listOf(true, true, true, true, true, true, true) to '8',
        listOf(true, true, true, true, false, true, true) to '9'
    )

    fun analyze(image: ImageProxy): BpOcrResult? {
        frameCount++
        if (frameCount % 15 != 0) return null

        val mat = imageToBgrMat(image)
        return try {
            val ocrResult = runOcr(mat)
            history.addLast(ocrResult.result)
            if (history.size > 5) history.removeFirst()
            ocrResult
        } finally {
            mat.release()
        }
    }

    fun drawDebugOverlay(canvas: Canvas, result: BpOcrResult) {
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val textPaint = Paint().apply {
            style = Paint.Style.FILL
            textSize = 36f
            isAntiAlias = true
        }

        val labels = listOf("SYS", "DIA", "PLS")
        val rawValues = listOf(result.result.sys, result.result.dia, result.result.pulse)

        result.rows.forEachIndexed { index, row ->
            val color = when {
                row.rowConfidence > 0.8f -> Color.GREEN
                row.rowConfidence > 0.5f -> Color.YELLOW
                else -> Color.RED
            }
            strokePaint.color = color
            textPaint.color = color

            canvas.drawRect(row.rect, strokePaint)

            val label = labels.getOrElse(index) { "?" }
            val value = rawValues.getOrNull(index)?.toString() ?: "?"
            canvas.drawText("$label: $value", row.rect.left, row.rect.top - 8f, textPaint)
        }
    }

    private fun imageToBgrMat(image: ImageProxy): Mat {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val uvHeight = height / 2
        val uvWidth = width / 2
        val nv21 = ByteArray(width * height * 3 / 2)

        val yRow = ByteArray(width)
        for (row in 0 until height) {
            yBuf.position(row * yRowStride)
            yBuf.get(yRow, 0, width)
            System.arraycopy(yRow, 0, nv21, row * width, width)
        }

        val uvBase = width * height
        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val uvIndex = row * uvRowStride + col * uvPixelStride
                vBuf.position(uvIndex)
                nv21[uvBase + row * width + col * 2] = vBuf.get()
                uBuf.position(uvIndex)
                nv21[uvBase + row * width + col * 2 + 1] = uBuf.get()
            }
        }

        val yuv = Mat(height + uvHeight, width, CvType.CV_8UC1)
        yuv.put(0, 0, nv21)
        val bgr = Mat()
        Imgproc.cvtColor(yuv, bgr, Imgproc.COLOR_YUV2BGR_NV21)
        yuv.release()
        return bgr
    }

    private fun preprocessCrop(bgr: Mat): Mat {
        val gw = guideRect.width()
        val gh = guideRect.height()
        val pad = (min(gw, gh) / 30f).toInt()

        val x = max(0, guideRect.left.toInt() + pad)
        val y = max(0, guideRect.top.toInt() + pad)
        val x2 = min(bgr.cols(), guideRect.right.toInt() - pad)
        val y2 = min(bgr.rows(), guideRect.bottom.toInt() - pad)

        if (x2 <= x || y2 <= y) return Mat()

        val cropped = Mat(bgr, org.opencv.core.Rect(x, y, x2 - x, y2 - y))

        val gray = Mat()
        Imgproc.cvtColor(cropped, gray, Imgproc.COLOR_BGR2GRAY)
        cropped.release()

        val enhanced = Mat()
        clahe.apply(gray, enhanced)
        gray.release()

        val mean = Core.mean(enhanced).`val`[0]
        val threshType =
            if (mean < 128) Imgproc.THRESH_BINARY else Imgproc.THRESH_BINARY_INV

        val binary = Mat()
        Imgproc.threshold(
            enhanced,
            binary,
            0.0,
            255.0,
            threshType or Imgproc.THRESH_OTSU
        )
        enhanced.release()

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 1.0))
        val closed = Mat()
        Imgproc.morphologyEx(binary, closed, Imgproc.MORPH_CLOSE, kernel)
        binary.release()
        kernel.release()

        return closed
    }

    private fun cropDigitZone(binary: Mat): Mat {
        if (binary.empty()) return Mat()

        val sw = binary.cols()
        val x0 = (0.25 * sw).toInt()
        var x1 = (0.88 * sw).toInt()

        val rightStart = sw / 2
        for (col in rightStart until sw) {
            val colMat = binary.col(col)
            try {
                val coverage = Core.countNonZero(colMat).toFloat() / binary.rows()
                if (coverage > 0.3f) x1 = min(x1, col)
            } finally {
                colMat.release()
            }
        }

        x1 = x1.coerceAtLeast((sw * 0.60).toInt())
        if (x1 <= x0) return Mat()

        return Mat(binary, org.opencv.core.Rect(x0, 0, x1 - x0, binary.rows()))
    }

    private fun findRowBands(binary: Mat): List<IntRange> {
        if (binary.empty()) return emptyList()

        val h = binary.rows()
        val hProjMat = Mat()
        Core.reduce(binary, hProjMat, 1, Core.REDUCE_SUM, CvType.CV_32F)
        val hProj = FloatArray(h) { row -> hProjMat.get(row, 0)[0].toFloat() }
        hProjMat.release()

        val sigma = max(2f, h * 0.012f)
        val smoothed = gaussianSmooth(hProj, sigma)

        val maxVal = smoothed.maxOrNull() ?: 0f
        val inverted = FloatArray(h) { maxVal - smoothed[it] }

        val valleys = findPeaks(inverted, minDistance = max(1, h / 5))

        val boundaries = mutableListOf(0)
        boundaries.addAll(valleys)
        boundaries.add(h)

        val segments = mutableListOf<Pair<IntRange, Int>>()
        for (i in 0 until boundaries.size - 1) {
            val start = boundaries[i]
            val end = boundaries[i + 1]
            val size = end - start
            if (size > h * 0.05f) {
                segments.add((start until end) to size)
            }
        }

        return segments
            .sortedByDescending { it.second }
            .take(3)
            .sortedBy { it.first.first }
            .map { it.first }
    }

    private fun findDigitRects(rowBinary: Mat): List<IntRange> {
        if (rowBinary.empty()) return emptyList()

        val w = rowBinary.cols()
        val rowH = rowBinary.rows()

        val vProjMat = Mat()
        Core.reduce(rowBinary, vProjMat, 0, Core.REDUCE_SUM, CvType.CV_32F)
        val vProj = IntArray(w) { col -> vProjMat.get(0, col)[0].toInt() }
        vProjMat.release()

        val groups = mutableListOf<IntRange>()
        var start = -1
        for (col in 0..w) {
            val active = col < w && vProj[col] > 0
            if (active && start == -1) {
                start = col
            } else if (!active && start != -1) {
                val width = col - start
                if (width >= 3 && width <= rowH * 1.2f) {
                    groups.add(start until col)
                }
                start = -1
            }
        }

        return groups
    }

    private data class DigitResult(val char: Char?, val confidence: Float)

    private fun recognizeDigit(roi: Mat): DigitResult {
        val trimmed = trimEmptyRows(roi)

        if (trimmed.empty() || trimmed.rows() < 4 || trimmed.cols() < 2) {
            trimmed.release()
            return DigitResult(null, 0f)
        }

        val aspectRatio = trimmed.rows().toFloat() / trimmed.cols().toFloat()
        if (aspectRatio > 3.5f) {
            trimmed.release()
            return DigitResult('1', 0.9f)
        }

        val binary = Mat()
        Imgproc.threshold(
            trimmed,
            binary,
            0.0,
            255.0,
            Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU
        )
        trimmed.release()

        val h = binary.rows().toFloat()
        val w = binary.cols().toFloat()

        val zones = arrayOf(
            floatArrayOf(0.00f, 0.18f, 0.10f, 0.90f),
            floatArrayOf(0.05f, 0.50f, 0.65f, 1.00f),
            floatArrayOf(0.50f, 0.95f, 0.65f, 1.00f),
            floatArrayOf(0.82f, 1.00f, 0.10f, 0.90f),
            floatArrayOf(0.50f, 0.95f, 0.00f, 0.35f),
            floatArrayOf(0.05f, 0.50f, 0.00f, 0.35f),
            floatArrayOf(0.40f, 0.60f, 0.12f, 0.88f)
        )

        val densities = FloatArray(7) { i ->
            val z = zones[i]
            val ry0 = (z[0] * h).toInt().coerceIn(0, binary.rows() - 1)
            val ry1 = (z[1] * h).toInt().coerceIn(ry0 + 1, binary.rows())
            val rx0 = (z[2] * w).toInt().coerceIn(0, binary.cols() - 1)
            val rx1 = (z[3] * w).toInt().coerceIn(rx0 + 1, binary.cols())

            val zone = Mat(binary, org.opencv.core.Rect(rx0, ry0, rx1 - rx0, ry1 - ry0))
            val nonZero = Core.countNonZero(zone).toFloat()
            val area = ((ry1 - ry0) * (rx1 - rx0)).toFloat()
            zone.release()
            if (area > 0f) nonZero / area else 0f
        }

        binary.release()

        val threshold = kmeansThreshold(densities)
        val segs = densities.map { it > threshold }
        val digit = segmentMap[segs]

        val onDensities = densities.filterIndexed { i, _ -> segs[i] }
        val offDensities = densities.filterIndexed { i, _ -> !segs[i] }
        val meanOn = if (onDensities.isNotEmpty()) onDensities.average().toFloat() else 0f
        val meanOff = if (offDensities.isNotEmpty()) offDensities.average().toFloat() else 0f
        val conf = ((meanOn - meanOff) / max(threshold, 0.1f)).coerceIn(0f, 1f)

        return DigitResult(digit, conf)
    }

    private fun runOcr(bgr: Mat): BpOcrResult {
        val binary = preprocessCrop(bgr)
        if (binary.empty()) {
            return BpOcrResult(BpResult(null, null, null, 0f), emptyList())
        }

        val digitZone = cropDigitZone(binary)
        binary.release()

        if (digitZone.empty()) {
            return BpOcrResult(BpResult(null, null, null, 0f), emptyList())
        }

        val rowBands = findRowBands(digitZone)
        val debugRows = mutableListOf<RowDebugInfo>()
        val values = mutableListOf<Int?>()

        val scaleX =
            (guideRect.right - guideRect.left) * 0.63f / digitZone.cols().coerceAtLeast(1)
        val scaleY =
            (guideRect.bottom - guideRect.top) / digitZone.rows().coerceAtLeast(1)
        val rxBase = guideRect.left + 0.25f * (guideRect.right - guideRect.left)

        try {
            for (band in rowBands) {
                val bandH = (band.last - band.first + 1)
                    .coerceAtMost(digitZone.rows() - band.first)

                if (bandH <= 0) continue

                val rowMat = Mat(
                    digitZone,
                    org.opencv.core.Rect(0, band.first, digitZone.cols(), bandH)
                )

                val sb = StringBuilder()
                var rowConf = 0f

                try {
                    val digitRanges = findDigitRects(rowMat)
                    rowConf = if (digitRanges.isEmpty()) 0f else 1f

                    for (range in digitRanges) {
                        val roiW = range.last - range.first + 1
                        if (roiW <= 0) continue

                        val roiMat = Mat(
                            rowMat,
                            org.opencv.core.Rect(range.first, 0, roiW, rowMat.rows())
                        )
                        val dr = recognizeDigit(roiMat)
                        roiMat.release()

                        if (dr.char != null) sb.append(dr.char)
                        rowConf = min(rowConf, dr.confidence)
                    }
                } finally {
                    rowMat.release()
                }

                val num = sb.toString().toIntOrNull()
                values.add(num)

                val ry = guideRect.top + band.first * scaleY
                debugRows.add(
                    RowDebugInfo(
                        rect = RectF(
                            rxBase,
                            ry,
                            rxBase + digitZone.cols() * scaleX,
                            ry + bandH * scaleY
                        ),
                        value = num,
                        rowConfidence = rowConf
                    )
                )
            }
        } finally {
            digitZone.release()
        }

        var sys = values.getOrNull(0)
        var dia = values.getOrNull(1)
        var pulse = values.getOrNull(2)

        if (sys != null && sys !in 60..260) sys = null
        if (dia != null && dia !in 40..160) dia = null
        if (pulse != null && pulse !in 30..220) pulse = null
        if (sys != null && dia != null && sys <= dia) {
            sys = null
            dia = null
        }

        val currentResult = BpResult(
            sys = sys,
            dia = dia,
            pulse = pulse,
            confidence = listOf(sys, dia, pulse).count { it != null } / 3f
        )

        val majority = majorityVote(history.toList() + currentResult)
        return BpOcrResult(majority ?: currentResult, debugRows)
    }

    private fun majorityVote(results: List<BpResult>): BpResult? {
        if (results.size < 3) return null

        fun <T> plurality(values: List<T?>): T? =
            values.filterNotNull()
                .groupingBy { it }
                .eachCount()
                .filter { it.value >= 3 }
                .maxByOrNull { it.value }
                ?.key

        val sys = plurality(results.map { it.sys })
        val dia = plurality(results.map { it.dia })
        val pulse = plurality(results.map { it.pulse })

        if (sys == null && dia == null && pulse == null) return null

        return BpResult(
            sys = sys,
            dia = dia,
            pulse = pulse,
            confidence = listOf(sys, dia, pulse).count { it != null } / 3f
        )
    }

    private fun trimEmptyRows(mat: Mat): Mat {
        if (mat.empty()) return Mat()

        var top = 0
        var bottom = mat.rows() - 1

        while (top <= bottom) {
            val r = mat.row(top)
            val nz = Core.countNonZero(r)
            r.release()
            if (nz > 0) break
            top++
        }

        while (bottom >= top) {
            val r = mat.row(bottom)
            val nz = Core.countNonZero(r)
            r.release()
            if (nz > 0) break
            bottom--
        }

        if (bottom < top) return Mat()
        return Mat(
            mat,
            org.opencv.core.Rect(0, top, mat.cols(), bottom - top + 1)
        ).clone()
    }

    private fun gaussianSmooth(arr: FloatArray, sigma: Float): FloatArray {
        val radius = (sigma * 3).toInt().coerceAtLeast(1)
        val kernel = FloatArray(2 * radius + 1) { i ->
            val x = (i - radius).toFloat()
            exp(-(x * x) / (2 * sigma * sigma)).toFloat()
        }

        val result = FloatArray(arr.size)
        for (i in arr.indices) {
            var sum = 0f
            var wSum = 0f
            for (k in kernel.indices) {
                val idx = i + k - radius
                if (idx in arr.indices) {
                    sum += arr[idx] * kernel[k]
                    wSum += kernel[k]
                }
            }
            result[i] = if (wSum > 0f) sum / wSum else 0f
        }
        return result
    }

    private fun findPeaks(arr: FloatArray, minDistance: Int): List<Int> {
        val peaks = mutableListOf<Int>()
        for (i in 1 until arr.size - 1) {
            if (arr[i] > arr[i - 1] && arr[i] >= arr[i + 1]) {
                if (peaks.isEmpty() || i - peaks.last() >= minDistance) {
                    peaks.add(i)
                }
            }
        }
        return peaks
    }

    private fun kmeansThreshold(densities: FloatArray): Float {
        if (densities.isEmpty()) return 0.05f

        var c0 = densities.minOrNull() ?: return 0.05f
        var c1 = densities.maxOrNull() ?: return 0.05f

        if (c0 == c1) return max(c0, 0.05f)

        repeat(20) {
            val mid = (c0 + c1) / 2f
            val group0 = densities.filter { it <= mid }
            val group1 = densities.filter { it > mid }

            c0 = if (group0.isNotEmpty()) group0.average().toFloat() else c0
            c1 = if (group1.isNotEmpty()) group1.average().toFloat() else c1
        }

        return max((c0 + c1) / 2f, 0.05f)
    }
}