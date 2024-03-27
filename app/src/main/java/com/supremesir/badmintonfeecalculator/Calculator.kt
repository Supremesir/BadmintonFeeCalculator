package com.supremesir.badmintonfeecalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen() {
    var courtFee by remember { mutableStateOf("") }
    var badmintonFee by remember { mutableStateOf("") }
    var extraMaleFee by remember { mutableStateOf("5") }
    var maleCount by remember { mutableStateOf("") }
    var femaleCount by remember { mutableStateOf("") }
    var maleCost by remember { mutableDoubleStateOf(0.0) }
    var femaleCost by remember { mutableDoubleStateOf(0.0) }

    val context = LocalContext.current
    val resources = context.resources

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.surface,
                ),
                title = { Text(resources.getString(R.string.app_name)) }
            )
        },
        // 计算按钮
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    calculateString(
                        courtFee,
                        badmintonFee,
                        extraMaleFee,
                        maleCount,
                        femaleCount
                    ).run {
                        maleCost = this.maleFee
                        femaleCost = this.femaleFee
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.calculation),
                        contentDescription = resources.getString(R.string.calculate),
                        modifier = Modifier.size(40.dp)
                    )
                },
                text = { Text(text = resources.getString(R.string.calculate)) },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 内容输入区
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = courtFee,
                onValueChange = { courtFee = it },
                label = { Text(resources.getString(R.string.court_fee_label)) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = badmintonFee,
                onValueChange = { badmintonFee = it },
                label = { Text(resources.getString(R.string.shuttlecock_fee_label)) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = extraMaleFee,
                onValueChange = { extraMaleFee = it },
                label = { Text(resources.getString(R.string.extra_fee_label)) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = maleCount,
                onValueChange = { maleCount = it },
                label = { Text(resources.getString(R.string.male_count_label)) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = femaleCount,
                onValueChange = { femaleCount = it },
                label = { Text(resources.getString(R.string.female_count_label)) },
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 结果展示区
            ElevatedCard(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                onClick = { copyOnClick(context, maleCost.toString())},
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(resources.getString(R.string.male_fee_label))
                    OutlinedCard(
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = "$maleCost",
                            fontSize = 24.sp
                        )
                    }
                }
            }
            ElevatedCard(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                onClick = { copyOnClick(context, femaleCost.toString())},
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(resources.getString(R.string.female_fee_label))
                    OutlinedCard(
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = "$femaleCost",
                            fontSize = 24.sp
                        )
                    }

                }
            }
        }
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