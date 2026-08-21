package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.supremesir.badmintonfeecalculator.ui.theme.LiquidType

@Composable
fun LiquidInsetGroup(
    backdrop: Backdrop,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(22.dp) },
                effects = {
                    liquidColorControls(isLightTheme)
                    blur(5f.dp.toPx())
                    lens(10f.dp.toPx(), 20f.dp.toPx())
                },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun LiquidGlassChip(
    text: String,
    backdrop: Backdrop,
    textColor: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    minWidth: Dp = 72.dp,
    onClick: (() -> Unit)? = null
) {
    val isLightTheme = !isSystemInDarkTheme()
    Box(
        modifier
            .widthIn(min = minWidth)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(12.dp) },
                effects = {
                    liquidColorControls(isLightTheme)
                    blur(4f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = LiquidType.chip
        )
    }
}
