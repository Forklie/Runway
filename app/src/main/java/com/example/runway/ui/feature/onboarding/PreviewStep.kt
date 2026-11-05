package com.example.runway.ui.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.runway.R
import com.example.runway.core.money.formatLkr
import com.example.runway.core.result.AppError
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.MoneyText
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.feature.auth.AuthErrorBanner
import com.example.runway.domain.usecase.DayZeroPreview
import com.example.runway.domain.usecase.Feasibility
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * Step 5 of 5: value before data (FR-06). Shows the numbers ComputeDayZeroPreview worked out, in calm
 * words. The primary button finishes onboarding. Nothing here calculates: it only formats.
 */
@Composable
fun PreviewStep(state: OnboardingState, actions: OnboardingActions) {
    OnboardingScaffold(
        step = OnboardingStep.Preview,
        lockup = listOf(
            LockupWord(stringResource(R.string.onboarding_lockup_day), WordShape.Box),
            LockupWord(stringResource(R.string.onboarding_lockup_zero), WordShape.Goal),
        ),
        title = stringResource(R.string.onboarding_preview_title),
        subtitle = stringResource(R.string.onboarding_preview_subtitle),
        primaryLabel = stringResource(R.string.onboarding_finish),
        onPrimary = actions.onNext,
        submitting = state.submitting,
        onBack = actions.onBack,
    ) {
        state.preview?.let { PreviewCard(it, state.goalName, state.goalMonths) }
        state.error?.let { AuthErrorBanner(stringResource(it.messageRes())) }
    }
}

@Composable
private fun PreviewCard(preview: DayZeroPreview, goalName: String, months: Int) {
    val colors = RunwayTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.accentSoft, RunwayShape.Card)
            .padding(Dimens.cardPadding)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Text(text = goalName.uppercase(), style = RunwayType.eyebrow, color = colors.accent)

        if (preview.requiredPerMonthMinor == 0L) {
            Text(text = stringResource(R.string.onboarding_preview_done), style = RunwayType.title2, color = colors.ink)
        } else {
            MoneyText(
                amountMinor = preview.requiredPerMonthMinor,
                style = RunwayType.moneyLg,
                showCurrency = true,
            )
            Text(
                text = stringResource(R.string.onboarding_preview_per_month, months),
                style = RunwayType.bodySm,
                color = colors.inkMuted,
            )
        }

        val share = preview.sharePercent
        if (share != null) {
            Text(text = stringResource(R.string.onboarding_preview_share, share), style = RunwayType.body, color = colors.ink)
        } else {
            Text(text = stringResource(R.string.onboarding_preview_no_salary), style = RunwayType.body, color = colors.ink)
        }

        preview.leftAfterRentMinor?.let {
            Text(
                text = stringResource(R.string.onboarding_preview_after_rent, it.formatLkr()),
                style = RunwayType.body,
                color = colors.ink,
            )
        }
        preview.leftAfterRentAndGoalMinor?.takeIf { it >= 0L }?.let {
            Text(
                text = stringResource(R.string.onboarding_preview_after_goal, it.formatLkr()),
                style = RunwayType.body,
                color = colors.ink,
            )
        }

        preview.estimateGapMinor?.let { gap ->
            Text(
                text = if (gap > 0L) {
                    stringResource(R.string.onboarding_preview_estimate_above, gap.formatLkr())
                } else {
                    stringResource(R.string.onboarding_preview_estimate_even)
                },
                style = RunwayType.bodySm,
                color = colors.inkMuted,
            )
        }

        when (preview.feasibility) {
            Feasibility.FITS -> Text(
                text = stringResource(R.string.onboarding_preview_fits),
                style = RunwayType.label,
                color = colors.accent,
            )
            Feasibility.STRETCH -> Text(
                text = stringResource(R.string.onboarding_preview_stretch),
                style = RunwayType.bodySm,
                color = colors.ink,
            )
            Feasibility.UNKNOWN -> Unit
        }
    }
}

private val previewFits = DayZeroPreview(
    requiredPerMonthMinor = 3_990_000,
    sharePercent = 33,
    leftAfterRentMinor = 8_600_000,
    leftAfterRentAndGoalMinor = 4_610_000,
    estimateGapMinor = 9_000_000,
    feasibility = Feasibility.FITS,
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PreviewStepFitsPreview() {
    RunwayTheme {
        PreviewStep(
            OnboardingState(step = OnboardingStep.Preview, goalName = "MacBook", preview = previewFits),
            OnboardingActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PreviewStepStretchPreview() {
    RunwayTheme {
        PreviewStep(
            OnboardingState(
                step = OnboardingStep.Preview,
                goalName = "MacBook",
                preview = previewFits.copy(
                    leftAfterRentAndGoalMinor = -1_000_000,
                    feasibility = Feasibility.STRETCH,
                    estimateGapMinor = null,
                ),
            ),
            OnboardingActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PreviewStepNoSalaryPreview() {
    RunwayTheme {
        PreviewStep(
            OnboardingState(
                step = OnboardingStep.Preview,
                goalName = "MacBook",
                preview = DayZeroPreview(3_990_000, null, null, null, null, Feasibility.UNKNOWN),
                error = AppError.Unknown(),
            ),
            OnboardingActions(),
        )
    }
}
