package com.example.addnevnik.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.addnevnik.data.local.BloodPressureEntity
import com.example.addnevnik.domain.BpCategory
import com.example.addnevnik.domain.BpClassifier
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun generateReport(data: List<BloodPressureEntity>): File? {
        val fileName = "CardioLog_Report_${System.currentTimeMillis()}.pdf"
        val filePath = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }
        val textPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 12f
        }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 40f

        canvas.drawText("КардиоЛог — Отчёт об артериальном давлении", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("Дата формирования: ${dateFormat.format(Date())}", 40f, y, textPaint)
        y += 40f

        // Table headers
        val xCols = floatArrayOf(40f, 180f, 240f, 300f, 360f, 450f)
        val headers = arrayOf("Дата", "Сис", "Диа", "Пульс", "Категория")
        
        headers.forEachIndexed { index, s ->
            canvas.drawText(s, xCols[index], y, headerPaint)
        }
        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        data.forEach { entry ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            canvas.drawText(dateFormat.format(Date(entry.timestamp_ms)), xCols[0], y, textPaint)
            canvas.drawText(entry.systolic.toString(), xCols[1], y, textPaint)
            canvas.drawText(entry.diastolic.toString(), xCols[2], y, textPaint)
            canvas.drawText(entry.pulse.toString(), xCols[3], y, textPaint)

            val category = BpClassifier.classify(entry.systolic, entry.diastolic)
            val categoryText = when(category) {
                BpCategory.Low -> "Низкое"
                BpCategory.Optimal -> "Оптимальное"
                BpCategory.Normal -> "Нормальное"
                BpCategory.Elevated -> "Повышенное"
                BpCategory.High -> "Высокое"
                BpCategory.VeryHigh -> "Очень выс."
                BpCategory.CriticallyHigh -> "Крит."
                BpCategory.Emergency -> "Экстрен."
            }
            canvas.drawText(categoryText, xCols[4], y, textPaint)
            y += 20f
        }

        if (data.isNotEmpty()) {
            y += 20f
            val avgSys = data.map { it.systolic }.average().toInt()
            val avgDia = data.map { it.diastolic }.average().toInt()
            val avgPulse = data.map { it.pulse }.average().toInt()

            canvas.drawText("Средние значения:", 40f, y, headerPaint)
            y += 20f
            canvas.drawText("Систолическое: $avgSys мм рт. ст.", 40f, y, textPaint)
            y += 20f
            canvas.drawText("Диастолическое: $avgDia мм рт. ст.", 40f, y, textPaint)
            y += 20f
            canvas.drawText("Пульс: $avgPulse уд/мин", 40f, y, textPaint)
        }

        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(FileOutputStream(filePath))
            pdfDocument.close()
            return filePath
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
