package com.supremesir.badmintonfeecalculator

import android.view.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargemap.compose.numberpicker.NumberPicker
import com.holix.android.bottomsheetdialog.compose.BottomSheetDialog
import com.holix.android.bottomsheetdialog.compose.BottomSheetDialogProperties
import com.supremesir.badmintonfeecalculator.picker.InfiniteNumberPicker
import kotlinx.coroutines.delay

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
    val showDialog = remember { mutableStateOf(false) }

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
                modifier = Modifier.padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
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
                modifier = Modifier.padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
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
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { showDialog.value = true }) {
                Text(resources.getString(R.string.other_fee_label))
                ShowBottomSheetDialog(showDialog, maleCost, femaleCost)
            }
        }
    }
}

@Composable
fun ShowBottomSheetDialog(
    show: MutableState<Boolean>,
    maleFee: Double,
    femaleFee: Double
) {
    var malePicker by remember { mutableIntStateOf(3) }
    var femalePicker by remember { mutableIntStateOf(0) }
    var customFee by remember { mutableDoubleStateOf(0.0) }
    val context = LocalContext.current
    if (show.value) {
        BottomSheetDialog(
            onDismissRequest = {
                show.value = false
            },
            properties = BottomSheetDialogProperties()
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    repeat(6) {
                        val pair = getNumCombination(it)
                        val fee = calculateFeeCombination(pair, maleFee, femaleFee)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
                                .clickable { copyOnClick(context, fee.toString()) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.common_num_fee_label, pair.first, pair.second),
                                modifier = Modifier
                                    .weight(1F)
                                    .padding(start = 16.dp),
                                fontSize = 16.sp
                            )
                            Text(
                                text = "$fee",
                                modifier = Modifier.padding(start = 0.dp, end = 16.dp),
                                fontSize = 21.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp)
                            .clickable { copyOnClick(context, customFee.toString()) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 自动计算自定义男女时的费用
                        LaunchedEffect(malePicker, femalePicker) {
                            delay(100) // 等待其他状态变量的变化稳定下来
                            customFee = calculateFeeCombination(
                                Pair(malePicker, femalePicker),
                                maleFee,
                                femaleFee
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1F)
                        ) {
                            NumberPicker(
                                dividersColor = MaterialTheme.colorScheme.primary,
                                value = malePicker,
                                range = 0..10,
                                onValueChange = {
                                    malePicker = it
                                },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = stringResource(id = R.string.man),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 4.dp, end = 4.dp),
                                fontSize = 16.sp
                            )
                            NumberPicker(
                                dividersColor = MaterialTheme.colorScheme.primary,
                                value = femalePicker,
                                range = 0..10,
                                onValueChange = {
                                    femalePicker = it
                                },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = stringResource(id = R.string.woman),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 4.dp, end = 4.dp),
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "$customFee",
                            modifier = Modifier.padding(start = 0.dp, end = 16.dp),
                            fontSize = 21.sp
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

fun getNumCombination(index: Int): Pair<Int, Int> {
    return listOf(
        Pair(0, 2),
        Pair(1, 1),
        Pair(1, 2),
        Pair(2, 0),
        Pair(2, 1),
        Pair(2, 2),
        Pair(3, 0),
        Pair(3, 1),
        Pair(3, 2),
    )[index]
}

fun calculateFeeCombination(
    numPair: Pair<Int, Int>,
    maleFee: Double,
    femaleFee: Double
): Double {
    return FeeCalculate.calculateOther(numPair.first, numPair.second, maleFee, femaleFee)
}