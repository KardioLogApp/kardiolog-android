package com.example.addnevnik.domain

import androidx.compose.ui.graphics.Color

sealed class BpCategory(val color: Color) {
    data object Low : BpCategory(Color.Blue)
    data object Optimal : BpCategory(Color.Green)
    data object Normal : BpCategory(Color(0xFF90EE90))
    data object Elevated : BpCategory(Color.Yellow)
    data object High : BpCategory(Color(0xFFFFA500))
    data object VeryHigh : BpCategory(Color(0xFFFF5722))
    data object CriticallyHigh : BpCategory(Color.Red)
    data object Emergency : BpCategory(Color(0xFF8B0000))
}

object BpClassifier {
    fun classify(systolic: Int, diastolic: Int): BpCategory {
        return when {
            systolic >= 180 && diastolic >= 120 -> BpCategory.Emergency
            systolic >= 180 || diastolic >= 110 -> BpCategory.CriticallyHigh
            systolic >= 160 || diastolic >= 100 -> BpCategory.VeryHigh
            systolic >= 140 || diastolic >= 90 -> BpCategory.High
            systolic >= 130 || diastolic >= 85 -> BpCategory.Elevated
            systolic >= 120 || diastolic >= 80 -> BpCategory.Normal
            systolic < 90 || diastolic < 60 -> BpCategory.Low
            else -> BpCategory.Optimal
        }
    }
}
