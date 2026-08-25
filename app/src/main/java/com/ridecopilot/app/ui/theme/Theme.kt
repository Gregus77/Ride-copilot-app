package com.ridecopilot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color.Black,
    secondary = BrandGreenDark,
    background = SlateBackgroundDark,
    onBackground = SlateOnDark,
    surface = SlateSurfaceDark,
    onSurface = SlateOnDark
)

private val LightColors = lightColorScheme(
    primary = BrandGreenDark,
    onPrimary = Color.White,
    secondary = BrandGreen,
    background = SlateBackgroundLight,
    onBackground = SlateOnLight,
    surface = SlateSurfaceLight,
    onSurface = SlateOnLight
)

@Composable
fun RideCopilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
