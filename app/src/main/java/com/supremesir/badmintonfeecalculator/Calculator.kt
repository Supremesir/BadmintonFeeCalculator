package com.supremesir.badmintonfeecalculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidAtmosphere
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidBottomSheet
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidButton
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidGlassChip
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidInsetGroup
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidPanel
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidWheelPicker
import kotlinx.coroutines.delay

@Composable
fun CalculatorScreen(
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    var courtFee by remember { mutableStateOf("") }
    var badmintonFee by remember { mutableStateOf("") }
    var extraMaleFee by remember { mutableStateOf("5") }
    var maleCount by remember { mutableStateOf("") }
    var femaleCount by remember { mutableStateOf("") }
    var absentCount by remember { mutableStateOf("0") }
    var maleCost by remember { mutableDoubleStateOf(0.0) }
    var femaleCost by remember { mutableDoubleStateOf(0.0) }
    var absentCost by remember { mutableDoubleStateOf(0.0) }
    val showDialog = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val resources = context.resources
    val darkTheme = isSystemInDarkTheme()
    val contentColor = if (darkTheme) Color.White else Color(0xFF1B1B1F)
    val mutedColor = contentColor.copy(alpha = 0.62f)
    val glassSurface = if (darkTheme) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.20f)
    val accent = Color(0xFF0088FF)
    val backdrop = rememberLayerBackdrop()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = contentColor,
        unfocusedTextColor = contentColor,
        focusedLabelColor = mutedColor,
        unfocusedLabelColor = mutedColor,
        cursorColor = accent,
        focusedBorderColor = contentColor.copy(alpha = 0.35f),
        unfocusedBorderColor = contentColor.copy(alpha = 0.16f),
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent
    )

    Box(Modifier.fillMaxSize()) {
        LiquidAtmosphere(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            darkTheme = darkTheme
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = resources.getString(R.string.app_name),
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                color = contentColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )

            LiquidPanel(backdrop = backdrop, surfaceColor = glassSurface) {
                GlassFieldRow {
                    OutlinedTextField(
                        value = courtFee,
                        onValueChange = { courtFee = it },
                        label = { Text(resources.getString(R.string.court_fee_label)) },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = badmintonFee,
                        onValueChange = { badmintonFee = it },
                        label = { Text(resources.getString(R.string.shuttlecock_fee_label)) },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = extraMaleFee,
                    onValueChange = { extraMaleFee = it },
                    label = { Text(resources.getString(R.string.extra_fee_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                GlassFieldRow {
                    OutlinedTextField(
                        value = maleCount,
                        onValueChange = { maleCount = it },
                        label = { Text(resources.getString(R.string.male_count_label)) },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = femaleCount,
                        onValueChange = { femaleCount = it },
                        label = { Text(resources.getString(R.string.female_count_label)) },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = absentCount,
                    onValueChange = { absentCount = it },
                    label = { Text(resources.getString(R.string.absent_count_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true
                )
            }

            LiquidPanel(backdrop = backdrop, surfaceColor = glassSurface) {
                FeeRow(
                    label = resources.getString(R.string.male_fee_label),
                    value = maleCost,
                    contentColor = contentColor,
                    onClick = { copyOnClick(context, maleCost.toString()) }
                )
                Spacer(Modifier.height(12.dp))
                FeeRow(
                    label = resources.getString(R.string.female_fee_label),
                    value = femaleCost,
                    contentColor = contentColor,
                    onClick = { copyOnClick(context, femaleCost.toString()) }
                )
                if (absentCount.safeToInt() > 0) {
                    Spacer(Modifier.height(12.dp))
                    FeeRow(
                        label = resources.getString(R.string.absent_fee_label),
                        value = absentCost,
                        contentColor = contentColor,
                        onClick = { copyOnClick(context, absentCost.toString()) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LiquidButton(
                onClick = { showDialog.value = true },
                backdrop = backdrop,
                modifier = Modifier.weight(1f),
                surfaceColor = glassSurface
            ) {
                Text(
                    text = resources.getString(R.string.other_fee_label),
                    color = contentColor
                )
            }
            LiquidButton(
                onClick = {
                    calculateStringWithAbsent(
                        courtFee,
                        badmintonFee,
                        extraMaleFee,
                        maleCount,
                        femaleCount,
                        absentCount
                    ).run {
                        maleCost = this.maleFee
                        femaleCost = this.femaleFee
                        absentCost = this.absentFee
                    }
                },
                backdrop = backdrop,
                modifier = Modifier.weight(1f),
                tint = accent
            ) {
                Icon(
                    painter = painterResource(R.drawable.calculation),
                    contentDescription = resources.getString(R.string.calculate),
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
                Text(
                    text = resources.getString(R.string.calculate),
                    color = Color.White
                )
            }
        }

        ShowBottomSheetDialog(
            show = showDialog,
            maleFee = maleCost,
            femaleFee = femaleCost,
            absentFee = absentCost,
            backdrop = backdrop,
            contentColor = contentColor,
            glassSurface = glassSurface,
            dimColor = if (darkTheme) Color(0xFF121212).copy(alpha = 0.45f) else Color(0xFF29293A).copy(alpha = 0.22f)
        )
    }
}

@Composable
private fun GlassFieldRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun FeeRow(
    label: String,
    value: Double,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = contentColor.copy(alpha = 0.72f), fontSize = 16.sp)
        Text(
            text = "$value",
            color = contentColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ShowBottomSheetDialog(
    show: MutableState<Boolean>,
    maleFee: Double,
    femaleFee: Double,
    absentFee: Double,
    backdrop: Backdrop,
    contentColor: Color,
    glassSurface: Color,
    dimColor: Color
) {
    var malePicker by remember { mutableIntStateOf(3) }
    var femalePicker by remember { mutableIntStateOf(0) }
    var absentPicker by remember { mutableIntStateOf(0) }
    var customFee by remember { mutableDoubleStateOf(0.0) }
    val context = LocalContext.current

    LiquidBottomSheet(
        visible = show.value,
        onDismiss = { show.value = false },
        backdrop = backdrop,
        surfaceColor = glassSurface,
        dimColor = dimColor,
        handleColor = contentColor.copy(alpha = 0.28f)
    ) {
        val mutedLabel = contentColor.copy(alpha = 0.62f)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LiquidInsetGroup(backdrop = backdrop, surfaceColor = glassSurface) {
                repeat(6) { index ->
                    val pair = getNumCombination(index)
                    val fee = calculateFeeCombination(pair, maleFee, femaleFee)
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp, end = 12.dp),
                            thickness = 0.5.dp,
                            color = contentColor.copy(alpha = 0.08f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyOnClick(context, fee.toString()) }
                            .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.common_num_fee_label,
                                pair.first,
                                pair.second
                            ),
                            modifier = Modifier.weight(1F),
                            color = mutedLabel,
                            fontSize = 16.sp
                        )
                        LiquidGlassChip(
                            text = "$fee",
                            backdrop = backdrop,
                            textColor = contentColor,
                            surfaceColor = glassSurface,
                            onClick = { copyOnClick(context, fee.toString()) }
                        )
                    }
                }
            }

            LiquidInsetGroup(backdrop = backdrop, surfaceColor = glassSurface) {
                LaunchedEffect(malePicker, femalePicker, absentPicker) {
                    delay(100)
                    customFee = calculateFeeCombinationWithAbsent(
                        Pair(malePicker, maleFee),
                        Pair(femalePicker, femaleFee),
                        Pair(absentPicker, absentFee)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WheelField(
                            value = malePicker,
                            onValueChange = { malePicker = it },
                            label = stringResource(id = R.string.man),
                            backdrop = backdrop,
                            textColor = contentColor,
                            labelColor = mutedLabel,
                            surfaceColor = glassSurface
                        )
                        WheelField(
                            value = femalePicker,
                            onValueChange = { femalePicker = it },
                            label = stringResource(id = R.string.woman),
                            backdrop = backdrop,
                            textColor = contentColor,
                            labelColor = mutedLabel,
                            surfaceColor = glassSurface
                        )
                        if (absentFee > 0) {
                            WheelField(
                                value = absentPicker,
                                onValueChange = { absentPicker = it },
                                label = stringResource(id = R.string.absent),
                                backdrop = backdrop,
                                textColor = contentColor,
                                labelColor = mutedLabel,
                                surfaceColor = glassSurface
                            )
                        }
                    }
                    LiquidGlassChip(
                        text = "$customFee",
                        backdrop = backdrop,
                        textColor = contentColor,
                        surfaceColor = glassSurface,
                        onClick = { copyOnClick(context, customFee.toString()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    backdrop: Backdrop,
    textColor: Color,
    labelColor: Color,
    surfaceColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LiquidWheelPicker(
            value = value,
            range = 0..10,
            onValueChange = onValueChange,
            backdrop = backdrop,
            textColor = textColor,
            surfaceColor = surfaceColor
        )
        Text(
            text = label,
            color = labelColor,
            fontSize = 16.sp,
            maxLines = 1,
            softWrap = false
        )
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

fun calculateStringWithAbsent(
    courtFee: String,
    badmintonFee: String,
    extraMaleFee: String,
    maleCount: String,
    femaleCount: String,
    absentCount: String
): FeeResult {
    FeeCalculate.calculateWithAbsent(
        courtFee.safeToDouble(),
        badmintonFee.safeToDouble(),
        extraMaleFee.safeToDouble(),
        maleCount.safeToInt(),
        femaleCount.safeToInt(),
        absentCount.safeToInt()
    ).run {
        return FeeResult(this[0], this[1], this[2])
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

fun calculateFeeCombinationWithAbsent(
    malePair: Pair<Int, Double>,
    femalePair: Pair<Int, Double>,
    absentPair: Pair<Int, Double>,
): Double {
    return FeeCalculate.calculateOtherWithAbsent(
        malePair.first,
        malePair.second,
        femalePair.first,
        femalePair.second,
        absentPair.first,
        absentPair.second
    )
}