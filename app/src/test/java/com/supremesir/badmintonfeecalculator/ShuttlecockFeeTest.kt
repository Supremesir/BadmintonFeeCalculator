package com.supremesir.badmintonfeecalculator

import org.junit.Assert.assertEquals
import org.junit.Test

class ShuttlecockFeeTest {
    @Test
    fun halfTubeAtEightyIsForty() {
        assertEquals(40.0, shuttlecockLineFee(80.0, 6), 1e-9)
    }

    @Test
    fun fullTubeEqualsTubePrice() {
        assertEquals(100.0, shuttlecockLineFee(100.0, 12), 1e-9)
    }

    @Test
    fun multipleLinesSum() {
        val total = shuttlecockFeeTotal(
            listOf(
                ShuttlecockLine(1, "80", 6),
                ShuttlecockLine(2, "100", 12)
            )
        )
        assertEquals(140.0, total, 1e-9)
    }

    @Test
    fun blankPriceCountsAsZero() {
        assertEquals(0.0, shuttlecockFeeTotal(listOf(ShuttlecockLine(1, "", 12))), 1e-9)
    }

    @Test
    fun formatDropsTrailingZero() {
        assertEquals("40", formatShuttlecockFee(40.0))
        assertEquals("49.6", formatShuttlecockFee(85.0 / 12.0 * 7.0))
        assertEquals("", formatShuttlecockFee(0.0))
    }
}
