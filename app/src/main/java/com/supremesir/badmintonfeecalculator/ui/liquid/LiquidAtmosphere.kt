package com.supremesir.badmintonfeecalculator.ui.liquid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun LiquidAtmosphere(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val background = if (darkTheme) Color(0xFF101018) else Color(0xFFF4F1FA)
    val blobMint = if (darkTheme) Color(0xFF1E4D45) else Color(0xFFB7EBD6)
    val blobLilac = if (darkTheme) Color(0xFF3B2A58) else Color(0xFFD5C6F6)
    val blobPeach = if (darkTheme) Color(0xFF3A2A22) else Color(0xFFFFD9B8)

    Canvas(modifier.fillMaxSize()) {
        drawRect(background)
        drawCircle(
            color = blobMint.copy(alpha = 0.9f),
            radius = size.minDimension * 0.72f,
            center = Offset(size.width * 0.08f, size.height * 0.18f)
        )
        drawCircle(
            color = blobLilac.copy(alpha = 0.88f),
            radius = size.minDimension * 0.68f,
            center = Offset(size.width * 0.96f, size.height * 0.32f)
        )
        drawCircle(
            color = blobPeach.copy(alpha = 0.82f),
            radius = size.minDimension * 0.86f,
            center = Offset(size.width * 0.42f, size.height * 0.92f)
        )
    }
}
