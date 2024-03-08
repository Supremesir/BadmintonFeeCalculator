package com.supremesir.badmintonfeecalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CalculatorScreen() {
    var courtFee by remember { mutableStateOf("") }
    var badmintonFee by remember { mutableStateOf("") }
    var extraMaleFee by remember { mutableStateOf("") }
    var maleCount by remember { mutableStateOf("") }
    var femaleCount by remember { mutableStateOf("") }
    var maleCost by remember { mutableDoubleStateOf(0.0) }
    var femaleCost by remember { mutableDoubleStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = courtFee,
            onValueChange = { courtFee = it },
            label = { Text("请输入场地费") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = badmintonFee,
            onValueChange = { badmintonFee = it },
            label = { Text("请输入羽毛球费" ) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = extraMaleFee,
            onValueChange = { extraMaleFee = it },
            label = { Text("请输入男生比女生多的费用") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = maleCount,
            onValueChange = { maleCount = it },
            label = { Text("请输入男生人数" )},
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = femaleCount,
            onValueChange = { femaleCount = it },
            label = { Text("请输入女生人数") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            calculateString(courtFee, badmintonFee, extraMaleFee, maleCount, femaleCount).run {
                maleCost = this.maleFee
                femaleCost = this.femaleFee
            }
        }) {
            Text("计算")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("男生费用: $maleCost")
        Spacer(modifier = Modifier.height(8.dp))
        Text("女生费用: $femaleCost")
    }
}


fun calculateString(
    courtFee: String,
    badmintonFee: String,
    extraMaleFee: String,
    maleCount: String,
    femaleCount: String
): FeeResult {
    FeeCalculate.calculate(
        courtFee.safeToDouble(),
        badmintonFee.safeToDouble(),
        extraMaleFee.safeToDouble(),
        maleCount.safeToInt(),
        femaleCount.safeToInt()
    ).run {
        return FeeResult(this[0], this[1])
    }
}