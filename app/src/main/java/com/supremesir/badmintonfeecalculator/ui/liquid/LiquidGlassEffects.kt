package com.supremesir.badmintonfeecalculator.ui.liquid

import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.colorControls

internal fun BackdropEffectScope.liquidColorControls(isLightTheme: Boolean) {
    colorControls(
        brightness = if (isLightTheme) -0.04f else 0f,
        saturation = 1.5f
    )
}
