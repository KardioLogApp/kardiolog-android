package com.example.addnevnik.util

import android.util.Log
import com.google.mlkit.vision.text.Text

object BpOcrParser {

    data class OcrResult(
        val systolic: Int?,
        val diastolic: Int?,
        val pulse: Int?
    )

    // Numbers that are definitely not BP/pulse
    private val IGNORE_RANGES = listOf(2000..2099, 1..9)

    // ── Entry point for analyzer (uses bounding box positions) ────────────────
    fun parseBlocks(blocks: List<Text.TextBlock>): OcrResult {
        // Sort blocks top → bottom by their vertical center
        val sortedBlocks = blocks.sortedBy { block ->
            block.boundingBox?.let { it.top + it.height() / 2 } ?: Int.MAX_VALUE
        }

        val rawText = sortedBlocks.joinToString("\n") { it.text }
        Log.d("BpOcrParser", "RAW blocks sorted:\n$rawText")

        // First try slash pattern (some monitors show "120/80")
        val slashResult = trySlashPattern(rawText)
        if (slashResult != null) return slashResult

        // Extract numbers in top-to-bottom order
        val numbers = sortedBlocks
            .flatMap { block -> block.lines.sortedBy { it.boundingBox?.top ?: 0 } }
            .flatMap { line -> extractNumbers(normalizeOcrText(line.text)) }
            .filter { n -> IGNORE_RANGES.none { n in it } }

        Log.d("BpOcrParser", "Numbers top→bottom: $numbers")
        return parseFromOrderedNumbers(numbers)
    }

    // ── Fallback for plain text input (manual/test) ────────────────────────────
    fun parse(rawText: String): OcrResult {
        val normalized = normalizeOcrText(rawText)
        val slashResult = trySlashPattern(normalized)
        if (slashResult != null) return slashResult

        val numbers = extractNumbers(normalized).filter { n -> IGNORE_RANGES.none { n in it } }
        return parseFromOrderedNumbers(numbers)
    }

    // ── Slash pattern: "120/80" ────────────────────────────────────────────────
    private fun trySlashPattern(text: String): OcrResult? {
        val textNoTime = text.replace(Regex("""\d{1,2}[:.]\d{2}"""), "")
        val match = Regex("""(\d{2,3})\s*[/\\]\s*(\d{2,3})""").find(textNoTime) ?: return null
        val sys = match.groupValues[1].toIntOrNull() ?: return null
        val dia = match.groupValues[2].toIntOrNull() ?: return null
        if (sys !in 90..210 || dia !in 50..130 || sys <= dia) return null
        val remaining = textNoTime.removeRange(match.range)
        val pulse = extractNumbers(remaining).firstOrNull { it in 40..180 }
        Log.d("BpOcrParser", "SLASH: $sys/$dia pulse=$pulse")
        return OcrResult(sys, dia, pulse)
    }

    // ── Parse three values from top-to-bottom ordered number list ─────────────
    // Tonometer layout: SYS (top, largest) → DIA (middle) → PULSE (bottom)
    private fun parseFromOrderedNumbers(numbers: List<Int>): OcrResult {
        // Systolic: first number in 90–210
        val sysIdx = numbers.indexOfFirst { it in 90..210 }
        val systolic = if (sysIdx >= 0) numbers[sysIdx] else null

        // Diastolic: next number after systolic in 50–130, must be < systolic
        val diastolic = if (systolic != null && sysIdx >= 0) {
            numbers.drop(sysIdx + 1).firstOrNull { it in 50..130 && it < systolic }
        } else null

        // Pulse: next number after diastolic in 40–180
        val diaIdx = if (diastolic != null) numbers.indexOf(diastolic) else -1
        val pulse = if (diaIdx >= 0) {
            numbers.drop(diaIdx + 1).firstOrNull { it in 40..180 }
        } else {
            // fallback: any 40-180 number that isn't systolic
            numbers.firstOrNull { it in 40..180 && it != systolic }
        }

        Log.d("BpOcrParser", "Result: sys=$systolic dia=$diastolic pulse=$pulse")
        return OcrResult(systolic, diastolic, pulse)
    }

    // ── LCD OCR normalization ──────────────────────────────────────────────────
    private fun normalizeOcrText(text: String): String = text
        .replace("О", "0").replace("о", "0")   // Cyrillic O → 0
        .replace(Regex("(?<!\\d)l(?!\\d)"), "1")  // l → 1 (not between digits)
        .replace(Regex("(?<!\\d)I(?!\\d)"), "1")  // I → 1
        .replace("B", "8")
        .replace("b", "6")
        .replace("S", "5")
        .replace(Regex("""\d{1,2}[:.]\d{2}"""), "")  // remove time HH:MM

    private fun extractNumbers(text: String): List<Int> =
        Regex("""\d{2,3}""").findAll(text).mapNotNull { it.value.toIntOrNull() }.toList()
}
