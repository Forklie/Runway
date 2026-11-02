package com.example.runway.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType

data class DonutSlice(val valueMinor: Long, val color: Color)

/**
 * Committed vs discretionary for the month, drawn on Canvas (no chart library). Slices start at
 * 12 o'clock and run clockwise with a small gap between them. With nothing spent it draws only the
 * track, so an empty month is calm rather than broken. One image for TalkBack, described by [description].
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerValue: String,
    centerLabel: String,
    description: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 136.dp,
    thickness: Dp = 16.dp,
) {
    val c = RunwayTheme.colors
    val total = slices.sumOf { it.valueMinor.coerceAtLeast(0L) }
    val visible = slices.filter { it.valueMinor > 0 }

    Box(
        modifier = modifier.size(diameter).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(c.surfaceSunken, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            if (total == 0L) return@Canvas

            val gap = if (visible.size > 1) GAP_DEGREES else 0f
            var start = -90f
            visible.forEach { slice ->
                val sweep = 360f * slice.valueMinor / total
                drawArc(
                    color = slice.color,
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(centerValue, style = RunwayType.money, color = c.ink, maxLines = 1)
            Text(centerLabel, style = RunwayType.caption, color = c.inkMuted, maxLines = 1)
        }
    }
}

private const val GAP_DEGREES = 3f

@Preview(showBackground = true)
@Composable
private fun DonutChartPreview() {
    RunwayTheme {
        val c = RunwayTheme.colors
        DonutChart(
            slices = listOf(DonutSlice(4_500_000, c.accent), DonutSlice(7_400_000, c.highlight)),
            centerValue = "119,000",
            centerLabel = "spent",
            description = "Committed 45,000 rupees, 38 percent. Discretionary 74,000 rupees, 62 percent.",
        )
    }
}
