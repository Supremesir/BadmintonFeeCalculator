package com.supremesir.badmintonfeecalculator

import org.junit.Test

import org.junit.Assert.*

class FeeCalculateText {
    @Test
    fun testFeeCalculate() {
        val courtFee = randomInt(200, 400).toDouble()
        val shuttlecockFee = randomInt(200, 400).toDouble()
        val man = randomInt(1)
        val woman = randomInt(1)
        val gugu = randomInt(0, 0)
        println("场地费:$courtFee, 羽毛球费:$shuttlecockFee, 男生:$man, 女生:$woman, 没有鸽子:${gugu <= 0}")
        testFeeCalculateReal(courtFee, shuttlecockFee, man, woman, gugu)
    }

    @Test
    fun testFeeCalculateWithAbsent() {
        val courtFee = randomInt(200, 400).toDouble()
        val shuttlecockFee = randomInt(200, 400).toDouble()
        val man = randomInt(1)
        val woman = randomInt(1)
        val gugu = randomInt(0, 5)
        println("场地费:$courtFee, 羽毛球费:$shuttlecockFee, 男生:$man, 女生:$woman, 鸽子:$gugu")
        testFeeCalculateReal(courtFee, shuttlecockFee, man, woman, gugu)
    }

    fun randomDouble(min: Int, max: Int): Double {
        return (min..max).random() + Math.random()
    }

    fun randomInt(min: Int, max: Int = 10): Int {
        return (min..max).random()
    }



    fun testFeeCalculateReal(
        courtFee: Double,
        shuttlecockFee: Double,
        man: Int,
        woman: Int,
        gugu: Int
    ) {
        val resultGugu =
            FeeCalculate.calculateWithAbsent(courtFee, shuttlecockFee, 5.0, man, woman, gugu)
        val manResultGugu = resultGugu[0]
        val womanResultGugu = resultGugu[1]
        val guguResultGugu = resultGugu[2]
        println("男生费用:$manResultGugu, 女生费用:$womanResultGugu, 鸽子费用:$guguResultGugu")
        println("鸽子费用正常:${(gugu > 0 && guguResultGugu > 0) || (gugu <= 0 && guguResultGugu <= 0)}")
        val expectFee = courtFee + shuttlecockFee
        val totalFee = manResultGugu * man + womanResultGugu * woman + guguResultGugu * gugu
        println("没亏本:${totalFee >= expectFee}, 多收的钱:${totalFee - expectFee}")

        val resultOld = FeeCalculate.calculate(courtFee, shuttlecockFee, 5.0, man, woman)
        val manResult = resultOld[0]
        val womanResult = resultOld[1]
        println("旧版男生费用:$manResultGugu, 旧版女生费用:$womanResultGugu")
        println("新旧费用相等:${manResult == manResultGugu && womanResult == womanResultGugu}")
    }
}