package com.supremesir.badmintonfeecalculator

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.supremesir.badmintonfeecalculator.ui.theme.BadmintonFeeCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
            // `auto` enforces a contrast scrim on the nav bar (the 小白条).
            // light/dark keeps icons readable without drawing an opaque strip.
            navigationBarStyle = if (night) {
                SystemBarStyle.dark(scrim = Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    scrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                )
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            BadmintonFeeCalculatorTheme {
                CalculatorScreen()
            }
        }
    }
}
