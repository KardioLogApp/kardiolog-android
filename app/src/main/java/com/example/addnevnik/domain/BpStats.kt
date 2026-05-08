package com.example.addnevnik.domain

data class BpStats(
    val avgSystolic7: Int?, val avgDiastolic7: Int?, val avgPulse7: Int?,
    val avgSystolic14: Int?, val avgDiastolic14: Int?, val avgPulse14: Int?,
    val avgSystolic30: Int?, val avgDiastolic30: Int?, val avgPulse30: Int?,
    val totalCount: Int
)
