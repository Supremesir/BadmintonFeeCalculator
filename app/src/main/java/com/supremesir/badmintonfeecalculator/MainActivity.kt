package com.supremesir.badmintonfeecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.supremesir.badmintonfeecalculator.ui.theme.BadmintonFeeCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BadmintonFeeCalculatorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var courtFee by remember { mutableDoubleStateOf(0.0) }
    var badmintonFee by remember { mutableDoubleStateOf(0.0) }
    var extraMaleFee by remember { mutableDoubleStateOf(5.0) }
    var maleCount by remember { mutableIntStateOf(0) }
    var femaleCount by remember { mutableIntStateOf(0) }
    var maleCost by remember { mutableDoubleStateOf(0.0) }
    var femaleCost by remember { mutableDoubleStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DoubleInputField(
            value = courtFee,
            onValueChange = { courtFee = it },
            label = "请输入场地费",
        )
        Spacer(modifier = Modifier.height(16.dp))
        DoubleInputField(
            value = badmintonFee,
            onValueChange = { badmintonFee = it },
            label = "请输入羽毛球费"
        )
        Spacer(modifier = Modifier.height(16.dp))
        DoubleInputField(
            value = extraMaleFee,
            onValueChange = { extraMaleFee = it },
            label = "请输入男生比女生多的费用"
        )
        Spacer(modifier = Modifier.height(16.dp))
        IntInputField(
            value = maleCount,
            onValueChange = { maleCount = it },
            label = "请输入男生人数"
        )
        Spacer(modifier = Modifier.height(16.dp))
        IntInputField(
            value = femaleCount,
            onValueChange = { femaleCount = it },
            label ="请输入女生人数"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            calculate(courtFee, badmintonFee, extraMaleFee, maleCount, femaleCount).run {
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

@Composable
fun IntInputField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
) {
    val valueString = value.toString()
    OutlinedTextField(
        value = valueString,
        onValueChange = { newValue ->
            // 过滤非数字
            val filteredValue = newValue.filter { it.isDigit() }
            onValueChange(filteredValue.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation = DoubleVisualTransformation(),
    )
}

@Composable
fun DoubleInputField(
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String,
) {
    val valueString = value.toString()
    OutlinedTextField(
        value = valueString,
        onValueChange = { newValue ->
            // 过滤非数字和小数点字符
            val filteredValue = newValue.filter { it.isDigit() || it == '.' }
            // 只允许一个小数点
            if (filteredValue.count { it == '.' } <= 1) {
                onValueChange(filteredValue.toDoubleOrNull() ?: 0.0)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation = DoubleVisualTransformation(),
    )
}

fun calculate(
    courtFee: Double,
    badmintonFee: Double,
    extraMaleFee: Double,
    maleCount: Int,
    femaleCount: Int
): FeeResult {
    FeeCalculate.calculate(courtFee, badmintonFee, extraMaleFee, maleCount, femaleCount).run {
        return FeeResult(this[0], this[1])
    }
}

// 自定义的VisualTransformation,用于限制输入框仅能显示数字和小数点
class DoubleVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val filteredText = text.text.filter { it.isDigit() || it == '.' }
        return TransformedText(AnnotatedString(filteredText), offsetTranslator)
    }

    private val offsetTranslator = OffsetMapping.Identity
}
