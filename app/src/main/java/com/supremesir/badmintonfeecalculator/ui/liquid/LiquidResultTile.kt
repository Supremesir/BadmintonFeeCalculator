package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow

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
    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Default },
                innerShadow = {
                    InnerShadow(radius = 6f.dp, alpha = 0.28f)
                },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = mutedColor,
            fontSize = 13.sp
        )
        Text(
            text = "$value",
            color = textColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
