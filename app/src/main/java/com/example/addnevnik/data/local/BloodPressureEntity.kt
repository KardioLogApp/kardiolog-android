package com.example.addnevnik.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_pressure")
data class BloodPressureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val timestamp_ms: Long,
    val tag: String? = null,
    val isPrimary: Boolean = false,
    val isManual: Boolean = false
)
