package com.example.addnevnik.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    val id: Int = 0,
    val name: String = "Иван Иванов",
    val gender: String = "Мужской",
    val birthDate: String = "01.01.1980",
    val status: String = "",
    val isPremium: Boolean = false,
    val promoCode: String? = null,
    val premiumActivatedAt: Long? = null
)
