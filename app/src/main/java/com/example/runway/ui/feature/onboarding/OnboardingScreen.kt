package com.example.runway.ui.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.domain.model.OnboardingIncomeKind

/** Every user action of the five steps in one place, so the step composables stay stateless and previewable. */
data class OnboardingActions(
    val onGoalNameChange: (String) -> Unit = {},
    val onGoalTargetChange: (String) -> Unit = {},
    val onGoalMonthsChange: (Int) -> Unit = {},
    val onSalaryBaseChange: (String) -> Unit = {},
    val onSalaryDayChange: (String) -> Unit = {},
    val onOwnEstimateChange: (String) -> Unit = {},
    val onToggleSource: (OnboardingIncomeKind) -> Unit = {},
    val onBankChange: (String) -> Unit = {},
    val onCashChange: (String) -> Unit = {},
    val onGoalFundChange: (String) -> Unit = {},
    val onRentChange: (String) -> Unit = {},
    val onUtilitiesChange: (String) -> Unit = {},
    val onGymChange: (String) -> Unit = {},
    val onSubscriptionsChange: (String) -> Unit = {},
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSkip: () -> Unit = {},
)

/** Stateful wrapper: the only part that touches Hilt. Nothing to navigate to, the session routing does that. */
@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        actions = OnboardingActions(
            onGoalNameChange = viewModel::onGoalNameChange,
            onGoalTargetChange = viewModel::onGoalTargetChange,
            onGoalMonthsChange = viewModel::onGoalMonthsChange,
            onSalaryBaseChange = viewModel::onSalaryBaseChange,
            onSalaryDayChange = viewModel::onSalaryDayChange,
            onOwnEstimateChange = viewModel::onOwnEstimateChange,
            onToggleSource = viewModel::onToggleVariableSource,
            onBankChange = viewModel::onBankBalanceChange,
            onCashChange = viewModel::onCashBalanceChange,
            onGoalFundChange = viewModel::onGoalFundBalanceChange,
            onRentChange = viewModel::onRentChange,
            onUtilitiesChange = viewModel::onUtilitiesChange,
            onGymChange = viewModel::onGymChange,
            onSubscriptionsChange = viewModel::onSubscriptionsChange,
            onNext = viewModel::onNext,
            onBack = viewModel::onBack,
            onSkip = viewModel::onSkip,
        ),
    )
}

/** Picks the step to draw. System Back walks to the previous step instead of leaving the app. */
@Composable
fun OnboardingScreen(state: OnboardingState, actions: OnboardingActions) {
    BackHandler(enabled = state.step != OnboardingStep.Goal && !state.submitting) { actions.onBack() }
    when (state.step) {
        OnboardingStep.Goal -> GoalStep(state, actions)
        OnboardingStep.Income -> IncomeStep(state, actions)
        OnboardingStep.Accounts -> AccountsStep(state, actions)
        OnboardingStep.Committed -> CommittedStep(state, actions)
        OnboardingStep.Preview -> PreviewStep(state, actions)
    }
}

/** The inline message for an optional amount, or null when the field is fine. */
@Composable
internal fun OnboardingState.amountError(field: OnboardingField): String? =
    if (field in fieldErrors) stringResource(R.string.onboarding_error_amount) else null
