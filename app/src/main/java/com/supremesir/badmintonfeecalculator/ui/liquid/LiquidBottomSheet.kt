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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

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
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(with(density) { 640.dp.toPx() }) }
    var scrimProgress by remember { mutableFloatStateOf(0f) }
    var animationJob by remember { mutableStateOf<Job?>(null) }
    var closingInternally by remember { mutableStateOf(false) }

    fun runAnimation(block: suspend CoroutineScope.() -> Unit) {
        animationJob?.cancel()
        animationJob = scope.launch(block = block)
    }

    fun animateIn(height: Float) {
        if (height <= 0f) return
        runAnimation {
            offsetY = height
            launch {
                animate(
                    initialValue = scrimProgress,
                    targetValue = 1f,
                    animationSpec = tween(280)
                ) { value, _ -> scrimProgress = value }
            }
            animate(
                initialValue = height,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ -> offsetY = value }
        }
    }

    fun animateOut(notifyDismiss: Boolean) {
        closingInternally = true
        val target = sheetHeightPx.coerceAtLeast(offsetY)
        runAnimation {
            launch {
                animate(
                    initialValue = scrimProgress,
                    targetValue = 0f,
                    animationSpec = tween(220)
                ) { value, _ -> scrimProgress = value }
            }
            animate(
                initialValue = offsetY,
                targetValue = target,
                animationSpec = tween(280, easing = FastOutLinearInEasing)
            ) { value, _ -> offsetY = value }
            displayed = false
            closingInternally = false
            if (notifyDismiss) dismissHandler()
        }
    }

    fun settle(velocity: Float) {
        val height = sheetHeightPx.coerceAtLeast(1f)
        val shouldDismiss = offsetY > height * 0.22f || velocity > 1800f
        if (shouldDismiss) {
            animateOut(notifyDismiss = true)
        } else {
            runAnimation {
                launch {
                    animate(
                        initialValue = scrimProgress,
                        targetValue = 1f,
                        animationSpec = tween(180)
                    ) { value, _ -> scrimProgress = value }
                }
                animate(
                    initialValue = offsetY,
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { value, _ -> offsetY = value }
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            closingInternally = false
            displayed = true
            if (sheetHeightPx > 0f) animateIn(sheetHeightPx)
        } else if (displayed && !closingInternally) {
            animateOut(notifyDismiss = false)
        }
    }

    BackHandler(enabled = visible) { dismissHandler() }

    val dragState = rememberDraggableState { delta ->
        animationJob?.cancel()
        offsetY = (offsetY + delta).coerceAtLeast(0f)
        if (sheetHeightPx > 0f) {
            scrimProgress = (1f - offsetY / sheetHeightPx).coerceIn(0f, 1f)
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < 0f && offsetY > 0f) {
                animationJob?.cancel()
                val consumed = maxOf(available.y, -offsetY)
                offsetY += consumed
                if (sheetHeightPx > 0f) {
                    scrimProgress = (1f - offsetY / sheetHeightPx).coerceIn(0f, 1f)
                }
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
                animationJob?.cancel()
                offsetY += available.y
                if (sheetHeightPx > 0f) {
                    scrimProgress = (1f - offsetY / sheetHeightPx).coerceIn(0f, 1f)
                }
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle(available.y)
            return available
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dimColor.copy(alpha = dimColor.alpha * scrimProgress))
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
                    val firstMeasure = sheetHeightPx == 0f
                    sheetHeightPx = measured
                    if (visible && firstMeasure) animateIn(measured)
                }
                .offset { IntOffset(0, offsetY.roundToInt()) }
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
