package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
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
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val isLightTheme = !isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    CompositionLocalProvider(LocalTextStyle provides LiquidType.body) {
        Row(
            modifier
                .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50) },
                effects = {
                    val progress = interactiveHighlight.pressProgress
                    liquidColorControls(isLightTheme)
                    blur(2f.dp.toPx())
                    lens(
                        (12f + 10f * progress).dp.toPx(),
                        (24f + 16f * progress).dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = progress > 0.05f
                    )
                },
                highlight = {
                    val progress = interactiveHighlight.pressProgress
                    Highlight.Default.copy(alpha = 0.45f + 0.55f * progress)
                },
                shadow = {
                    Shadow(alpha = 0.12f + 0.28f * interactiveHighlight.pressProgress)
                },
                innerShadow = {
                    val progress = interactiveHighlight.pressProgress
                    InnerShadow(
                        radius = 4f.dp + 8f.dp * progress,
                        alpha = 0.18f + 0.32f * progress
                    )
                },
                layerBlock = {
                    val width = size.width
                    val height = size.height
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 10f.dp.toPx() / height, progress)

                    val maxOffset = size.minDimension
                    val initialDerivative = 0.05f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                    val maxDragScale = 6f.dp.toPx() / height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX = scale +
                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                        (width / height).fastCoerceAtMost(1f)
                    scaleY = scale +
                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                        (height / width).fastCoerceAtMost(1f)
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.55f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .height(48.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
        )
    }
}
