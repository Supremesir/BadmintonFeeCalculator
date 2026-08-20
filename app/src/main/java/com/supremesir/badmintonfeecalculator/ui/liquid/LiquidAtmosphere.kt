package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Calm wallpaper inspired by iOS 26 Sky / Shadow:
 * one muted blue-gray family, overlapping frosted slabs, soft light.
 * Busy multi-hue blobs make Liquid Glass look gaudy instead of refractive.
 */
@Composable
fun LiquidAtmosphere(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val palette = if (darkTheme) ShadowPalette else SkyPalette

    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.top, palette.bottom)
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.light.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.16f, size.height * 0.04f),
                radius = size.minDimension * 1.05f
            ),
            radius = size.minDimension * 1.05f,
            center = Offset(size.width * 0.16f, size.height * 0.04f)
        )

        val w = size.width
        val h = size.height
        frostSlab(
            rotation = -24f,
            origin = Offset(w * -0.22f, h * 0.02f),
            slabSize = Size(w * 1.02f, h * 0.36f),
            color = palette.slab,
            highlight = palette.light
        )
        frostSlab(
            rotation = 16f,
            origin = Offset(w * 0.12f, h * 0.28f),
            slabSize = Size(w * 1.08f, h * 0.34f),
            color = palette.slabDeep,
            highlight = palette.light
        )
        frostSlab(
            rotation = -9f,
            origin = Offset(w * -0.12f, h * 0.58f),
            slabSize = Size(w * 1.18f, h * 0.44f),
            color = palette.slab,
            highlight = palette.light
        )
    }
}

private fun DrawScope.frostSlab(
    rotation: Float,
    origin: Offset,
    slabSize: Size,
    color: Color,
    highlight: Color
) {
    val pivot = origin + Offset(slabSize.width / 2f, slabSize.height / 2f)
    val radius = CornerRadius(slabSize.minDimension * 0.22f)
    rotate(rotation, pivot = pivot) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    highlight.copy(alpha = 0.38f),
                    color.copy(alpha = 0.28f),
                    color.copy(alpha = 0.08f)
                ),
                start = origin,
                end = origin + Offset(slabSize.width * 0.2f, slabSize.height)
            ),
            topLeft = origin,
            size = slabSize,
            cornerRadius = radius
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(highlight.copy(alpha = 0.22f), Color.Transparent),
                startY = origin.y,
                endY = origin.y + slabSize.height * 0.45f
            ),
            topLeft = origin,
            size = Size(slabSize.width, slabSize.height * 0.45f),
            cornerRadius = radius
        )
    }
}

private data class AtmospherePalette(
    val top: Color,
    val bottom: Color,
    val light: Color,
    val slab: Color,
    val slabDeep: Color
)

private val SkyPalette = AtmospherePalette(
    top = Color(0xFFEAF1F7),
    bottom = Color(0xFFC5D3E2),
    light = Color(0xFFF8FBFF),
    slab = Color(0xFFB4C6D8),
    slabDeep = Color(0xFF96ADC4)
)

private val ShadowPalette = AtmospherePalette(
    top = Color(0xFF1B222C),
    bottom = Color(0xFF0D1117),
    light = Color(0xFF3D4A5C),
    slab = Color(0xFF2A3544),
    slabDeep = Color(0xFF1C2632)
)
