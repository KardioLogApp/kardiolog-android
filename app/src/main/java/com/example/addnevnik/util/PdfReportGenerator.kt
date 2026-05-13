package com.example.addnevnik.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MeasurementRow(
    val date: Long,
    val morningAd: String?,
    val morningHr: Int?,
    val eveningAd: String?,
    val eveningHr: Int?,
    val note: String? = null
)

data class PatientInfo(
    val name: String,
    val gender: String,
    val birthDate: String,
    val avgSystolic: Int,
    val avgDiastolic: Int,
    val avgHr: Int
)

object PdfReportGenerator {

    private val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val titleFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private const val PW = 595
    private const val PH = 842
    private const val M = 40f

    private const val C_DATE = 80f
    private const val C_MAD = 80f
    private const val C_MHR = 55f
    private const val C_EAD = 80f
    private const val C_EHR = 55f
    private const val C_NOTE = 165f

    private const val ROW_H = 22f
    private const val HEAD_H = 26f

    private val COL_HEADER_BG = Color.parseColor("#2C3E50")
    private val COL_HEADER_TEXT = Color.WHITE
    private val COL_ROW_EVEN = Color.parseColor("#F2F2F2")
    private val COL_ROW_ODD = Color.WHITE
    private val COL_LINE = Color.parseColor("#CCCCCC")
    private val COL_BOLD_LINE = Color.parseColor("#888888")
    private val COL_HIGH_SYST = Color.parseColor("#C0392B")
    private val COL_HIGH_DIAS = Color.parseColor("#E67E22")
    private val COL_TEXT = Color.parseColor("#1A1A1A")
    private val COL_MUTED = Color.parseColor("#555555")
    private val COL_ACCENT = Color.parseColor("#2980B9")

    fun generate(
        context: Context,
        patient: PatientInfo,
        rows: List<MeasurementRow>,
        locale: Locale = Locale.getDefault()
    ): File {
        val isRu = locale.language == "ru"

        val doc = PdfDocument()
        var pageNum = 1
        var page = newPage(doc, pageNum)
        var canvas = page.canvas
        var y = M

        y = drawDocHeader(canvas, patient, rows, y, isRu)
        y = drawTableHeader(canvas, y, isRu)

        val sorted = rows.sortedByDescending { it.date }
        sorted.forEachIndexed { i, row ->
            if (y + ROW_H > PH - M - 30f) {
                drawPageFooter(canvas, pageNum)
                doc.finishPage(page)
                pageNum++
                page = newPage(doc, pageNum)
                canvas = page.canvas
                y = M
                y = drawTableHeader(canvas, y, isRu)
            }
            y = drawRow(canvas, row, y, i % 2 == 0)
        }

        drawLine(canvas, M, y, PW - M, y, COL_BOLD_LINE, 1f)
        y += 14f
        drawSummary(canvas, patient, y, isRu)

        drawPageFooter(canvas, pageNum)
        doc.finishPage(page)

        val name = if (isRu) {
            "Dnevnik_AD_${System.currentTimeMillis()}.pdf"
        } else {
            "BloodPressure_Diary_${System.currentTimeMillis()}.pdf"
        }

        val file = File(context.getExternalFilesDir(null), name)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun newPage(doc: PdfDocument, num: Int): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(PW, PH, num).create()
        return doc.startPage(info)
    }

    private fun drawDocHeader(
        c: Canvas,
        p: PatientInfo,
        rows: List<MeasurementRow>,
        startY: Float,
        isRu: Boolean
    ): Float {
        var y = startY

        val titlePaint = paint(18f, COL_TEXT, bold = true)
        val title = if (isRu) {
            "Дневник самоконтроля артериального давления"
        } else {
            "Blood Pressure Diary"
        }
        c.drawText(title, (PW - titlePaint.measureText(title)) / 2f, y + 18f, titlePaint)
        y += 32f

        drawLine(c, M, y, PW - M, y, COL_ACCENT, 1.5f)
        y += 10f

        val reg = paint(10f, COL_MUTED)
        val bold = paint(10f, COL_TEXT, bold = true)

        val lblPatient = if (isRu) "Пациент:" else "Patient:"
        val lblGender = if (isRu) "Пол:" else "Gender:"
        val lblBirth = if (isRu) "Дата рождения:" else "Birth Date:"
        val lblPeriod = if (isRu) "Период наблюдения:" else "Observation Period:"

        c.drawText(lblPatient, M, y, reg)
        c.drawText(p.name.ifBlank { "—" }, M + 80f, y, bold)
        c.drawText(lblGender, PW / 2f, y, reg)
        c.drawText(p.gender.ifBlank { "—" }, PW / 2f + 50f, y, bold)
        y += 16f

        c.drawText(lblBirth, M, y, reg)
        c.drawText(p.birthDate.ifBlank { "—" }, M + 80f, y, bold)

        if (rows.isNotEmpty()) {
            val minD = titleFmt.format(Date(rows.minOf { it.date }))
            val maxD = titleFmt.format(Date(rows.maxOf { it.date }))
            c.drawText(lblPeriod, PW / 2f, y, reg)
            c.drawText("$minD — $maxD", PW / 2f + 115f, y, bold)
        }

        y += 20f
        drawLine(c, M, y, PW - M, y, COL_LINE, 0.5f)
        y += 12f

        return y
    }

    private fun drawTableHeader(c: Canvas, startY: Float, isRu: Boolean): Float {
        val bot = startY + HEAD_H

        val bg = Paint().apply { color = COL_HEADER_BG }
        c.drawRect(M, startY, PW - M, bot, bg)

        val hp = paint(9f, COL_HEADER_TEXT, bold = true)
        val heads = if (isRu) {
            listOf("Дата", "Утро АД", "Утро ЧСС", "Вечер АД", "Вечер ЧСС", "Примечание")
        } else {
            listOf("Date", "Morning BP", "Morning HR", "Evening BP", "Evening HR", "Notes")
        }
        val widths = listOf(C_DATE, C_MAD, C_MHR, C_EAD, C_EHR, C_NOTE)

        var x = M
        val textY = startY + HEAD_H / 2f + hp.textSize / 2f - 2f
        for (i in heads.indices) {
            val tw = hp.measureText(heads[i])
            c.drawText(heads[i], x + (widths[i] - tw) / 2f, textY, hp)
            if (i > 0) {
                drawLine(c, x, startY, x, bot, Color.parseColor("#4A5568"), 0.5f)
            }
            x += widths[i]
        }
        drawLine(c, PW - M, startY, PW - M, bot, Color.parseColor("#4A5568"), 0.5f)

        return bot
    }

    private fun drawRow(c: Canvas, row: MeasurementRow, startY: Float, even: Boolean): Float {
        val bot = startY + ROW_H

        val bg = Paint().apply { color = if (even) COL_ROW_EVEN else COL_ROW_ODD }
        c.drawRect(M, startY, PW - M, bot, bg)

        val widths = listOf(C_DATE, C_MAD, C_MHR, C_EAD, C_EHR, C_NOTE)
        val textY = startY + ROW_H / 2f + 9f / 2f + 3f

        fun adColor(ad: String?): Int {
            if (ad == null) return COL_MUTED
            val parts = ad.split("/")
            val syst = parts.getOrNull(0)?.toIntOrNull() ?: return COL_TEXT
            val dias = parts.getOrNull(1)?.toIntOrNull() ?: return COL_TEXT
            return when {
                syst > 139 -> COL_HIGH_SYST
                dias > 89 -> COL_HIGH_DIAS
                else -> COL_TEXT
            }
        }

        val values = listOf(
            dateFmt.format(Date(row.date)),
            row.morningAd ?: "—",
            row.morningHr?.toString() ?: "—",
            row.eveningAd ?: "—",
            row.eveningHr?.toString() ?: "—",
            row.note ?: ""
        )

        val colors = listOf(
            COL_TEXT,
            adColor(row.morningAd),
            COL_TEXT,
            adColor(row.eveningAd),
            COL_TEXT,
            COL_MUTED
        )

        val isBold = listOf(false, true, false, true, false, false)

        var x = M
        for (i in values.indices) {
            drawLine(c, x, startY, x, bot, COL_LINE, 0.3f)
            val tp = paint(9f, colors[i], bold = isBold[i])
            var txt = values[i]
            val maxW = widths[i] - 8f
            while (txt.isNotEmpty() && tp.measureText(txt) > maxW) {
                txt = txt.dropLast(1)
            }

            val tx = if (i in listOf(1, 2, 3, 4)) {
                x + (widths[i] - tp.measureText(txt)) / 2f
            } else {
                x + 4f
            }

            c.drawText(txt, tx, textY, tp)
            x += widths[i]
        }

        drawLine(c, PW - M, startY, PW - M, bot, COL_LINE, 0.3f)
        drawLine(c, M, bot, PW - M, bot, COL_LINE, 0.3f)

        return bot
    }

    private fun drawSummary(c: Canvas, p: PatientInfo, y: Float, isRu: Boolean) {
        val label = if (isRu) "Средние значения за период:" else "Average values for the period:"
        c.drawText(label, M, y, paint(10f, COL_MUTED))

        val cardW = 100f
        val cardH = 44f
        val gap = 12f
        val startX = M
        val cardY = y + 8f

        data class Card(val lbl: String, val value: String, val color: Int)

        val sColor = if (p.avgSystolic > 139) COL_HIGH_SYST else COL_ACCENT
        val dColor = if (p.avgDiastolic > 89) COL_HIGH_DIAS else COL_ACCENT

        val cards = listOf(
            Card(if (isRu) "СИСТ" else "SYS", "${p.avgSystolic}", sColor),
            Card(if (isRu) "ДИАС" else "DIA", "${p.avgDiastolic}", dColor),
            Card(if (isRu) "ПУЛЬС" else "HR", "${p.avgHr}", COL_TEXT)
        )

        cards.forEachIndexed { i, card ->
            val cx = startX + i * (cardW + gap)
            val rect = RectF(cx, cardY, cx + cardW, cardY + cardH)

            val bgPaint = Paint().apply {
                color = Color.parseColor("#F0F4F8")
                isAntiAlias = true
            }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#DDE3EA")
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            c.drawRoundRect(rect, 8f, 8f, bgPaint)
            c.drawRoundRect(rect, 8f, 8f, borderPaint)

            val lp = paint(8f, COL_MUTED, bold = true)
            c.drawText(card.lbl, cx + (cardW - lp.measureText(card.lbl)) / 2f, cardY + 14f, lp)

            val vp = paint(16f, card.color, bold = true)
            c.drawText(card.value, cx + (cardW - vp.measureText(card.value)) / 2f, cardY + 36f, vp)
        }

        val legY = cardY + cardH + 16f
        val redDot = Paint().apply { color = COL_HIGH_SYST; isAntiAlias = true }
        val orDot = Paint().apply { color = COL_HIGH_DIAS; isAntiAlias = true }

        c.drawCircle(M + 6f, legY - 3f, 4f, redDot)
        val legText1 = if (isRu) "— систола > 139 мм рт.ст." else "— systolic > 139 mmHg"
        c.drawText(legText1, M + 14f, legY, paint(8f, COL_MUTED))

        c.drawCircle(M + 166f, legY - 3f, 4f, orDot)
        val legText2 = if (isRu) "— диастола > 89 мм рт.ст." else "— diastolic > 89 mmHg"
        c.drawText(legText2, M + 174f, legY, paint(8f, COL_MUTED))
    }

    private fun drawPageFooter(c: Canvas, pageNum: Int) {
        val txt = "CardioLog  ·  $pageNum"
        val fp = paint(8f, COL_MUTED)
        drawLine(c, M, PH - 28f, PW - M, PH - 28f, COL_LINE, 0.5f)
        c.drawText(txt, (PW - fp.measureText(txt)) / 2f, PH - 16f, fp)
    }

    private fun drawLine(
        c: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
        w: Float
    ) {
        val p = Paint().apply {
            this.color = color
            strokeWidth = w
            style = Paint.Style.STROKE
        }
        c.drawLine(x1, y1, x2, y2, p)
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint().apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        isAntiAlias = true
    }
}
