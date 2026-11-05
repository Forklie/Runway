package com.example.runway.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Each shape has one meaning in the product (design system, Shape language). */
object RunwayShape {
    val Pill = CircleShape                                  // press this
    val Tag = RoundedCornerShape(Radii.xs)
    val Keycap = RoundedCornerShape(Radii.sm)               // type or tap a key
    val Field = RoundedCornerShape(Radii.md)
    val Card = RoundedCornerShape(Radii.lg)
    val HeroPanel = RoundedCornerShape(bottomStart = Radii.xl, bottomEnd = Radii.xl)
    val Sheet = RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl)
}

/** Arrow tag: square-ish left side, point on the right. Where money goes, what comes next. */
class ArrowTagShape(
    private val tip: Dp = 11.dp,
    private val corner: Dp = Radii.xs,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { corner.toPx() }
        val t = with(density) { tip.toPx() }.coerceAtMost(size.width)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(w - t, 0f)
            lineTo(w, h / 2f)
            lineTo(w - t, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            lineTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

val RunwayMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.xs),
    small = RoundedCornerShape(Radii.sm),
    medium = RoundedCornerShape(Radii.md),
    large = RoundedCornerShape(Radii.lg),
    extraLarge = RoundedCornerShape(Radii.xl),
)