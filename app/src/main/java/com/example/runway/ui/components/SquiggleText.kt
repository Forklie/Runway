package com.example.runway.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType

/**
 * Body text with the coral wavy underline under one phrase (design system: "when the projection moves
 * the goal date, put the coral wavy underline under the phrase that says so"). No red, no warning icon.
 * The wave follows the phrase across line breaks.
 */
@Composable
fun SquiggleText(
    text: String,
    squiggle: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = RunwayType.bodySm,
    color: Color = RunwayTheme.colors.ink,
    squiggleColor: Color = RunwayTheme.colors.goal,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val start = squiggle?.takeIf { it.isNotEmpty() }?.let { text.indexOf(it) } ?: -1
    val end = if (start >= 0) start + (squiggle?.length ?: 0) else -1

    Text(
        text = text,
        style = style,
        color = color,
        onTextLayout = { layout = it },
        modifier = modifier
            .padding(bottom = 4.dp)
            .drawBehind {
                val result = layout ?: return@drawBehind
                if (start < 0) return@drawBehind
                val amplitude = 1.5.dp.toPx()
                val halfWave = 3.dp.toPx()
                for (line in result.getLineForOffset(start)..result.getLineForOffset(end - 1)) {
                    val from = result.getHorizontalPosition(maxOf(start, result.getLineStart(line)), true)
                    val to = result.getHorizontalPosition(minOf(end, result.getLineEnd(line, visibleEnd = true)), true)
                    val y = result.getLineBottom(line) + 1.dp.toPx()
                    val path = Path().apply {
                        moveTo(from, y)
                        var x = from
                        var up = true
                        while (x < to) {
                            val next = minOf(x + halfWave, to)
                            quadraticTo((x + next) / 2, if (up) y - amplitude else y + amplitude, next, y)
                            x = next
                            up = !up
                        }
                    }
                    drawPath(path, squiggleColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
            },
    )
}

@Preview(showBackground = true, widthDp = 300)
@Composable
private fun SquiggleTextPreview() {
    RunwayTheme {
        SquiggleText(
            text = "At your pace you land in Jul 2027, a month after your plan.",
            squiggle = "a month after your plan",
        )
    }
}
