package com.example.addnevnik.domain

import androidx.compose.ui.graphics.Color

/**
 * Классификация уровней артериального давления для UI и отчетов.
 *
 * Основана на привычной для клинической практики в РФ шкале:
 * - Оптимальное:        <120 и <80 мм рт.ст.
 * - Нормальное:         120–129 и/или 80–84 мм рт.ст.
 * - Высокое нормальное: 130–139 и/или 85–89 мм рт.ст.
 * - АГ 1 степени:       140–159 и/или 90–99 мм рт.ст.
 * - АГ 2 степени:       160–179 и/или 100–109 мм рт.ст.
 * - АГ 3 степени:       >=180 и/или >=110 мм рт.ст.
 *
 * ВАЖНО:
 * - Класс отражает диапазон измеренного АД, а не диагноз.
 * - При разных диапазонах САД и ДАД выбирается более высокая категория.
 * - Категория Low добавлена как пользовательская сервисная метка для низкого АД
 *   и не является частью стандартной шкалы артериальной гипертензии.
 */
sealed class BpCategory(
    val color: Color,
    val shortLabel: String,
    val reportLabel: String
) {
    data object Low : BpCategory(
        color = Color(0xFF1565C0),
        shortLabel = "Низкое АД",
        reportLabel = "значение в диапазоне низкого артериального давления"
    )

    data object Optimal : BpCategory(
        color = Color(0xFF2E7D32),
        shortLabel = "Оптимальное",
        reportLabel = "значение в диапазоне оптимального артериального давления"
    )

    data object Normal : BpCategory(
        color = Color(0xFF558B2F),
        shortLabel = "Нормальное",
        reportLabel = "значение в диапазоне нормального артериального давления"
    )

    data object HighNormal : BpCategory(
        color = Color(0xFF8D6E00),
        shortLabel = "Высокое нормальное",
        reportLabel = "значение в диапазоне высокого нормального артериального давления"
    )

    data object Grade1 : BpCategory(
        color = Color(0xFFEF6C00),
        shortLabel = "АГ 1 степени",
        reportLabel = "значение в диапазоне артериального давления, соответствующего АГ 1 степени"
    )

    data object Grade2 : BpCategory(
        color = Color(0xFFD84315),
        shortLabel = "АГ 2 степени",
        reportLabel = "значение в диапазоне артериального давления, соответствующего АГ 2 степени"
    )

    data object Grade3 : BpCategory(
        color = Color(0xFFC62828),
        shortLabel = "АГ 3 степени",
        reportLabel = "значение в диапазоне артериального давления, соответствующего АГ 3 степени"
    )
}

object BpClassifier {

    fun classify(systolic: Int, diastolic: Int): BpCategory {
        if (systolic < 90 || diastolic < 60) {
            return BpCategory.Low
        }

        return when {
            systolic >= 180 || diastolic >= 110 -> BpCategory.Grade3
            systolic >= 160 || diastolic >= 100 -> BpCategory.Grade2
            systolic >= 140 || diastolic >= 90 -> BpCategory.Grade1
            systolic >= 130 || diastolic >= 85 -> BpCategory.HighNormal
            systolic >= 120 || diastolic >= 80 -> BpCategory.Normal
            else -> BpCategory.Optimal
        }
    }

    fun shortLabel(category: BpCategory): String = category.shortLabel

    fun reportLabel(category: BpCategory): String = category.reportLabel
}