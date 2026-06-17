package com.example.addnevnik.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MeasurementRow(
    val date: Long,
    val morningTime: String?,   // format "HH:mm", nullable if no morning measurement
    val eveningTime: String?,   // format "HH:mm", nullable if no evening measurement
    val morningAd: String?,
    val morningHr: Int?,
    val eveningAd: String?,
    val eveningHr: Int?,
    val note: String? = null,
    val medication: String? = null,   // from DailyNoteEntity
    val wellbeing: String? = null,    // from DailyNoteEntity
    val outOfRangeCount: Int = 0      // measurements outside morning/evening windows
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
    private val fileDateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private const val PW = 595f
    private const val PH = 842f
    private const val MARGIN_LEFT = 57f
    private const val MARGIN_RIGHT = 28f
    private const val MARGIN_TOP = 16f
    private const val MARGIN_BOTTOM = 36f

    private val workingWidth = PW - MARGIN_LEFT - MARGIN_RIGHT
    private val C_DATE = workingWidth * 0.12f
    private val C_MAD = workingWidth * 0.18f
    private val C_MHR = workingWidth * 0.10f
    private val C_EAD = workingWidth * 0.18f
    private val C_EHR = workingWidth * 0.10f
    private val C_NOTE = workingWidth * 0.32f

    private const val HEAD_H = 38f
    private const val ROW_H = 28f
    private const val ROW_H_EXPANDED = 40f
    private const val INFO_BLOCK_H = 36f
    private const val INFO_LINE_H = 12f
    private const val INFO_FONT_SIZE = 9f
    private const val GAP_BEFORE_TABLE = 6f

    private const val THRESHOLD_SYSTOLIC = 135
    private const val THRESHOLD_DIASTOLIC = 85

    private val COL_HEADER_BG = Color.parseColor("#1A7A7A")
    private val COL_HEADER_EVENING = Color.parseColor("#489595")
    private val COL_ROW_EVEN = Color.parseColor("#F2F2F2")
    private val COL_ROW_ODD = Color.WHITE
    private val COL_DATE_TINT = Color.parseColor("#08000000")
    private val COL_LINE = Color.parseColor("#CCCCCC")
    private val COL_BOLD_LINE = Color.parseColor("#888888")
    private val COL_HIGH_SYST = Color.parseColor("#C0392B")
    private val COL_HIGH_DIAS = Color.parseColor("#E67E22")
    private val COL_TEXT = Color.parseColor("#1A1A1A")
    private val COL_MUTED = Color.parseColor("#555555")
    private val COL_ACCENT = Color.parseColor("#1A7A7A")
    private val COL_EMPTY = Color.parseColor("#AAAAAA")

    private var typefaceRegular: Typeface? = null
    private var typefaceBold: Typeface? = null

    private fun loadTypefaces(context: Context) {
        if (typefaceRegular == null) {
            typefaceRegular = try {
                Typeface.createFromAsset(context.assets, "fonts/NotoSans-Regular.ttf")
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
        if (typefaceBold == null) {
            typefaceBold = try {
                Typeface.createFromAsset(context.assets, "fonts/NotoSans-Bold.ttf")
            } catch (e: Exception) {
                Typeface.DEFAULT_BOLD
            }
        }
    }

    fun generate(
        context: Context,
        patient: PatientInfo,
        rows: List<MeasurementRow>,
        locale: Locale = Locale.getDefault()
    ): File {
        loadTypefaces(context)
        val isRu = locale.language == "ru"

        val doc = PdfDocument()
        var pageNum = 1
        var page = newPage(doc, pageNum)
        var canvas = page.canvas
        var y = MARGIN_TOP

        y = drawDocHeader(canvas, patient, rows, y, isRu)
        y = drawTableHeader(canvas, y, isRu)

        val sorted = rows.sortedBy { it.date }
        sorted.forEachIndexed { i, row ->
            val expandedRow = !row.medication.isNullOrBlank() || !row.wellbeing.isNullOrBlank()
            val currentH = if (expandedRow) ROW_H_EXPANDED else ROW_H
            if (y + currentH > PH - MARGIN_BOTTOM - 30f) {
                drawPageFooter(canvas, pageNum)
                doc.finishPage(page)
                pageNum++
                page = newPage(doc, pageNum)
                canvas = page.canvas
                y = MARGIN_TOP

                val patientHeader = "${patient.name}  ·  " + if (isRu) "продолжение" else "continuation"
                val phPaint = paint(8f, COL_MUTED)
                canvas.drawText(
                    patientHeader,
                    MARGIN_LEFT + workingWidth - phPaint.measureText(patientHeader),
                    y + 8f,
                    phPaint
                )
                y += 12f

                y = drawTableHeader(canvas, y, isRu)
            }
            y = drawRow(canvas, row, y, i % 2 == 0)
        }

        drawLine(canvas, MARGIN_LEFT, y, MARGIN_LEFT + workingWidth, y, COL_BOLD_LINE, 1f)
        y += 20f
        drawSummary(canvas, patient, sorted, y, isRu)

        drawPageFooter(canvas, pageNum)
        doc.finishPage(page)

        val startDate = rows.minOfOrNull { it.date }?.let { fileDateFmt.format(Date(it)) } ?: "unknown"
        val endDate = rows.maxOfOrNull { it.date }?.let { fileDateFmt.format(Date(it)) } ?: "unknown"
        val nameParts = patient.name.trim().split("\\s+".toRegex())
        val lastName = transliterate(nameParts.getOrNull(0) ?: "Patient")
        val firstName = transliterate(nameParts.getOrNull(1) ?: "")

        val name = buildString {
            append(lastName)
            if (firstName.isNotEmpty()) append("_$firstName")
            append("_AD_$startDate-$endDate.pdf")
        }

        val file = File(context.getExternalFilesDir(null), name)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun newPage(doc: PdfDocument, num: Int): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), num).create()
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
        val title = if (isRu) "Дневник самоконтроля артериального давления" else "Blood Pressure Diary"
        c.drawText(
            title,
            MARGIN_LEFT + (workingWidth - titlePaint.measureText(title)) / 2f,
            y + 18f,
            titlePaint
        )
        y += 32f

        drawLine(c, MARGIN_LEFT, y, MARGIN_LEFT + workingWidth, y, COL_ACCENT, 1.5f)

        val infoBlockTop = y
        val totalTextH = INFO_LINE_H + INFO_FONT_SIZE
        val infoStartY = infoBlockTop + (INFO_BLOCK_H - totalTextH) / 2f + INFO_FONT_SIZE

        val reg = paint(INFO_FONT_SIZE, COL_MUTED)
        val bold = paint(INFO_FONT_SIZE, COL_TEXT, bold = true)
        val xCol2 = MARGIN_LEFT + workingWidth * 0.5f

        val label1 = if (isRu) "Пациент: " else "Patient: "
        c.drawText(label1, MARGIN_LEFT, infoStartY, reg)
        c.drawText(p.name.ifBlank { "—" }, MARGIN_LEFT + reg.measureText(label1), infoStartY, bold)

        val label2 = if (isRu) "Пол: " else "Gender: "
        c.drawText(label2, xCol2, infoStartY, reg)
        c.drawText(p.gender.ifBlank { "—" }, xCol2 + reg.measureText(label2), infoStartY, bold)

        val y2 = infoStartY + INFO_LINE_H
        val label3 = if (isRu) "Дата рождения: " else "Birth Date: "
        c.drawText(label3, MARGIN_LEFT, y2, reg)
        c.drawText(p.birthDate.ifBlank { "—" }, MARGIN_LEFT + reg.measureText(label3), y2, bold)

        if (rows.isNotEmpty()) {
            val minD = titleFmt.format(Date(rows.minOf { it.date }))
            val maxD = titleFmt.format(Date(rows.maxOf { it.date }))
            val label4 = if (isRu) "Период наблюдения: " else "Observation Period: "
            c.drawText(label4, xCol2, y2, reg)
            c.drawText("$minD — $maxD", xCol2 + reg.measureText(label4), y2, bold)
        }

        val afterInfoY = infoBlockTop + INFO_BLOCK_H
        drawLine(c, MARGIN_LEFT, afterInfoY, MARGIN_LEFT + workingWidth, afterInfoY, COL_LINE, 0.5f)

        return afterInfoY + GAP_BEFORE_TABLE
    }

    private fun drawTableHeader(c: Canvas, startY: Float, isRu: Boolean): Float {
        val bot = startY + HEAD_H
        val pTeal = Paint().apply { color = COL_HEADER_BG }
        val pLighterTeal = Paint().apply { color = COL_HEADER_EVENING }

        c.drawRect(MARGIN_LEFT, startY, MARGIN_LEFT + C_DATE, bot, pTeal)
        c.drawRect(MARGIN_LEFT + C_DATE, startY, MARGIN_LEFT + C_DATE + C_MAD + C_MHR, bot, pTeal)
        c.drawRect(
            MARGIN_LEFT + C_DATE + C_MAD + C_MHR,
            startY,
            MARGIN_LEFT + C_DATE + C_MAD + C_MHR + C_EAD + C_EHR,
            bot,
            pLighterTeal
        )
        c.drawRect(
            MARGIN_LEFT + C_DATE + C_MAD + C_MHR + C_EAD + C_EHR,
            startY,
            MARGIN_LEFT + workingWidth,
            bot,
            pTeal
        )

        val hp = paint(8f, Color.WHITE, bold = true)
        val up = paint(6.8f, Color.argb(216, 255, 255, 255))

        val heads = if (isRu) {
            listOf(
                "Дата",
                "Утро АД\nмм рт.ст.",
                "Утро ЧСС\nуд/мин",
                "Вечер АД\nмм рт.ст.",
                "Вечер ЧСС\nуд/мин",
                "Примечание"
            )
        } else {
            listOf(
                "Date",
                "Morning BP\nmmHg",
                "Morning HR\nbpm",
                "Evening BP\nmmHg",
                "Evening HR\nbpm",
                "Note"
            )
        }
        val widths = listOf(C_DATE, C_MAD, C_MHR, C_EAD, C_EHR, C_NOTE)

        var x = MARGIN_LEFT
        for (i in heads.indices) {
            val parts = heads[i].split("\n")
            if (parts.size > 1) {
                val h1 = 10f
                val h2 = 8f
                val spacing = 2f
                val totalH = h1 + h2 + spacing
                val topY = startY + (HEAD_H - totalH) / 2f

                val tw1 = hp.measureText(parts[0])
                c.drawText(parts[0], x + (widths[i] - tw1) / 2f, topY + h1, hp)
                val tw2 = up.measureText(parts[1])
                c.drawText(parts[1], x + (widths[i] - tw2) / 2f, topY + h1 + spacing + h2, up)
            } else {
                val tw = hp.measureText(heads[i])
                c.drawText(
                    heads[i],
                    x + (widths[i] - tw) / 2f,
                    startY + HEAD_H / 2f + 4f,
                    hp
                )
            }
            x += widths[i]
        }
        return bot
    }

    private fun drawRow(c: Canvas, row: MeasurementRow, startY: Float, even: Boolean): Float {
        val expandedRow = !row.medication.isNullOrBlank() || !row.wellbeing.isNullOrBlank()
        val currentH = if (expandedRow) ROW_H_EXPANDED else ROW_H
        val rowBgColor = if (even) COL_ROW_EVEN else COL_ROW_ODD

        c.drawRect(
            MARGIN_LEFT,
            startY,
            MARGIN_LEFT + workingWidth,
            startY + currentH,
            Paint().apply { color = rowBgColor }
        )
        c.drawRect(
            MARGIN_LEFT,
            startY,
            MARGIN_LEFT + C_DATE,
            startY + currentH,
            Paint().apply { color = COL_DATE_TINT }
        )

        val widths = listOf(C_DATE, C_MAD, C_MHR, C_EAD, C_EHR, C_NOTE)
        val rowCenterY = startY + currentH / 2f

        fun adColor(ad: String?): Int {
            if (ad == null) return COL_EMPTY
            val clean = ad.removePrefix("▲")
            val parts = clean.split("/")
            val s = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val d = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return when {
                s >= THRESHOLD_SYSTOLIC -> COL_HIGH_SYST
                d >= THRESHOLD_DIASTOLIC -> COL_HIGH_DIAS
                else -> COL_TEXT
            }
        }

        fun formatAd(ad: String?): String {
            if (ad == null) return "—"
            val p = ad.split("/")
            val s = p.getOrNull(0)?.toIntOrNull() ?: 0
            val d = p.getOrNull(1)?.toIntOrNull() ?: 0
            return if (s >= THRESHOLD_SYSTOLIC || d >= THRESHOLD_DIASTOLIC) "▲$ad" else ad
        }

        val values = listOf(
            dateFmt.format(Date(row.date)),
            formatAd(row.morningAd),
            row.morningHr?.toString() ?: "—",
            formatAd(row.eveningAd),
            row.eveningHr?.toString() ?: "—"
        )
        val times = listOf(null, row.morningTime, null, row.eveningTime, null)
        val colors = listOf(
            COL_TEXT,
            adColor(values[1]),
            if (row.morningHr == null) COL_EMPTY else COL_TEXT,
            adColor(values[3]),
            if (row.eveningHr == null) COL_EMPTY else COL_TEXT
        )
        val isBold = listOf(false, true, false, true, false)

        var x = MARGIN_LEFT
        for (i in 0..4) {
            val tp = paint(9f, colors[i], bold = isBold[i])
            var txt = values[i]
            val maxW = widths[i] - 4f
            if (tp.measureText(txt) > maxW) {
                while (txt.isNotEmpty() && tp.measureText("$txt…") > maxW) {
                    txt = txt.dropLast(1)
                }
                txt = "$txt…"
            }

            if (i in 1..4 && times[i] != null && values[i] != "—") {
                c.drawText(
                    txt,
                    x + (widths[i] - tp.measureText(txt)) / 2f,
                    rowCenterY - 4f,
                    tp
                )
                val tTp = paint(7f, COL_MUTED)
                c.drawText(
                    times[i]!!,
                    x + (widths[i] - tTp.measureText(times[i]!!)) / 2f,
                    rowCenterY + 6f,
                    tTp
                )
            } else {
                if (i == 0) {
                    val dowFmt = SimpleDateFormat("EEE", Locale("ru"))
                    val dow = dowFmt.format(Date(row.date)).replaceFirstChar { it.uppercase() }
                    val dateTp = paint(9f, COL_TEXT)
                    val dowTp = paint(7f, COL_MUTED)
                    val dateX = x + (widths[i] - dateTp.measureText(txt)) / 2f
                    val dowX = x + (widths[i] - dowTp.measureText(dow)) / 2f
                    c.drawText(txt, dateX, rowCenterY - 3f, dateTp)
                    c.drawText(dow, dowX, rowCenterY + 7f, dowTp)
                } else {
                    val yPos = rowCenterY + 4.5f
                    val xPos = x + (widths[i] - tp.measureText(txt)) / 2f
                    c.drawText(txt, xPos, yPos, tp)
                }
            }
            x += widths[i]
        }

        val eveningStart = MARGIN_LEFT + C_DATE + C_MAD + C_MHR
        val noteStart = MARGIN_LEFT + C_DATE + C_MAD + C_MHR + C_EAD + C_EHR
        val divColor = Color.argb(38, 0, 0, 0)

        drawLine(c, eveningStart, startY, eveningStart, startY + currentH, divColor, 1f)
        drawLine(c, noteStart, startY, noteStart, startY + currentH, divColor, 1f)

        val noteLines = mutableListOf<String>()
        if (!row.note.isNullOrBlank()) noteLines.add(row.note)
        if (!row.medication.isNullOrBlank()) noteLines.add("Доп. препарат: ${row.medication}")
        if (!row.wellbeing.isNullOrBlank()) noteLines.add("Самочувствие: ${row.wellbeing}")

        val lineH = 9f
        val totalNoteH = noteLines.size * lineH
        val noteStartY = startY + (currentH - totalNoteH) / 2f + 7.5f

        noteLines.forEachIndexed { idx, txt ->
            val lY = noteStartY + (idx * lineH)
            val tp = if (idx == 0 && !row.note.isNullOrBlank()) paint(9f, COL_MUTED) else paint(7f, COL_MUTED)
            var t = txt
            val maxW = widths[5] - 16f
            if (tp.measureText(t) > maxW) {
                while (t.isNotEmpty() && tp.measureText("$t…") > maxW) {
                    t = t.dropLast(1)
                }
                t = "$t…"
            }
            c.drawText(t, noteStart + 10f, lY, tp)
        }

        drawLine(
            c,
            MARGIN_LEFT,
            startY + currentH,
            MARGIN_LEFT + workingWidth,
            startY + currentH,
            COL_LINE,
            0.3f
        )
        return startY + currentH
    }

    private fun drawSummary(
        c: Canvas,
        p: PatientInfo,
        rows: List<MeasurementRow>,
        y: Float,
        isRu: Boolean
    ) {
        val paintLabel = paint(8f, COL_TEXT)
        val paintMuted = paint(7.5f, COL_MUTED)
        val paintHead = paint(7.5f, COL_MUTED)

        fun parse(ad: String?): Pair<Int, Int>? {
            if (ad == null || ad == "—") return null
            val clean = ad.removePrefix("▲")
            val parts = clean.split("/")
            return if (parts.size >= 2) {
                val s = parts[0].toIntOrNull()
                val d = parts[1].toIntOrNull()
                if (s != null && d != null) s to d else null
            } else null
        }

        val mS = rows.mapNotNull { parse(it.morningAd)?.first }
        val mD = rows.mapNotNull { parse(it.morningAd)?.second }
        val mP = rows.mapNotNull { it.morningHr }
        val eS = rows.mapNotNull { parse(it.eveningAd)?.first }
        val eD = rows.mapNotNull { parse(it.eveningAd)?.second }
        val eP = rows.mapNotNull { it.eveningHr }

        val avgMS = if (mS.isNotEmpty()) mS.average().toInt().toString() else "—"
        val avgMD = if (mD.isNotEmpty()) mD.average().toInt().toString() else "—"
        val avgMP = if (mP.isNotEmpty()) mP.average().toInt().toString() else "—"
        val avgES = if (eS.isNotEmpty()) eS.average().toInt().toString() else "—"
        val avgED = if (eD.isNotEmpty()) eD.average().toInt().toString() else "—"
        val avgEP = if (eP.isNotEmpty()) eP.average().toInt().toString() else "—"

        val avgS = p.avgSystolic.toString()
        val avgD = p.avgDiastolic.toString()
        val avgP = p.avgHr.toString()

        val lineH = 14f
        var currentY = y

        c.drawText(
            if (isRu) "Средние значения за период наблюдения" else "Summary for the observation period",
            MARGIN_LEFT,
            currentY,
            paint(8.5f, COL_TEXT, bold = true)
        )

        val xM = MARGIN_LEFT + 245f
        val xE = MARGIN_LEFT + 325f
        val xT = MARGIN_LEFT + 405f

        val hM = if (isRu) "Утро" else "Morning"
        val hE = if (isRu) "Вечер" else "Evening"
        val hT = if (isRu) "За период" else "Overall"

        val y0 = currentY + lineH
        c.drawText(hM, xM - paintHead.measureText(hM) / 2f, y0, paintHead)
        c.drawText(hE, xE - paintHead.measureText(hE) / 2f, y0, paintHead)
        c.drawText(hT, xT - paintHead.measureText(hT) / 2f, y0, paintHead)

        val r1 = if (isRu) "Сист. АД, мм рт.ст." else "Systolic BP, mmHg"
        val r2 = if (isRu) "Диаст. АД, мм рт.ст." else "Diastolic BP, mmHg"
        val r3 = if (isRu) "Пульс, уд/мин" else "Pulse, bpm"

        val y1 = y0 + lineH
        val y2 = y1 + lineH
        val y3 = y2 + lineH

        fun drawDataRow(
            rowY: Float,
            label: String,
            vM: String,
            vE: String,
            vT: String,
            vColor: Int = COL_TEXT
        ) {
            c.drawText(label, MARGIN_LEFT, rowY, paintLabel)
            val vp = paint(9f, vColor, bold = true)
            c.drawText(vM, xM - vp.measureText(vM) / 2f, rowY, vp)
            c.drawText(vE, xE - vp.measureText(vE) / 2f, rowY, vp)
            c.drawText(vT, xT - vp.measureText(vT) / 2f, rowY, vp)
        }

        val sColor = if (p.avgSystolic >= THRESHOLD_SYSTOLIC) COL_HIGH_SYST else COL_TEXT
        val dColor = if (p.avgDiastolic >= THRESHOLD_DIASTOLIC) COL_HIGH_DIAS else COL_TEXT

        drawDataRow(y1, r1, avgMS, avgES, avgS, sColor)
        drawDataRow(y2, r2, avgMD, avgED, avgD, dColor)
        drawDataRow(y3, r3, avgMP, avgEP, avgP)

        currentY = y3 + lineH + 4f

        val pulsePress = p.avgSystolic - p.avgDiastolic
        c.drawText(
            if (isRu) "Пульсовое давление: $pulsePress мм рт.ст." else "Pulse pressure: $pulsePress mmHg",
            MARGIN_LEFT,
            currentY,
            paintMuted
        )

        var elevated = 0
        rows.forEach { row ->
            parse(row.morningAd)?.let {
                if (it.first >= THRESHOLD_SYSTOLIC || it.second >= THRESHOLD_DIASTOLIC) elevated++
            }
            parse(row.eveningAd)?.let {
                if (it.first >= THRESHOLD_SYSTOLIC || it.second >= THRESHOLD_DIASTOLIC) elevated++
            }
        }

        val totalM = (mS.size + eS.size).coerceAtLeast(1)
        val percent = (elevated * 100) / totalM
        currentY += lineH

        val eTxt = if (isRu) {
            "Измерений выше справочного порога домашнего АД (≥ $THRESHOLD_SYSTOLIC и/или ≥ $THRESHOLD_DIASTOLIC): "
        } else {
            "Readings above the home BP reference threshold (≥ $THRESHOLD_SYSTOLIC and/or ≥ $THRESHOLD_DIASTOLIC): "
        }
        val eVal = if (isRu) "$elevated из $totalM ($percent%)" else "$elevated of $totalM ($percent%)"
        c.drawText(eTxt, MARGIN_LEFT, currentY, paintLabel)
        c.drawText(
            eVal,
            MARGIN_LEFT + paintLabel.measureText(eTxt),
            currentY,
            paint(8f, COL_TEXT, bold = true)
        )

        currentY += lineH + 4f
        c.drawText(if (isRu) "Обозначения:" else "Legend:", MARGIN_LEFT, currentY, paintMuted)

        currentY += lineH
        val dotR = 2.5f

        c.drawCircle(
            MARGIN_LEFT + dotR + 1f,
            currentY - dotR - 1f,
            dotR,
            Paint().apply { color = COL_HIGH_SYST; isAntiAlias = true }
        )
        val sysTxt = if (isRu) {
            "— систола ≥ $THRESHOLD_SYSTOLIC мм рт.ст."
        } else {
            "— systolic BP ≥ $THRESHOLD_SYSTOLIC mmHg"
        }
        c.drawText(sysTxt, MARGIN_LEFT + dotR * 2 + 5f, currentY, paintMuted)

        val sysBlockW = dotR * 2 + 5f + paint(7.5f, COL_MUTED).measureText(sysTxt) + 16f
        val dot2X = MARGIN_LEFT + sysBlockW + dotR + 1f
        c.drawCircle(
            dot2X,
            currentY - dotR - 1f,
            dotR,
            Paint().apply { color = COL_HIGH_DIAS; isAntiAlias = true }
        )
        val diaTxt = if (isRu) {
            "— диастола ≥ $THRESHOLD_DIASTOLIC мм рт.ст."
        } else {
            "— diastolic BP ≥ $THRESHOLD_DIASTOLIC mmHg"
        }
        c.drawText(diaTxt, dot2X + dotR + 4f, currentY, paintMuted)

        currentY += lineH

        val footTxt = if (isRu) {
            "* Для домашнего мониторинга в отчёте использован справочный порог ≥135/85 мм рт.ст. в соответствии с клиническими рекомендациями по артериальной гипертензии (РФ, 2024) и рекомендациями ESC (2024)."
        } else {
            "* For home blood pressure monitoring, this report uses the reference threshold of ≥135/85 mmHg in accordance with the 2024 Russian hypertension guidelines and the 2024 ESC guidelines."
        }
        currentY += drawWrappedText(
            canvas = c,
            text = footTxt,
            x = MARGIN_LEFT,
            y = currentY,
            width = workingWidth,
            textPaint = textPaint(6.5f, COL_MUTED)
        ) + 4f

        val totalAllMeasurements = rows.count { !it.morningAd.isNullOrEmpty() } +
            rows.count { !it.eveningAd.isNullOrEmpty() }
        val totalOutOfRange = rows.sumOf { it.outOfRangeCount }
        val totalLabel = if (isRu) {
            "Всего замеров в таблице: $totalAllMeasurements" +
                if (totalOutOfRange > 0) "   (вне диапазона утро/вечер: $totalOutOfRange — не вошли в таблицу)" else ""
        } else {
            "Total readings in table: $totalAllMeasurements" +
                if (totalOutOfRange > 0) "   (out of morning/evening window: $totalOutOfRange — excluded)" else ""
        }
        currentY += drawWrappedText(
            canvas = c,
            text = totalLabel,
            x = MARGIN_LEFT,
            y = currentY,
            width = workingWidth,
            textPaint = textPaint(6.5f, COL_MUTED)
        ) + 4f

        val disclaimer = if (isRu) {
            "Документ сформирован на основе данных, введённых пользователем, и предназначен для передачи лечащему врачу. Отчёт не является медицинским заключением и не предназначен для самостоятельной постановки диагноза или выбора лечения."
        } else {
            "This document is generated from user-entered data and is intended for review by the treating physician. It is not a medical conclusion and is not intended for self-diagnosis or treatment decisions."
        }
        drawWrappedText(
            canvas = c,
            text = disclaimer,
            x = MARGIN_LEFT,
            y = currentY,
            width = workingWidth,
            textPaint = textPaint(6.5f, COL_MUTED)
        )
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        textPaint: TextPaint
    ): Float {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y - textPaint.textSize)
        layout.draw(canvas)
        canvas.restore()

        return layout.height.toFloat()
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean = false) = TextPaint().apply {
        textSize = size
        this.color = color
        typeface = if (bold) typefaceBold ?: Typeface.DEFAULT_BOLD else typefaceRegular ?: Typeface.DEFAULT
        isAntiAlias = true
    }

    private fun drawPageFooter(c: Canvas, num: Int) {
        val txt = "CardioLog  ·  $num"
        val fp = paint(7f, COL_MUTED)
        drawLine(c, MARGIN_LEFT, PH - 28f, MARGIN_LEFT + workingWidth, PH - 28f, COL_LINE, 0.5f)
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
        c.drawLine(
            x1,
            y1,
            x2,
            y2,
            Paint().apply {
                this.color = color
                strokeWidth = w
                style = Paint.Style.STROKE
            }
        )
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint().apply {
        textSize = size
        this.color = color
        typeface = if (bold) typefaceBold ?: Typeface.DEFAULT_BOLD else typefaceRegular ?: Typeface.DEFAULT
        isAntiAlias = true
    }

    private fun transliterate(input: String): String {
        val map = mapOf(
            'А' to "A", 'Б' to "B", 'В' to "V", 'Г' to "G", 'Д' to "D", 'Е' to "E",
            'Ё' to "E", 'Ж' to "Zh", 'З' to "Z", 'И' to "I", 'Й' to "Y", 'К' to "K",
            'Л' to "L", 'М' to "M", 'Н' to "N", 'О' to "O", 'П' to "P", 'Р' to "R",
            'С' to "S", 'Т' to "T", 'У' to "U", 'Ф' to "F", 'Х' to "Kh", 'Ц' to "Ts",
            'Ч' to "Ch", 'Ш' to "Sh", 'Щ' to "Sch", 'Ъ' to "", 'Ы' to "Y", 'Ь' to "",
            'Э' to "E", 'Ю' to "Yu", 'Я' to "Ya",
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e",
            'ё' to "e", 'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k",
            'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r",
            'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts",
            'ч' to "ch", 'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "",
            'э' to "e", 'ю' to "yu", 'я' to "ya"
        )
        return input.map { map[it] ?: it.toString() }.joinToString("")
    }

    private data class Card(val lbl: String, val value: String, val color: Int)
}