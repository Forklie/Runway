package com.example.runway.ui.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.WordLockup
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * Shared frame of the five steps, built like AuthScaffold: a 64dp top bar (Back on the left, "Skip for
 * now" on the right), a segmented progress bar, word lockup, title and a calm line, then the fields
 * 24dp apart on 20dp margins. One Large primary is pinned at the bottom and rises with the keyboard.
 * Stateless: it only draws what it is given.
 */
@Composable
fun OnboardingScaffold(
    step: OnboardingStep,
    lockup: List<LockupWord>,
    title: String,
    subtitle: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    submitting: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RunwayTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(Dimens.topBar).padding(horizontal = Spacing.s2)) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    enabled = !submitting,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.onboarding_back),
                        tint = colors.ink,
                    )
                }
            }
            if (onSkip != null) {
                RunwayButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = onSkip,
                    style = ButtonStyle.Tertiary,
                    size = ButtonSize.Small,
                    enabled = !submitting,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }

        StepProgress(
            current = step.number,
            total = OnboardingStep.COUNT,
            modifier = Modifier
                .widthIn(max = Dimens.contentMax)
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenMargin),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = Dimens.contentMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenMargin)
                .padding(top = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            WordLockup(lockup)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Text(
                    text = title,
                    style = RunwayType.title1,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(text = subtitle, style = RunwayType.body, color = colors.inkMuted)
            }
            content()
        }

        Column(
            modifier = Modifier
                .widthIn(max = Dimens.contentMax)
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenMargin)
                .padding(top = Spacing.s3, bottom = Spacing.s4),
        ) {
            RunwayButton(
                text = primaryLabel,
                onClick = onPrimary,
                size = ButtonSize.Large,
                fillWidth = true,
                loading = submitting,
            )
        }
    }
}

/** Five pills, filled up to the current step. TalkBack reads one phrase: "Step 2 of 5". */
@Composable
private fun StepProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = RunwayTheme.colors
    val description = stringResource(R.string.onboarding_step_counter, current, total)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Spacing.s1 + 2.dp)
                    .background(if (index < current) colors.accent else colors.line, CircleShape),
            )
        }
    }
}
