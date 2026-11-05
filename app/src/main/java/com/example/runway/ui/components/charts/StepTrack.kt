package com.example.runway.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * Steps version of the runway track (onboarding, weekly check-in). Done: filled accent with a check.
 * Current: accent ring and centre. Next: hollow line-strong ring. At most 5 nodes: a longer flow is
 * too long. Each half-line belongs to the node it leads into, so the halves join between columns.
 */
@Composable
fun StepTrack(
    labels: List<String>,
    current: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    val c = RunwayTheme.colors
    fun lineInto(index: Int): Color = if (index <= current) c.accent else c.lineStrong

    Row(modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        labels.forEachIndexed { index, label ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                    Row(Modifier.fillMaxWidth()) {
                        HalfLine(if (index > 0) lineInto(index) else Color.Transparent, Modifier.weight(1f))
                        HalfLine(if (index < labels.lastIndex) lineInto(index + 1) else Color.Transparent, Modifier.weight(1f))
                    }
                    StepNode(state = (index - current).coerceIn(-1, 1))
                }
                Text(
                    text = label,
                    style = RunwayType.caption,
                    color = if (index == current) c.ink else c.inkMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HalfLine(color: Color, modifier: Modifier) {
    Box(modifier.height(2.dp).background(color))
}

/** state: -1 done, 0 current, 1 next. */
@Composable
private fun StepNode(state: Int) {
    val c = RunwayTheme.colors
    when (state) {
        -1 -> Box(Modifier.size(24.dp).background(c.accent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = c.surface)
        }
        0 -> Box(
            Modifier.size(24.dp).background(c.bg, CircleShape).border(2.dp, c.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(10.dp).background(c.accent, CircleShape))
        }
        else -> Box(Modifier.size(24.dp).background(c.bg, CircleShape).border(2.dp, c.lineStrong, CircleShape))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StepTrackPreview() {
    RunwayTheme {
        StepTrack(
            labels = listOf("Bank", "Cash", "Goal fund", "Review"),
            current = 1,
            description = "Weekly check-in, step 2 of 4, Cash",
        )
    }
}
