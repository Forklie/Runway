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
 * Step 3 of 5, skippable. Opening balances of the three accounts created at sign-up. The goal fund
 * balance is "saved so far" (BR-71) and every balance becomes that account's first checkpoint (BR-32).
 */
@Composable
fun AccountsStep(state: OnboardingState, actions: OnboardingActions) {
    OnboardingScaffold(
        step = OnboardingStep.Accounts,
        lockup = listOf(
            LockupWord(stringResource(R.string.onboarding_lockup_where), WordShape.Box),
            LockupWord(stringResource(R.string.onboarding_lockup_it_sits), WordShape.Pill),
        ),
        title = stringResource(R.string.onboarding_accounts_title),
        subtitle = stringResource(R.string.onboarding_accounts_subtitle),
        primaryLabel = stringResource(R.string.onboarding_next),
        onPrimary = actions.onNext,
        submitting = state.submitting,
        onBack = actions.onBack,
        onSkip = actions.onSkip,
    ) {
        AmountField(
            value = state.bankBalance,
            onValueChange = actions.onBankChange,
            label = stringResource(R.string.onboarding_bank_label),
            errorText = state.amountError(OnboardingField.BANK),
        )
        AmountField(
            value = state.cashBalance,
            onValueChange = actions.onCashChange,
            label = stringResource(R.string.onboarding_cash_label),
            errorText = state.amountError(OnboardingField.CASH),
        )
        AmountField(
            value = state.goalFundBalance,
            onValueChange = actions.onGoalFundChange,
            label = stringResource(R.string.onboarding_goal_fund_label),
            helperText = stringResource(R.string.onboarding_goal_fund_helper),
            errorText = state.amountError(OnboardingField.GOAL_FUND),
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AccountsStepPreview() {
    RunwayTheme {
        AccountsStep(
            OnboardingState(step = OnboardingStep.Accounts, bankBalance = "85000", goalFundBalance = "11200"),
            OnboardingActions(),
        )
    }
}
