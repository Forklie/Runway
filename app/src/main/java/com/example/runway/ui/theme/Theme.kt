package com.example.runway.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalRunwayColors = staticCompositionLocalOf { LightRunwayColors }

object RunwayTheme {
    val colors: RunwayColors
        @Composable @ReadOnlyComposable get() = LocalRunwayColors.current
}

/** Brand-led, so no dynamic colour. surfaceTint is transparent: no Material elevation overlay. */
@Composable
fun RunwayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkRunwayColors else LightRunwayColors
    CompositionLocalProvider(LocalRunwayColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = RunwayMaterialTypography,
            shapes = RunwayMaterialShapes,
            content = content,
        )
    }
}

private fun RunwayColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = action, onPrimary = onAction,
        primaryContainer = accentSoft, onPrimaryContainer = accent,
        inversePrimary = inverseAction,
        secondary = accent, onSecondary = surface,
        secondaryContainer = accentSoft, onSecondaryContainer = accent,
        tertiary = goal, onTertiary = onGoal,
        tertiaryContainer = goalSoft, onTertiaryContainer = ink,
        background = bg, onBackground = ink,
        surface = surface, onSurface = ink,
        surfaceVariant = surfaceSunken, onSurfaceVariant = inkMuted,
        surfaceTint = Color.Transparent,
        inverseSurface = inverseSurface, inverseOnSurface = onInverse,
        error = danger, onError = onDanger,
        errorContainer = surfaceSunken, onErrorContainer = danger,
        outline = lineStrong, outlineVariant = line, scrim = scrim,
        surfaceBright = surface, surfaceDim = bg,
        surfaceContainerLowest = surface, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surfaceSunken, surfaceContainerHighest = surfaceSunken,
    )
}