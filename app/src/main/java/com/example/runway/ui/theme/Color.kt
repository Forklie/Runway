package com.example.runway.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Brand palette, same in both themes. Use inside role colours and motifs only. */
object Brand {
    val Azul = Color(0xFF3B38E8)
    val Coral = Color(0xFFFF6461)
    val Amarillo = Color(0xFFFFB81C)
    val Rosa = Color(0xFFFFC9CC)
    val Blanco = Color(0xFFFFFFFF)
    val Midnight = Color(0xFF15134B)
}

/** Role tokens. Screens and components use these, never hex values or Brand directly. */
@Immutable
data class RunwayColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkDisabled: Color,
    val accent: Color,
    val accentSoft: Color,
    val action: Color,
    val onAction: Color,
    val brandBlock: Color,
    val onBrandSoft: Color,
    val onBrand: Color,
    val onBrandMuted: Color,
    val goal: Color,
    val onGoal: Color,
    val goalSoft: Color,
    val highlight: Color,
    val onHighlight: Color,
    val highlightSoft: Color,
    val danger: Color,
    val onDanger: Color,
    val inverseSurface: Color,
    val onInverse: Color,
    val inverseAction: Color,
    val scrim: Color,
)

val LightRunwayColors = RunwayColors(
    isDark = false,
    bg = Color(0xFFF4F4FD),
    surface = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFEBEBFB),
    line = Color(0xFFDEDEF3),
    lineStrong = Color(0xFF8584B5),
    ink = Brand.Midnight,
    inkMuted = Color(0xFF57568A),
    inkDisabled = Color(0xFFA9A8C9),
    accent = Brand.Azul,
    accentSoft = Color(0xFFE6E6FD),
    action = Brand.Azul,
    onAction = Brand.Blanco,
    brandBlock = Brand.Azul,
    onBrandSoft = Color(0x29FFFFFF),
    onBrand = Brand.Blanco,
    onBrandMuted = Brand.Rosa,
    goal = Brand.Coral,
    onGoal = Brand.Midnight,
    goalSoft = Brand.Rosa,
    highlight = Brand.Amarillo,
    onHighlight = Brand.Midnight,
    highlightSoft = Color(0xFFFFF1CC),
    danger = Color(0xFFC42B3A),
    onDanger = Brand.Blanco,
    inverseSurface = Brand.Midnight,
    onInverse = Color(0xFFF1F1FF),
    inverseAction = Brand.Amarillo,
    scrim = Color(0x6615134B),
)

val DarkRunwayColors = RunwayColors(
    isDark = true,
    bg = Color(0xFF0B0A26),
    surface = Color(0xFF17163F),
    surfaceSunken = Color(0xFF211F55),
    line = Color(0xFF2C2A66),
    lineStrong = Color(0xFF6E6CA8),
    ink = Color(0xFFF1F1FF),
    inkMuted = Color(0xFFA9A8D8),
    inkDisabled = Color(0xFF5A5990),
    accent = Color(0xFFA3A1FF),
    accentSoft = Color(0xFF26246B),
    action = Brand.Blanco,
    onAction = Brand.Azul,
    brandBlock = Brand.Azul,
    onBrandSoft = Color(0x29FFFFFF),
    onBrand = Brand.Blanco,
    onBrandMuted = Brand.Rosa,
    goal = Brand.Coral,
    onGoal = Brand.Midnight,
    goalSoft = Color(0xFF4A1E3A),
    highlight = Brand.Amarillo,
    onHighlight = Brand.Midnight,
    highlightSoft = Color(0xFF3A2C06),
    danger = Color(0xFFFF8A93),
    onDanger = Brand.Midnight,
    inverseSurface = Color(0xFFF1F1FF),
    onInverse = Brand.Midnight,
    inverseAction = Brand.Azul,
    scrim = Color(0xA3000000),
)