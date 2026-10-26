package com.example.runway.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.runway.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
    tnum: Boolean = false,
) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = if (tnum) "tnum" else null,
)

/** The 15 type styles of the design system. Lightest weight is Medium 500. Money styles use tabular figures. */
object RunwayType {
    val displayLg = style(40, 44, FontWeight.ExtraBold, -0.02)
    val display = style(32, 38, FontWeight.ExtraBold, -0.015)
    val title1 = style(24, 30, FontWeight.Bold, -0.01)
    val title2 = style(20, 26, FontWeight.Bold, -0.005)
    val headline = style(17, 24, FontWeight.Bold)
    val body = style(16, 24, FontWeight.Medium)
    val bodySm = style(14, 20, FontWeight.Medium)
    val labelLg = style(16, 20, FontWeight.Bold)
    val label = style(14, 18, FontWeight.Bold)
    val caption = style(12, 16, FontWeight.SemiBold, 0.01)
    val eyebrow = style(11, 14, FontWeight.ExtraBold, 0.08) // render uppercase
    val moneyHero = style(52, 56, FontWeight.ExtraBold, -0.03, tnum = true)
    val moneyLg = style(28, 34, FontWeight.ExtraBold, -0.015, tnum = true)
    val money = style(17, 24, FontWeight.Bold, tnum = true)
    val keypad = style(28, 32, FontWeight.SemiBold, tnum = true)

    /** Word-chip lockups (empty states). */
    val word = style(24, 24, FontWeight.ExtraBold, -0.01)
}

/** Maps the Runway styles onto Material slots so stock components pick up the font. */
val RunwayMaterialTypography = Typography(
    displayLarge = RunwayType.displayLg,
    displayMedium = RunwayType.display,
    displaySmall = RunwayType.display,
    headlineLarge = RunwayType.title1,
    headlineMedium = RunwayType.title2,
    headlineSmall = RunwayType.headline,
    titleLarge = RunwayType.title2,
    titleMedium = RunwayType.headline,
    titleSmall = RunwayType.label,
    bodyLarge = RunwayType.body,
    bodyMedium = RunwayType.bodySm,
    bodySmall = RunwayType.caption,
    labelLarge = RunwayType.labelLg,
    labelMedium = RunwayType.label,
    labelSmall = RunwayType.eyebrow,
)