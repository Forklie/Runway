package com.example.runway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType

/**
 * Pill tags (design system, Tag): OnPace is fine, NeedsLook is yellow and never red ("Behind pace",
 * "Overdue"), Done is quiet, Goal is anything about the goal, Pending is saved on the phone only.
 */
enum class TagTone { OnPace, NeedsLook, Done, Goal, Pending }

/** A 24dp status label. Always carries a word: colour is never the only signal. Never danger. */
@Composable
fun StatusTag(text: String, tone: TagTone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = RunwayTheme.colors
    val (fill, content) = when (tone) {
        TagTone.OnPace -> c.action to c.onAction
        TagTone.NeedsLook -> c.highlight to c.onHighlight
        TagTone.Done -> c.accentSoft to c.accent
        TagTone.Goal -> c.goal to c.onGoal
        TagTone.Pending -> Color.Transparent to c.inkMuted
    }
    Row(
        modifier = modifier
            .height(24.dp)
            .background(fill, CircleShape)
            .then(if (tone == TagTone.Pending) Modifier.border(Dimens.hairline, c.lineStrong, CircleShape) else Modifier)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = content)
        Text(text, style = RunwayType.caption.copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}
