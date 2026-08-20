package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun LiquidBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    surfaceColor: Color,
    dimColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return

    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content
        )
    }
}
