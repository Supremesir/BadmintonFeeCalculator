package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.supremesir.badmintonfeecalculator.ui.theme.LiquidType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidResultTile(
    label: String,
    value: Double,
    backdrop: Backdrop,
    textColor: Color,
    mutedColor: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val isLightTheme = !isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    val progress = interactiveHighlight.pressProgress
                    liquidColorControls(isLightTheme)
                    blur(4f.dp.toPx())
                    lens(
                        (16f + 8f * progress).dp.toPx(),
                        (32f + 12f * progress).dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = progress > 0.05f
                    )
                },
                highlight = {
                    val progress = interactiveHighlight.pressProgress
                    Highlight.Default.copy(alpha = 0.45f + 0.55f * progress)
                },
                shadow = {
                    Shadow(alpha = 0.10f + 0.24f * interactiveHighlight.pressProgress)
                },
                innerShadow = {
                    val progress = interactiveHighlight.pressProgress
                    InnerShadow(
                        radius = 6f.dp + 6f.dp * progress,
                        alpha = 0.22f + 0.28f * progress
                    )
                },
                layerBlock = {
                    val width = size.width
                    val height = size.height
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 8f.dp.toPx() / height, progress)

                    val maxOffset = size.minDimension
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)

                    val maxDragScale = 5f.dp.toPx() / height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX = scale +
                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                        (width / height).fastCoerceAtMost(1f)
                    scaleY = scale +
                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                        (height / width).fastCoerceAtMost(1f)
                },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = mutedColor,
            style = LiquidType.caption
        )
        Text(
            text = "$value",
            color = textColor,
            style = LiquidType.number
        )
    }
}
