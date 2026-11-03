package com.example.runway.ui.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.runway.R
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.theme.RunwayTheme

/**
 * Step 4 of 5, skippable. Monthly committed costs (BR-25). Each amount above 0 becomes a recurring item
 * that first falls due next month (BR-90); Subscriptions is the one the audit screen reviews (F-16).
 */
@Composable
fun CommittedStep(state: OnboardingState, actions: OnboardingActions) {
    OnboardingScaffold(
        step = OnboardingStep.Committed,
        lockup = listOf(
            LockupWord(stringResource(R.string.onboarding_lockup_fixed), WordShape.Box),
            LockupWord(stringResource(R.string.onboarding_lockup_costs), WordShape.Pill),
        ),
        title = stringResource(R.string.onboarding_committed_title),
        subtitle = stringResource(R.string.onboarding_committed_subtitle),
        primaryLabel = stringResource(R.string.onboarding_next),
        onPrimary = actions.onNext,
        submitting = state.submitting,
        onBack = actions.onBack,
        onSkip = actions.onSkip,
    ) {
        AmountField(
            value = state.rent,
            onValueChange = actions.onRentChange,
            label = stringResource(R.string.onboarding_rent_label),
            errorText = state.amountError(OnboardingField.RENT),
        )
        AmountField(
            value = state.utilities,
            onValueChange = actions.onUtilitiesChange,
            label = stringResource(R.string.onboarding_utilities_label),
            errorText = state.amountError(OnboardingField.UTILITIES),
        )
        AmountField(
            value = state.gym,
            onValueChange = actions.onGymChange,
            label = stringResource(R.string.onboarding_gym_label),
            errorText = state.amountError(OnboardingField.GYM),
        )
        AmountField(
            value = state.subscriptions,
            onValueChange = actions.onSubscriptionsChange,
            label = stringResource(R.string.onboarding_subscriptions_label),
            helperText = stringResource(R.string.onboarding_subscriptions_helper),
            errorText = state.amountError(OnboardingField.SUBSCRIPTIONS),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CommittedStepPreview() {
    RunwayTheme {
        CommittedStep(
            OnboardingState(step = OnboardingStep.Committed, rent = "34000", utilities = "6000", subscriptions = "5000"),
            OnboardingActions(),
        )
    }
}
