package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun LiquidBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    surfaceColor: Color,
    dimColor: Color,
    handleColor: Color = Color.White.copy(alpha = 0.38f),
    content: @Composable ColumnScope.() -> Unit
) {
    var displayed by remember { mutableStateOf(false) }
    if (!visible && !displayed) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissHandler by rememberUpdatedState(onDismiss)
    val offsetY = remember { mutableFloatStateOf(with(density) { 640.dp.toPx() }) }
    val sheetHeightPx = remember { mutableFloatStateOf(0f) }
    val animationJob = remember { mutableStateOf<Job?>(null) }
    var closingInternally by remember { mutableStateOf(false) }

    fun runAnimation(block: suspend CoroutineScope.() -> Unit) {
        animationJob.value?.cancel()
        animationJob.value = scope.launch(block = block)
    }

    fun animateIn(height: Float) {
        if (height <= 0f) return
        runAnimation {
            offsetY.floatValue = height
            animate(
                initialValue = height,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.92f,
                    stiffness = Spring.StiffnessMedium
                )
            ) { value, _ -> offsetY.floatValue = value }
        }
    }

    fun animateOut(notifyDismiss: Boolean) {
        closingInternally = true
        val target = sheetHeightPx.floatValue.coerceAtLeast(offsetY.floatValue)
        runAnimation {
            animate(
                initialValue = offsetY.floatValue,
                targetValue = target,
                animationSpec = tween(220, easing = FastOutLinearInEasing)
            ) { value, _ -> offsetY.floatValue = value }
            displayed = false
            closingInternally = false
            if (notifyDismiss) dismissHandler()
        }
    }

    fun settle(velocity: Float) {
        val height = sheetHeightPx.floatValue.coerceAtLeast(1f)
        val shouldDismiss = offsetY.floatValue > height * 0.18f || velocity > 1400f
        if (shouldDismiss) {
            animateOut(notifyDismiss = true)
        } else {
            runAnimation {
                animate(
                    initialValue = offsetY.floatValue,
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { value, _ -> offsetY.floatValue = value }
            }
        }
    }

    val settleState = rememberUpdatedState(::settle)

    LaunchedEffect(visible) {
        if (visible) {
            closingInternally = false
            displayed = true
            if (sheetHeightPx.floatValue > 0f) animateIn(sheetHeightPx.floatValue)
        } else if (displayed && !closingInternally) {
            animateOut(notifyDismiss = false)
        }
    }

    BackHandler(enabled = visible) { dismissHandler() }

    val dragState = rememberDraggableState { delta ->
        animationJob.value?.cancel()
        offsetY.floatValue = (offsetY.floatValue + delta).coerceAtLeast(0f)
    }

    val nestedScrollConnection = remember(offsetY, sheetHeightPx, animationJob) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && offsetY.floatValue > 0f) {
                    animationJob.value?.cancel()
                    val consumed = maxOf(available.y, -offsetY.floatValue)
                    offsetY.floatValue += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f) {
                    animationJob.value?.cancel()
                    offsetY.floatValue += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                settleState.value(available.y)
                return available
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val height = sheetHeightPx.floatValue
                    alpha = if (height > 0f) {
                        (1f - offsetY.floatValue / height).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
                .background(dimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissHandler() }
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .onSizeChanged { size ->
                    val measured = size.height.toFloat()
                    val firstMeasure = sheetHeightPx.floatValue == 0f
                    sheetHeightPx.floatValue = measured
                    if (visible && firstMeasure) animateIn(measured)
                }
                .graphicsLayer { translationY = offsetY.floatValue }
                .nestedScroll(nestedScrollConnection)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(36.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 48f.dp.toPx(), true)
                    },
                    onDrawSurface = { drawRect(surfaceColor) }
                )
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> settle(velocity) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(handleColor)
                )
            }
            content()
        }
    }
}
