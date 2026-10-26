package com.example.runway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.ui.theme.ArrowTagShape
import com.example.runway.ui.theme.Brand
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

enum class WordShape { Box, Pill, PillOutline, Arrow, Goal }

data class LockupWord(val text: String, val shape: WordShape)

/** Words set in brand shapes instead of illustrations. Decorative, so hidden from TalkBack. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordLockup(words: List<LockupWord>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        words.forEach { WordChip(it) }
    }
}

private data class WordLook(
    val shape: Shape,
    val fill: Color,
    val text: Color,
    val border: Color?,
    val endPadding: Dp = 14.dp,
)

@Composable
private fun WordChip(word: LockupWord) {
    val c = RunwayTheme.colors
    val look = when (word.shape) {
        WordShape.Box -> WordLook(RunwayShape.Keycap, Color.Transparent, c.accent, c.accent)
        WordShape.Pill -> WordLook(CircleShape, c.action, c.onAction, null)
        WordShape.PillOutline -> WordLook(CircleShape, Color.Transparent, c.accent, c.accent)
        WordShape.Arrow -> WordLook(ArrowTagShape(tip = 16.dp), Brand.Rosa, Brand.Midnight, null, endPadding = 26.dp)
        WordShape.Goal -> WordLook(CircleShape, c.goal, c.onGoal, null)
    }
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(look.shape)
            .background(look.fill)
            .then(if (look.border != null) Modifier.border(Dimens.stroke, look.border, look.shape) else Modifier)
            .padding(start = 14.dp, end = look.endPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(word.text, style = RunwayType.word, color = look.text, maxLines = 1)
    }
}

/**
 * Lockup, title (what is true now), body (what happens next, no guilt), optional one action.
 * Never says "You haven't..." or "You forgot...".
 */
@Composable
fun EmptyState(
    lockup: List<LockupWord>,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = RunwayTheme.colors
    Column(
        modifier = modifier
            .widthIn(max = 340.dp)
            .padding(horizontal = Spacing.s6, vertical = Spacing.s10),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s3, Alignment.CenterVertically),
    ) {
        WordLockup(lockup, Modifier.padding(bottom = Spacing.s3))
        Text(
            text = title,
            style = RunwayType.title2,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = body, style = RunwayType.body, color = c.inkMuted, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            RunwayButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = Spacing.s3))
        }
    }
}

@Composable
fun offlineLockup(): List<LockupWord> = listOf(
    LockupWord(stringResource(R.string.lockup_saved), WordShape.Box),
    LockupWord(stringResource(R.string.lockup_on_this_phone), WordShape.Pill),
)

@Composable
fun problemLockup(): List<LockupWord> = listOf(
    LockupWord(stringResource(R.string.lockup_that), WordShape.Box),
    LockupWord(stringResource(R.string.lockup_didnt_load), WordShape.Pill),
)