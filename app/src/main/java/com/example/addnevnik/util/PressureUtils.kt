package com.example.addnevnik.util

import androidx.compose.ui.graphics.Color

/**
 * Нормы артериального давления по классификации ВОЗ (WHO).
 * 
 * Оптимальное: < 120 / < 80
 * Нормальное: 120-129 / 80-84
 * Высокое нормальное: 130-139 / 85-89
 * Гипертония 1 степени: 140-159 / 90-99
 * Гипертония 2 степени: 160-179 / 100-109
 * Гипертония 3 степени: >= 180 / >= 110
 */
object PressureUtils {
    
    fun getPressureColor(systolic: Int, diastolic: Int): Color {
        return when {
            systolic >= 140 || diastolic >= 90 -> Color(0xFFD32F2F) // Красный (Гипертония)
            systolic >= 130 || diastolic >= 85 -> Color(0xFFFBC02D) // Желтый (Высокое нормальное)
            else -> Color(0xFF388E3C) // Зеленый (Норма)
        }
    }
}
