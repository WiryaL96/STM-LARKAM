package com.wiryadinata.stmlarkam.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = LarkamBlue,
    onPrimary = Color.White,
    primaryContainer = LarkamBlueLight,
    secondary = LarkamGreen,
    tertiary = LarkamAmber
)

private val DarkColors = darkColorScheme(
    primary = LarkamBlueLight,
    onPrimary = Color.Black,
    primaryContainer = LarkamBlueDark,
    secondary = LarkamGreen,
    tertiary = LarkamAmber
)

@Composable
fun LarkamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You dynamic color is available on Android 12+.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
