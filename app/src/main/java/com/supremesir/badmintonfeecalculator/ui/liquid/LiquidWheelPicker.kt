package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun LiquidWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    backdrop: Backdrop,
    textColor: Color,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 36.dp,
    surfaceColor: Color = Color.White.copy(alpha = 0.22f)
) {
    val paddingCount = visibleCount / 2
    val values = remember(range) { range.toList() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, values.lastIndex)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val cameraDistancePx = with(density) { 18.dp.toPx() }

    val onValueChangeState by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    val selectedValue by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val nearest = info.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2) - center)
            }
            val index = (nearest?.index ?: listState.firstVisibleItemIndex) - paddingCount
            values.getOrElse(index.coerceIn(0, values.lastIndex)) { currentValue }
        }
    }

    LaunchedEffect(listState, values) {
        var initialized = false
        snapshotFlow { selectedValue }
            .distinctUntilChanged()
            .collect { selected ->
                if (initialized && selected != currentValue) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChangeState(selected)
                }
                initialized = true
            }
    }

    LaunchedEffect(value, values) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val target = (value - range.first).coerceIn(0, values.lastIndex)
        if (selectedValue != value) {
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier
            .width(44.dp)
            .height(itemHeight * visibleCount)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 2.dp)
                .fillMaxWidth()
                .height(itemHeight)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(12.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(surfaceColor) }
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            flingBehavior = flingBehavior
        ) {
            items(
                count = values.size + paddingCount * 2,
                key = { it }
            ) { index ->
                val valueIndex = index - paddingCount
                val number = values.getOrNull(valueIndex)
                val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                val viewportCenter =
                    (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2f
                val itemCenter = itemInfo?.let { it.offset + it.size / 2f } ?: viewportCenter
                val distance = ((itemCenter - viewportCenter) / itemHeightPx)
                val absDistance = abs(distance).coerceAtMost(2.4f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            rotationX = distance * -22f
                            alpha = (1f - absDistance * 0.38f).coerceAtLeast(0.14f)
                            val scale = 1f - absDistance * 0.08f
                            scaleX = scale
                            scaleY = scale
                            cameraDistance = cameraDistancePx
                            transformOrigin = TransformOrigin.Center
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (number != null) {
                        Text(
                            text = number.toString(),
                            modifier = Modifier.fillMaxWidth(),
                            color = textColor,
                            fontSize = 22.sp,
                            fontWeight = if (absDistance < 0.5f) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
