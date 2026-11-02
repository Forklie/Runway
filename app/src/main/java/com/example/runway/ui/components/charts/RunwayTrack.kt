package com.example.runway.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/** Surface cards use ink and line colours; the azul hero panel uses its on-brand pair. */
enum class TrackTone { OnSurface, OnBrand }

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The goal runway (design system "Runway track"): a 2dp rail with a dot per month, an 8dp coral fill
 * up to the saved share, a 24dp now-node on it and the goal node at the end. Always shown next to the
 * numbers it draws: the coral is decoration for the number, never a replacement for it.
 * One image for TalkBack, described by [description].
 */
@Composable
fun GoalRunwayTrack(
    progress: Float,
    months: Int,
    startLabel: String,
    endLabel: String,
    description: String,
    modifier: Modifier = Modifier,
    tone: TrackTone = TrackTone.OnSurface,
) {
    val c = RunwayTheme.colors
    val rail = if (tone == TrackTone.OnBrand) c.onBrandMuted else c.lineStrong
    val nodeGround = if (tone == TrackTone.OnBrand) c.brandBlock else c.surface
    val endNode = if (tone == TrackTone.OnBrand) c.onBrand else c.ink
    val labels = if (tone == TrackTone.OnBrand) c.onBrandMuted else c.inkMuted

    // Slides out from the start once per composition, then follows changes (360ms, emphasized).
    val shown = remember { Animatable(0f) }
    LaunchedEffect(progress) { shown.animateTo(progress.coerceIn(0f, 1f), tween(360, easing = Emphasized)) }

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val radius = 12.dp.toPx()
            val y = size.height / 2
            val start = radius
            val end = size.width - radius
            val now = start + (end - start) * shown.value

            drawLine(rail, Offset(start, y), Offset(end, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            for (month in 1 until months.coerceAtMost(MAX_TICKS)) {
                drawCircle(rail, radius = 3.dp.toPx(), center = Offset(start + (end - start) * month / months, y))
            }
            drawLine(c.goal, Offset(start, y), Offset(now, y), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
            ringNode(endNode, nodeGround, Offset(end, y), centre = 4.dp.toPx())
            ringNode(c.goal, nodeGround, Offset(now, y), centre = 6.dp.toPx())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, style = RunwayType.caption, color = labels)
            Text(endLabel, style = RunwayType.caption, color = labels)
        }
    }
}

/** ⊙: a 24dp disc in the ground colour, a 2dp ring and a filled centre. */
private fun DrawScope.ringNode(color: Color, ground: Color, center: Offset, centre: Float) {
    val radius = 12.dp.toPx()
    drawCircle(ground, radius = radius, center = center)
    drawCircle(color, radius = radius - 1.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
    drawCircle(color, radius = centre, center = center)
}

private const val MAX_TICKS = 37 // one dot a month up to three years, then the rail stays clean

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun GoalRunwayTrackPreview() {
    RunwayTheme {
        GoalRunwayTrack(
            progress = 0.38f,
            months = 12,
            startLabel = "Jun 2026",
            endLabel = "38% · Jun 2027",
            description = "186,400 of 490,000 rupees saved, 38 percent. Planned for June 2027.",
        )
    }
}
