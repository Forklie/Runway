package com.example.runway.ui.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.runway.R
import com.example.runway.domain.model.OnboardingIncomeKind
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * Step 2 of 5, skippable. Salary is the RELIABLE source (BR-40). The estimate is his own guess, kept so
 * the income-reality screen can compare it with what really arrives (BR-44). The ticked extras are VARIABLE (BR-41).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomeStep(state: OnboardingState, actions: OnboardingActions) {
    OnboardingScaffold(
        step = OnboardingStep.Income,
        lockup = listOf(
            LockupWord(stringResource(R.string.onboarding_lockup_your), WordShape.Box),
            LockupWord(stringResource(R.string.onboarding_lockup_income), WordShape.Pill),
        ),
        title = stringResource(R.string.onboarding_income_title),
        subtitle = stringResource(R.string.onboarding_income_subtitle),
        primaryLabel = stringResource(R.string.onboarding_next),
        onPrimary = actions.onNext,
        submitting = state.submitting,
        onBack = actions.onBack,
        onSkip = actions.onSkip,
    ) {
        AmountField(
            value = state.salaryBase,
            onValueChange = actions.onSalaryBaseChange,
            label = stringResource(R.string.onboarding_salary_label),
            helperText = stringResource(R.string.onboarding_salary_helper),
            errorText = state.amountError(OnboardingField.SALARY_BASE),
        )
        RunwayTextField(
            value = state.salaryDay,
            onValueChange = actions.onSalaryDayChange,
            label = stringResource(R.string.onboarding_pay_day_label),
            helperText = stringResource(R.string.onboarding_pay_day_helper),
            errorText = if (OnboardingField.SALARY_DAY in state.fieldErrors) {
                stringResource(R.string.onboarding_error_pay_day)
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )
        AmountField(
            value = state.ownEstimate,
            onValueChange = actions.onOwnEstimateChange,
            label = stringResource(R.string.onboarding_estimate_label),
            helperText = stringResource(R.string.onboarding_estimate_helper),
            errorText = state.amountError(OnboardingField.OWN_ESTIMATE),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Text(
                text = stringResource(R.string.onboarding_sources_label),
                style = RunwayType.label,
                color = RunwayTheme.colors.ink,
            )
            Text(
                text = stringResource(R.string.onboarding_sources_helper),
                style = RunwayType.bodySm,
                color = RunwayTheme.colors.inkMuted,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                OnboardingIncomeKind.entries.forEach { kind ->
                    CategoryChip(
                        label = stringResource(kind.labelRes()),
                        selected = kind in state.variableSources,
                        onClick = { actions.onToggleSource(kind) },
                        showCheck = true, // filter chip: several can be ticked
                    )
                }
            }
        }
    }
}

private fun OnboardingIncomeKind.labelRes(): Int = when (this) {
    OnboardingIncomeKind.FREELANCE -> R.string.onboarding_source_freelance
    OnboardingIncomeKind.ADSENSE -> R.string.onboarding_source_adsense
    OnboardingIncomeKind.CRYPTO -> R.string.onboarding_source_crypto
    OnboardingIncomeKind.OTHER -> R.string.onboarding_source_other
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun IncomeStepPreview() {
    RunwayTheme {
        IncomeStep(
            OnboardingState(
                step = OnboardingStep.Income,
                salaryBase = "120000",
                ownEstimate = "210000",
                variableSources = setOf(OnboardingIncomeKind.ADSENSE),
            ),
            OnboardingActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun IncomeStepErrorPreview() {
    RunwayTheme {
        IncomeStep(
            OnboardingState(step = OnboardingStep.Income, salaryBase = "12.345", fieldErrors = setOf(OnboardingField.SALARY_BASE)),
            OnboardingActions(),
        )
    }
}
