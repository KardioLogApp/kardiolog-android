package com.example.addnevnik.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_notes")
data class DailyNoteEntity(
    @PrimaryKey val dateKey: String,   // format "yyyy-MM-dd", e.g. "2026-05-13"
    val medication: String = "",       // free text, e.g. "Лозартан 50мг"
    val wellbeing: String = ""         // free text, e.g. "удовлетворительное"
)
