package com.example.runway.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/** Standard: surface. Note: a gentle prompt (welcome back, a gap). GoalMessage: what-if results. */
enum class CardTone { Standard, Note, GoalMessage }

/**
 * Flat card that holds one idea: title, the number, one line of context, at most one action.
 * No shadow, no border: it is told apart from bg by its fill. Never azul (that is the hero's).
 */
@Composable
fun RunwayCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Standard,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = RunwayTheme.colors
    val fill = when (tone) {
        CardTone.Standard -> c.surface
        CardTone.Note -> c.highlightSoft
        CardTone.GoalMessage -> c.goalSoft
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(fill, RunwayShape.Card)
            .padding(Dimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        content = content,
    )
}

/** Card title as a TalkBack heading, so users can jump card to card. [trailing] holds one tag. */
@Composable
fun CardTitle(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = RunwayType.headline,
            color = RunwayTheme.colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing?.invoke()
    }
}

/**
 * Every number that comes from a formula shows how it was worked out (design system, Card rules).
 * Collapsed by default; the open state survives rotation.
 */
@Composable
fun HowItWorks(explanation: String, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Row(
            modifier = Modifier
                .heightIn(min = Dimens.touchTarget)
                .clickable(role = Role.Button, onClick = { open = !open }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.accent)
            Text(stringResource(R.string.ui_how_it_works), style = RunwayType.label, color = c.accent)
        }
        AnimatedVisibility(visible = open) {
            Text(explanation, style = RunwayType.bodySm, color = c.inkMuted)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RunwayCardPreview() {
    RunwayTheme {
        Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            RunwayCard {
                CardTitle("Income reality", trailing = { StatusTag("Last 3 months", TagTone.Done) })
                Text("Safe to Spend only counts salary.", style = RunwayType.bodySm)
                HowItWorks("Average of the last 3 complete months, per source.")
            }
            RunwayCard(tone = CardTone.Note) { CardTitle("Welcome back") }
            RunwayCard(tone = CardTone.GoalMessage) { CardTitle("What if you buy it?") }
        }
    }
}
