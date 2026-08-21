package com.supremesir.badmintonfeecalculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidInputTile
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidInsetGroup
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidResultTile
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidTapToEditRow
import com.supremesir.badmintonfeecalculator.ui.liquid.LiquidWheelPicker
import kotlinx.coroutines.delay

@Composable
fun CalculatorScreen(
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    var courtFee by remember { mutableStateOf("") }
    var badmintonFee by remember { mutableStateOf("") }
    var extraMaleFee by remember { mutableStateOf("5") }
    var extraFeeEditing by remember { mutableStateOf(false) }
    var extraFeeRowBounds by remember { mutableStateOf(Rect.Zero) }
    var maleCount by remember { mutableIntStateOf(0) }
    var femaleCount by remember { mutableIntStateOf(0) }
    var absentCount by remember { mutableIntStateOf(0) }
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
    val focusManager = LocalFocusManager.current
    val extraFeeRowBoundsState = rememberUpdatedState(extraFeeRowBounds)
    val extraFeeEditingState = rememberUpdatedState(extraFeeEditing)
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (!extraFeeEditingState.value) continue
                        val down = event.changes.find { it.changedToDown() } ?: continue
                        val coords = rootCoordinates ?: continue
                        val tapInWindow = coords.localToWindow(down.position)
                        if (!extraFeeRowBoundsState.value.contains(tapInWindow)) {
                            extraFeeEditing = false
                            if (extraMaleFee.isBlank()) extraMaleFee = "5"
                            focusManager.clearFocus(force = true)
                        }
                    }
                }
            }
    ) {
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
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = resources.getString(R.string.app_name),
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                color = contentColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidInputTile(
                        label = resources.getString(R.string.court_fee_label),
                        value = courtFee,
                        onValueChange = { courtFee = it },
                        backdrop = backdrop,
                        textColor = contentColor,
                        mutedColor = mutedColor,
                        surfaceColor = glassSurface,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                    LiquidInputTile(
                        label = resources.getString(R.string.shuttlecock_fee_label),
                        value = badmintonFee,
                        onValueChange = { badmintonFee = it },
                        backdrop = backdrop,
                        textColor = contentColor,
                        mutedColor = mutedColor,
                        surfaceColor = glassSurface,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                }

                LiquidInsetGroup(backdrop = backdrop, surfaceColor = glassSurface) {
                    LiquidTapToEditRow(
                        label = resources.getString(R.string.extra_fee_label),
                        value = extraMaleFee,
                        onValueChange = { extraMaleFee = it },
                        backdrop = backdrop,
                        textColor = contentColor,
                        mutedColor = mutedColor,
                        surfaceColor = glassSurface,
                        accent = accent,
                        editing = extraFeeEditing,
                        onEditingChange = { extraFeeEditing = it },
                        onBoundsInWindow = { extraFeeRowBounds = it }
                    )
                }

                LiquidInsetGroup(backdrop = backdrop, surfaceColor = glassSurface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountWheel(
                            value = maleCount,
                            onValueChange = { maleCount = it },
                            label = resources.getString(R.string.man),
                            backdrop = backdrop,
                            textColor = contentColor,
                            labelColor = mutedColor,
                            surfaceColor = glassSurface
                        )
                        CountWheel(
                            value = femaleCount,
                            onValueChange = { femaleCount = it },
                            label = resources.getString(R.string.woman),
                            backdrop = backdrop,
                            textColor = contentColor,
                            labelColor = mutedColor,
                            surfaceColor = glassSurface
                        )
                        CountWheel(
                            value = absentCount,
                            onValueChange = { absentCount = it },
                            label = resources.getString(R.string.absent),
                            backdrop = backdrop,
                            textColor = contentColor,
                            labelColor = mutedColor,
                            surfaceColor = glassSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidResultTile(
                    label = resources.getString(R.string.man),
                    value = maleCost,
                    backdrop = backdrop,
                    textColor = contentColor,
                    mutedColor = mutedColor,
                    surfaceColor = glassSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { copyOnClick(context, maleCost.toString()) }
                )
                LiquidResultTile(
                    label = resources.getString(R.string.woman),
                    value = femaleCost,
                    backdrop = backdrop,
                    textColor = contentColor,
                    mutedColor = mutedColor,
                    surfaceColor = glassSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { copyOnClick(context, femaleCost.toString()) }
                )
                if (absentCount > 0) {
                    LiquidResultTile(
                        label = resources.getString(R.string.absent),
                        value = absentCost,
                        backdrop = backdrop,
                        textColor = contentColor,
                        mutedColor = mutedColor,
                        surfaceColor = glassSurface,
                        modifier = Modifier.weight(1f),
                        onClick = { copyOnClick(context, absentCost.toString()) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiquidButton(
                    onClick = { showDialog.value = true },
                    backdrop = backdrop,
                    modifier = Modifier.weight(1.28f),
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
                            maleCount.toString(),
                            femaleCount.toString(),
                            absentCount.toString()
                        ).run {
                            maleCost = this.maleFee
                            femaleCost = this.femaleFee
                            absentCost = this.absentFee
                        }
                    },
                    backdrop = backdrop,
                    modifier = Modifier.weight(0.88f),
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
private fun CountWheel(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    backdrop: Backdrop,
    textColor: Color,
    labelColor: Color,
    surfaceColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LiquidWheelPicker(
            value = value,
            range = 0..20,
            onValueChange = onValueChange,
            backdrop = backdrop,
            textColor = textColor,
            surfaceColor = surfaceColor,
            wheelWidth = 52.dp
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 6.dp),
            color = labelColor,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            modifier = Modifier.padding(top = 6.dp),
            color = labelColor,
            fontSize = 13.sp,
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