package com.example.runway.ui.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.example.runway.R
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/** Step 1 of 5, the only required one: goal name, target and how many months to take (BR-70). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalStep(state: OnboardingState, actions: OnboardingActions) {
    val focus = LocalFocusManager.current
    OnboardingScaffold(
        step = OnboardingStep.Goal,
        lockup = listOf(
            LockupWord(stringResource(R.string.onboarding_lockup_your), WordShape.Box),
            LockupWord(stringResource(R.string.onboarding_lockup_goal), WordShape.Goal),
        ),
        title = stringResource(R.string.onboarding_goal_title),
        subtitle = stringResource(R.string.onboarding_goal_subtitle),
        primaryLabel = stringResource(R.string.onboarding_next),
        onPrimary = actions.onNext,
        submitting = state.submitting,
    ) {
        RunwayTextField(
            value = state.goalName,
            onValueChange = actions.onGoalNameChange,
            label = stringResource(R.string.onboarding_goal_name_label),
            placeholder = stringResource(R.string.onboarding_goal_name_placeholder),
            errorText = if (OnboardingField.GOAL_NAME in state.fieldErrors) {
                stringResource(R.string.onboarding_error_goal_name)
            } else null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
        )
        AmountField(
            value = state.goalTarget,
            onValueChange = actions.onGoalTargetChange,
            label = stringResource(R.string.onboarding_goal_target_label),
            errorText = if (OnboardingField.GOAL_TARGET in state.fieldErrors) {
                stringResource(R.string.amount_error_invalid)
            } else null,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Text(
                text = stringResource(R.string.onboarding_goal_months_label),
                style = RunwayType.label,
                color = RunwayTheme.colors.ink,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                OnboardingState.MONTH_CHOICES.forEach { months ->
                    CategoryChip(
                        label = stringResource(R.string.onboarding_months_chip, months),
                        selected = state.goalMonths == months,
                        onClick = { actions.onGoalMonthsChange(months) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun GoalStepEmptyPreview() {
    RunwayTheme { GoalStep(OnboardingState(), OnboardingActions()) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun GoalStepErrorPreview() {
    RunwayTheme {
        GoalStep(
            OnboardingState(goalName = "", goalTarget = "0", fieldErrors = setOf(OnboardingField.GOAL_NAME, OnboardingField.GOAL_TARGET)),
            OnboardingActions(),
        )
    }
}
