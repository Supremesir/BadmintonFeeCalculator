package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.supremesir.badmintonfeecalculator.R
import com.supremesir.badmintonfeecalculator.SHUTTLECOCK_COUNT_RANGE
import com.supremesir.badmintonfeecalculator.ShuttlecockLine
import com.supremesir.badmintonfeecalculator.formatShuttlecockFee
import com.supremesir.badmintonfeecalculator.nextLineId
import com.supremesir.badmintonfeecalculator.shuttlecockFeeTotal
import com.supremesir.badmintonfeecalculator.ui.theme.LiquidType
import kotlinx.coroutines.delay

private val DeleteRed = Color(0xFFFF3B30)

@Composable
fun ShuttlecockFeeSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    currentFee: String,
    onFeeChange: (String) -> Unit,
    onDetailModeChange: (Boolean) -> Unit,
    backdrop: Backdrop,
    contentColor: Color,
    mutedColor: Color,
    glassSurface: Color,
    dimColor: Color,
    accent: Color
) {
    var lines by remember { mutableStateOf(listOf<ShuttlecockLine>()) }
    var feeSnapshot by remember { mutableStateOf("") }
    val currentFeeState = rememberUpdatedState(currentFee)
    val onFeeChangeState = rememberUpdatedState(onFeeChange)
    val onDetailModeChangeState = rememberUpdatedState(onDetailModeChange)

    fun exitDetail() {
        val hadPrice = lines.any { it.tubePrice.isNotBlank() }
        val computed = formatShuttlecockFee(shuttlecockFeeTotal(lines))
        lines = emptyList()
        onDetailModeChangeState.value(false)
        onFeeChangeState.value(if (hadPrice) computed else feeSnapshot)
        onDismiss()
    }

    LaunchedEffect(visible) {
        if (visible && lines.isEmpty()) {
            feeSnapshot = currentFeeState.value
            lines = listOf(ShuttlecockLine(id = 1L, usedCount = 12))
            onDetailModeChangeState.value(true)
        }
    }

    LaunchedEffect(lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        delay(48)
        onFeeChangeState.value(formatShuttlecockFee(shuttlecockFeeTotal(lines)))
    }

    LiquidBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        backdrop = backdrop,
        surfaceColor = glassSurface,
        dimColor = dimColor,
        handleColor = contentColor.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text(
                    text = stringResource(R.string.shuttlecock_detail_title),
                    color = contentColor,
                    style = LiquidType.body
                )
                Text(
                    text = stringResource(R.string.shuttlecock_per_tube),
                    color = mutedColor,
                    style = LiquidType.caption,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            LiquidInsetGroup(backdrop = backdrop, surfaceColor = glassSurface) {
                lines.forEachIndexed { index, line ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp, end = 12.dp),
                            thickness = 0.5.dp,
                            color = contentColor.copy(alpha = 0.08f)
                        )
                    }
                    ShuttlecockLineRow(
                        line = line,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        mutedColor = mutedColor,
                        glassSurface = glassSurface,
                        accent = accent,
                        onLineChange = { updated ->
                            lines = lines.map { if (it.id == updated.id) updated else it }
                        },
                        onDelete = {
                            val remaining = lines.filter { it.id != line.id }
                            if (remaining.isEmpty()) exitDetail() else lines = remaining
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidButton(
                    onClick = { exitDetail() },
                    backdrop = backdrop,
                    modifier = Modifier.weight(0.88f),
                    surfaceColor = glassSurface
                ) {
                    Text(
                        text = stringResource(R.string.shuttlecock_exit_detail),
                        color = contentColor
                    )
                }
                LiquidButton(
                    onClick = {
                        lines = lines + ShuttlecockLine(id = lines.nextLineId(), usedCount = 12)
                    },
                    backdrop = backdrop,
                    modifier = Modifier.weight(1.28f),
                    tint = accent
                ) {
                    Text(
                        text = stringResource(R.string.shuttlecock_add_line),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ShuttlecockLineRow(
    line: ShuttlecockLine,
    backdrop: Backdrop,
    contentColor: Color,
    mutedColor: Color,
    glassSurface: Color,
    accent: Color,
    onLineChange: (ShuttlecockLine) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.shuttlecock_tube_price),
            color = mutedColor,
            style = LiquidType.caption
        )
        BasicTextField(
            value = line.tubePrice,
            onValueChange = { onLineChange(line.copy(tubePrice = it)) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glassSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            textStyle = LiquidType.chip.copy(
                color = contentColor,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(accent),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (line.tubePrice.isEmpty()) {
                        Text(
                            text = "0",
                            color = mutedColor.copy(alpha = 0.45f),
                            style = LiquidType.chip,
                            textAlign = TextAlign.Center
                        )
                    }
                    inner()
                }
            }
        )
        Text(
            text = stringResource(R.string.shuttlecock_used_count),
            color = mutedColor,
            style = LiquidType.caption
        )
        LiquidWheelPicker(
            value = line.usedCount,
            range = SHUTTLECOCK_COUNT_RANGE,
            onValueChange = { onLineChange(line.copy(usedCount = it)) },
            backdrop = backdrop,
            textColor = contentColor,
            surfaceColor = glassSurface,
            visibleCount = 3,
            itemHeight = 32.dp,
            wheelWidth = 48.dp,
            liteGlass = true
        )
        ShuttlecockDeleteButton(
            backdrop = backdrop,
            onClick = onDelete
        )
    }
}

@Composable
private fun ShuttlecockDeleteButton(
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .size(36.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50) },
                effects = {
                    liquidColorControls(isLightTheme)
                    blur(2f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(DeleteRed, blendMode = BlendMode.Hue)
                    drawRect(DeleteRed.copy(alpha = 0.55f))
                }
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "×",
            color = Color.White,
            fontSize = 20.sp,
            style = LiquidType.body,
            textAlign = TextAlign.Center
        )
    }
}
