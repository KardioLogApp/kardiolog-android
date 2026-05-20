package com.example.addnevnik.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
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

class BpOcrParser {
    private companion object {
        const val TAG = "BP_OCR"
        const val BUILD_MARK = "v2024-05-20-01-ROW-THRESH"
    }

    init {
        Log.i(TAG, "★ BpOcrParser $BUILD_MARK created")
    }

    private var frameCount = 0
    private val history = ArrayDeque<BpResult>(5)
    private var debugDir: File? = null

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

    private fun ensureDebugDir(): File? {
        if (debugDir != null) return debugDir
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(base, "KardioLog/debug_ocr/${System.currentTimeMillis()}")
        if (dir.mkdirs()) {
            debugDir = dir
            Log.d(TAG, "debug dir created: ${dir.absolutePath}")
            return dir
        }
        Log.e(TAG, "failed to create debug dir: ${dir.absolutePath}")
        return null
    }

    private fun saveDebugMat(mat: Mat, name: String) {
        if (mat.empty()) {
            Log.d(TAG, "saveDebugMat: $name is EMPTY, skipping")
            return
        }
        val dir = ensureDebugDir() ?: return
        val file = File(dir, "${String.format("%04d", frameCount)}_$name.png")
        val ok = Imgcodecs.imwrite(file.absolutePath, mat)
        if (ok) {
            Log.d(TAG, "saved debug image: ${file.name} (${mat.cols()}x${mat.rows()})")
        } else {
            Log.e(TAG, "failed to save debug image: ${file.name}")
        }
    }

    fun analyze(image: ImageProxy, guideRect: RectF): BpOcrResult? {
        frameCount++
        Log.d(TAG, ">>> FRAME $frameCount (${image.width}x${image.height}), fmt=${image.format}")

        if (frameCount % 15 != 0) {
            return null
        }

        Log.d(TAG, "processing frame $frameCount (every 15th)")
        Log.d(TAG, "guideRect=[l=${guideRect.left}, t=${guideRect.top}, r=${guideRect.right}, b=${guideRect.bottom}]")
        Log.d(TAG, "history size=${history.size}")

        val bgr = imageToBgrMat(image)
        if (bgr.empty()) {
            Log.e(TAG, "imageToBgrMat returned empty mat!")
            return null
        }

        // Handle Rotation
        val rotation = image.imageInfo.rotationDegrees
        val rotatedBgr = Mat()
        when (rotation) {
            90 -> Core.rotate(bgr, rotatedBgr, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(bgr, rotatedBgr, Core.ROTATE_180)
            270 -> Core.rotate(bgr, rotatedBgr, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> bgr.copyTo(rotatedBgr)
        }
        bgr.release()

        // Transform guideRect to match rotated image coordinates
        val imgW = image.width.toFloat()
        val imgH = image.height.toFloat()
        val rotatedGuideRect = when (rotation) {
            90 -> RectF(
                guideRect.top,
                imgW - guideRect.right,
                guideRect.bottom,
                imgW - guideRect.left
            )
            180 -> RectF(
                imgW - guideRect.right,
                imgH - guideRect.bottom,
                imgW - guideRect.left,
                imgH - guideRect.top
            )
            270 -> RectF(
                imgH - guideRect.bottom,
                guideRect.left,
                imgH - guideRect.top,
                guideRect.right
            )
            else -> guideRect
        }

        Log.d(TAG, "BGR mat (rotated $rotation): ${rotatedBgr.cols()}x${rotatedBgr.rows()}")
        saveDebugMat(rotatedBgr, "00_bgr_full")

        return try {
            val ocrResult = runOcr(rotatedBgr, rotatedGuideRect)

            Log.d(TAG, "<<< FINAL: sys=${ocrResult.result.sys}, dia=${ocrResult.result.dia}, pulse=${ocrResult.result.pulse}, conf=${ocrResult.result.confidence}")
            ocrResult.rows.forEachIndexed { i, row ->
                Log.d(TAG, "  row[$i] value=${row.value}, conf=${row.rowConfidence}, rect=[l=${row.rect.left}, t=${row.rect.top}, r=${row.rect.right}, b=${row.rect.bottom}]")
            }

            history.addLast(ocrResult.result)
            if (history.size > 5) history.removeFirst()
            Log.d(TAG, "history: ${history.joinToString(" | ")}")

            ocrResult
        } catch (t: Throwable) {
            Log.e(TAG, "CRASH on frame $frameCount", t)
            null
        } finally {
            rotatedBgr.release()
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

        Log.d(TAG, "YUV planes: Y stride=$yRowStride, UV stride=$uvRowStride, UV pixelStride=$uvPixelStride")

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

    private fun preprocessCrop(bgr: Mat, guideRect: RectF): Mat {
        val gw = guideRect.width()
        val gh = guideRect.height()
        val pad = (min(gw, gh) / 30f).toInt()

        val x = max(0, guideRect.left.toInt() + pad)
        val y = max(0, guideRect.top.toInt() + pad)
        val x2 = min(bgr.cols(), guideRect.right.toInt() - pad)
        val y2 = min(bgr.rows(), guideRect.bottom.toInt() - pad)

        Log.d(TAG, "crop: guide=[${guideRect.left.toInt()},${guideRect.top.toInt()},${guideRect.right.toInt()},${guideRect.bottom.toInt()}], pad=$pad, bounds=[$x,$y,$x2,$y2], source=${bgr.cols()}x${bgr.rows()}")

        if (x2 <= x || y2 <= y) {
            Log.w(TAG, "INVALID crop bounds: x2<=$x or y2<=$y")
            return Mat()
        }

        val cropped = Mat(bgr, org.opencv.core.Rect(x, y, x2 - x, y2 - y))
        saveDebugMat(cropped, "01_cropped")

        val gray = Mat()
        Imgproc.cvtColor(cropped, gray, Imgproc.COLOR_BGR2GRAY)
        cropped.release()
        saveDebugMat(gray, "02_gray")

        // Reduced Gaussian blur (5x5) to preserve thin segments while removing noise
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
        gray.release()
        saveDebugMat(blurred, "03_blur")

        // Adaptive threshold - better for uneven lighting
        val adaptive = Mat()
        Imgproc.adaptiveThreshold(
            blurred,
            adaptive,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            31, // blockSize (Int)
            10.0  // C value (Double)
        )
        blurred.release()
        saveDebugMat(adaptive, "04_adaptive")

        // Morphological operations
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 3.0))
        val closed = Mat()
        Imgproc.morphologyEx(adaptive, closed, Imgproc.MORPH_CLOSE, kernel)
        adaptive.release()

        val opened = Mat()
        val openKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(closed, opened, Imgproc.MORPH_OPEN, openKernel)
        closed.release()
        openKernel.release()
        saveDebugMat(opened, "05_morph")

        val nonZero = Core.countNonZero(opened)
        val totalPixels = opened.rows() * opened.cols()
        val fillRatio = if (totalPixels > 0) nonZero.toFloat() / totalPixels else 0f
        Log.d(TAG, "preprocess done: ${opened.cols()}x${opened.rows()}, nonZero=$nonZero/$totalPixels (${String.format("%.1f", fillRatio * 100)}%)")

        return opened
    }

    private fun cropDigitZone(binary: Mat): Mat {
        if (binary.empty()) return Mat()

        val sw = binary.cols()
        val sh = binary.rows()

        // Vertical projection to find digit regions
        val vProjMat = Mat()
        Core.reduce(binary, vProjMat, 0, Core.REDUCE_SUM, CvType.CV_32F)
        val vProj = FloatArray(sw) { col -> vProjMat.get(0, col)[0].toFloat() }
        vProjMat.release()

        // Find the main digit block by looking for high-density columns
        val threshold = (vProj.maxOrNull() ?: 0f) * 0.15f
        var x0 = -1
        var x1 = -1

        // Find left edge
        for (col in 0 until sw) {
            if (vProj[col] >= threshold) {
                x0 = col
                break
            }
        }

        // Find right edge
        for (col in sw - 1 downTo 0) {
            if (vProj[col] >= threshold) {
                x1 = col
                break
            }
        }

        if (x0 == -1 || x1 == -1 || x1 <= x0) {
            Log.w(TAG, "digitZone FAILED: no significant projection found")
            // Fallback to hardcoded percentages
            x0 = (0.25 * sw).toInt()
            x1 = (0.88 * sw).toInt()
            Log.w(TAG, "digitZone FALLBACK: x0=$x0, x1=$x1")
        }

        // Add small padding
        val padding = (sw * 0.02).toInt().coerceAtLeast(2)
        x0 = (x0 - padding).coerceAtLeast(0)
        x1 = (x1 + padding).coerceAtMost(sw - 1)

        if (x1 <= x0) {
            Log.w(TAG, "digitZone FAILED after padding: x1($x1) <= x0($x0)")
            return Mat()
        }

        Log.d(TAG, "digitZone: x0=$x0, x1=$x1, width=${x1 - x0} (dynamic)")

        val zone = Mat(binary, org.opencv.core.Rect(x0, 0, x1 - x0, binary.rows()))
        saveDebugMat(zone, "06_digit_zone")
        return zone
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
        val minVal = smoothed.minOrNull() ?: 0f
        val inverted = FloatArray(h) { maxVal - smoothed[it] }

        Log.d(TAG, "hProjection: h=$h, maxProj=$maxVal, minProj=$minVal, sigma=$sigma")

        val peaks = findPeaks(inverted, minDistance = max(1, h / 5))
        Log.d(TAG, "valleys (peaks of inverted): $peaks")

        val boundaries = mutableListOf(0)
        boundaries.addAll(peaks)
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

        Log.d(TAG, "segments before filter: ${segments.map { "${it.first.first}-${it.first.last} (h=${it.second})" }}")

        val top3 = segments
            .sortedByDescending { it.second }
            .take(3)
            .sortedBy { it.first.first }

        Log.d(TAG, "top 3 row bands: ${top3.map { "${it.first.first}-${it.first.last} (h=${it.second})" }}")

        return top3.map { it.first }
    }

    private fun findDigitRects(rowBinary: Mat): List<IntRange> {
        if (rowBinary.empty()) return emptyList()

        val w = rowBinary.cols()
        val rowH = rowBinary.rows()

        val vProjMat = Mat()
        Core.reduce(rowBinary, vProjMat, 0, Core.REDUCE_SUM, CvType.CV_32F)
        val vProj = IntArray(w) { col -> vProjMat.get(0, col)[0].toInt() }
        vProjMat.release()

        val maxProj = vProj.maxOrNull() ?: 0
        val threshold = max(1, (maxProj * 0.15f).toInt())
        Log.d(TAG, "vProjection row=${rowH}x${w}: maxCol=$maxProj, threshold=$threshold")

        val groups = mutableListOf<IntRange>()
        var start = -1
        for (col in 0..w) {
            val active = col < w && vProj[col] >= threshold
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

        Log.d(TAG, "digit groups: ${groups.map { "${it.first}-${it.last} (w=${it.last - it.first + 1})" }}")

        return groups
    }

    private data class DigitData(val range: IntRange, val densities: FloatArray)

    // Returns raw densities for global thresholding
    private fun getDigitDensities(roi: Mat): FloatArray {
        val trimmed = trimEmptyRows(roi)

        if (trimmed.empty() || trimmed.rows() < 4 || trimmed.cols() < 2) {
            trimmed.release()
            return FloatArray(7) // Return empty/zero densities
        }

        val aspectRatio = trimmed.rows().toFloat() / trimmed.cols().toFloat()
        if (aspectRatio > 3.5f) {
            trimmed.release()
            // Return densities that force '1' (only b and c segments high)
            // Actually, for global thresholding, we should return realistic densities.
            // '1' has low density in most zones except right side.
            // Let's return a specific pattern or just let the caller handle aspect ratio.
            // For now, return zero array, caller handles '1' logic separately if needed.
            return FloatArray(7) 
        }

        val h = trimmed.rows().toFloat()
        val w = trimmed.cols().toFloat()

        val zones = arrayOf(
            floatArrayOf(0.00f, 0.22f, 0.15f, 0.85f),   // a(top)
            floatArrayOf(0.08f, 0.48f, 0.70f, 1.00f),   // b(top-R)
            floatArrayOf(0.52f, 0.92f, 0.70f, 1.00f),   // c(bot-R)
            floatArrayOf(0.78f, 1.00f, 0.15f, 0.85f),   // d(bot)
            floatArrayOf(0.52f, 0.92f, 0.00f, 0.30f),   // e(bot-L)
            floatArrayOf(0.08f, 0.48f, 0.00f, 0.30f),   // f(top-L)
            floatArrayOf(0.38f, 0.62f, 0.20f, 0.80f)    // g(mid)
        )

        val densities = FloatArray(7) { i ->
            val z = zones[i]
            val ry0 = (z[0] * h).toInt().coerceIn(0, trimmed.rows() - 1)
            val ry1 = (z[1] * h).toInt().coerceIn(ry0 + 1, trimmed.rows())
            val rx0 = (z[2] * w).toInt().coerceIn(0, trimmed.cols() - 1)
            val rx1 = (z[3] * w).toInt().coerceIn(rx0 + 1, trimmed.cols())

            val zone = Mat(trimmed, org.opencv.core.Rect(rx0, ry0, rx1 - rx0, ry1 - ry0))
            val nonZero = Core.countNonZero(zone).toFloat()
            val area = ((ry1 - ry0) * (rx1 - rx0)).toFloat()
            zone.release()
            if (area > 0f) nonZero / area else 0f
        }

        trimmed.release()
        return densities
    }

    private fun runOcr(bgr: Mat, guideRect: RectF): BpOcrResult {
        val binary = preprocessCrop(bgr, guideRect)
        if (binary.empty()) {
            Log.w(TAG, "runOcr: binary EMPTY after preprocess")
            return BpOcrResult(BpResult(null, null, null, 0f), emptyList())
        }

        val digitZone = cropDigitZone(binary)
        binary.release()

        if (digitZone.empty()) {
            Log.w(TAG, "runOcr: digitZone EMPTY")
            return BpOcrResult(BpResult(null, null, null, 0f), emptyList())
        }

        val rowBands = findRowBands(digitZone)
        Log.d(TAG, "rowBands count=${rowBands.size}: ${rowBands.map { "${it.first}-${it.last}" }}")

        val debugRows = mutableListOf<RowDebugInfo>()
        val values = mutableListOf<Int?>()

        val scaleX = (guideRect.right - guideRect.left) * 0.63f / digitZone.cols().coerceAtLeast(1)
        val scaleY = (guideRect.bottom - guideRect.top) / digitZone.rows().coerceAtLeast(1)
        val rxBase = guideRect.left + 0.25f * (guideRect.right - guideRect.left)

        try {
            for (bandIdx in rowBands.indices) {
                val band = rowBands[bandIdx]
                val bandH = (band.last - band.first + 1).coerceAtMost(digitZone.rows() - band.first)
                if (bandH <= 0) continue

                val rowMat = Mat(digitZone, org.opencv.core.Rect(0, band.first, digitZone.cols(), bandH))
                saveDebugMat(rowMat, "07_row_${bandIdx}_band")

                val sb = StringBuilder()
                var rowConf = 0f

                try {
                    val digitRanges = findDigitRects(rowMat)
                    Log.d(TAG, "row $bandIdx: ${digitRanges.size} digit groups")

                    if (digitRanges.isEmpty()) {
                        rowConf = 0f
                    } else {
                        // 1. Collect densities for all digits in this row
                        val digitDataList = mutableListOf<DigitData>()
                        var hasValidDigits = false

                        for (range in digitRanges) {
                            val roiW = range.last - range.first + 1
                            if (roiW <= 0) continue

                            val roiMat = Mat(rowMat, org.opencv.core.Rect(range.first, 0, roiW, rowMat.rows()))
                            // Check aspect ratio for '1' before adding to list
                            val aspect = roiMat.rows().toFloat() / roiMat.cols().toFloat()
                            
                            if (aspect > 3.5f) {
                                // It's a '1', handle separately
                                sb.append('1')
                                rowConf = max(rowConf, 0.9f)
                                hasValidDigits = true
                                Log.d(TAG, "digit: aspectRatio=$aspect -> forced '1'")
                            } else {
                                val densities = getDigitDensities(roiMat)
                                digitDataList.add(DigitData(range, densities))
                            }
                            roiMat.release()
                        }

                        // 2. Calculate GLOBAL threshold for the row
                        if (digitDataList.isNotEmpty()) {
                            val allDensities = digitDataList.flatMap { it.densities.toList() }.toFloatArray()
                            val rowThreshold = kmeansThreshold(allDensities)
                            Log.d(TAG, "row $bandIdx: global threshold=$rowThreshold")

                            // 3. Decode digits using global threshold
                            for (data in digitDataList) {
                                val segs = data.densities.map { it > rowThreshold }
                                val digit = segmentMap[segs]
                                
                                val segStr = segs.mapIndexed { i, on -> "${if (on) "1" else "0"}" }.joinToString("")
                                Log.d(TAG, "digit pattern=$segStr, result=$digit")

                                if (digit != null) {
                                    sb.append(digit)
                                    hasValidDigits = true
                                    // Calculate confidence based on distance from threshold
                                    val dist = data.densities.map { kotlin.math.abs(it - rowThreshold) }.average().toFloat()
                                    rowConf = max(rowConf, min(1f, dist * 5f)) 
                                }
                            }
                        }
                        
                        rowConf = if (hasValidDigits) rowConf else 0f
                    }

                } finally {
                    rowMat.release()
                }

                val num = sb.toString().toIntOrNull()
                values.add(num)
                Log.d(TAG, "row $bandIdx: text='${sb}' parsed=$num, conf=${String.format("%.2f", rowConf)}")

                val ry = guideRect.top + band.first * scaleY
                debugRows.add(
                    RowDebugInfo(
                        rect = RectF(rxBase, ry, rxBase + digitZone.cols() * scaleX, ry + bandH * scaleY),
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

        Log.d(TAG, "raw values: [$sys, $dia, $pulse]")

        // Range checks
        if (sys != null && sys !in 60..260) { Log.d(TAG, "sys=$sys OUT OF RANGE [60-260], nulling"); sys = null }
        if (dia != null && dia !in 40..160) { Log.d(TAG, "dia=$dia OUT OF RANGE [40-160], nulling"); dia = null }
        if (pulse != null && pulse !in 30..220) { Log.d(TAG, "pulse=$pulse OUT OF RANGE [30-220], nulling"); pulse = null }

        // Pulse Pressure Check
        if (sys != null && dia != null) {
            val pp = sys - dia
            when {
                pp < 15 -> { Log.d(TAG, "Pulse Pressure $pp < 15, invalid"); sys = null; dia = null }
                pp > 90 -> { Log.d(TAG, "Pulse Pressure $pp > 90, suspicious") } // Keep but maybe lower conf?
            }
        } else {
             if (sys != null && dia != null && sys <= dia) { Log.d(TAG, "sys($sys) <= dia($dia), nulling both"); sys = null; dia = null }
        }

        val currentResult = BpResult(sys, dia, pulse, listOf(sys, dia, pulse).count { it != null } / 3f)
        val majority = majorityVote(history.toList() + currentResult)

        Log.d(TAG, "current=$currentResult, majority=$majority, final=${majority ?: currentResult}")

        return BpOcrResult(majority ?: currentResult, debugRows)
    }

    private fun majorityVote(results: List<BpResult>): BpResult? {
        if (results.isEmpty()) return null

        // Sliding consensus: if a value appears >= 2 times in history, accept it.
        // This is faster than "3 of 5" and more stable than "1 of 1".
        fun <T> consensus(values: List<T?>): T? =
            values.filterNotNull()
                .groupingBy { it }
                .eachCount()
                .filter { it.value >= 2 } // Reduced from 3 to 2
                .maxByOrNull { it.value }
                ?.key

        val sys = consensus(results.map { it.sys })
        val dia = consensus(results.map { it.dia })
        val pulse = consensus(results.map { it.pulse })

        if (sys == null && dia == null && pulse == null) return null

        return BpResult(sys, dia, pulse, listOf(sys, dia, pulse).count { it != null } / 3f)
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
        return Mat(mat, org.opencv.core.Rect(0, top, mat.cols(), bottom - top + 1)).clone()
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
