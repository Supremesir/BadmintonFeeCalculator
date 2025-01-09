package com.supremesir.badmintonfeecalculator

import org.junit.Test

import org.junit.Assert.*

class FeeCalculateText {
    @Test
    fun testFeeCalculate() {
        val courtFee = 270.0
        val shuttlecockFee = 300.0
        val man = 10
        val woman = 6
        val gugu = 0
        val result = FeeCalculate.calculate(courtFee, shuttlecockFee, 5.0, man, woman, gugu)
        val manResult = result[0]
        val womanResult = result[1]
        val guguResult = result[2]
        println("男生费用:$manResult, 女生费用:$womanResult, 鸽子费用:$guguResult")
        val expectFee = courtFee + shuttlecockFee
        val totalFee = manResult * man + womanResult * woman + guguResult * gugu
        println("没亏本:${totalFee >= expectFee}, 多收的钱:${totalFee - expectFee}")
    }
}