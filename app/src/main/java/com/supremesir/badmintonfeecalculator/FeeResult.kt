package com.supremesir.badmintonfeecalculator

data class FeeResult(
    val maleFee: Double,
    val femaleFee: Double,
    val absentFee: Double = 0.0
)
