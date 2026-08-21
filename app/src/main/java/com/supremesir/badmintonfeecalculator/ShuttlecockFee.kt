package com.supremesir.badmintonfeecalculator

import kotlin.math.round

const val SHUTTLECOCKS_PER_TUBE = 12
val SHUTTLECOCK_COUNT_RANGE = 1..36

data class ShuttlecockLine(
    val id: Long,
    val tubePrice: String = "",
    val usedCount: Int = 12
)

fun shuttlecockLineFee(tubePrice: Double, usedCount: Int): Double {
    if (usedCount <= 0) return 0.0
    return tubePrice / SHUTTLECOCKS_PER_TUBE * usedCount
}

fun shuttlecockFeeTotal(lines: List<ShuttlecockLine>): Double {
    return lines.sumOf { line ->
        shuttlecockLineFee(line.tubePrice.safeToDouble(), line.usedCount)
    }
}

fun formatShuttlecockFee(total: Double): String {
    val rounded = round(total * 10.0) / 10.0
    if (rounded == 0.0) return ""
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", rounded)
    }
}

fun List<ShuttlecockLine>.nextLineId(): Long = (maxOfOrNull { it.id } ?: 0L) + 1L
