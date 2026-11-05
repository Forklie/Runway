package com.example.runway.ui.feature.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.SmallTopBar
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.monthYearLabel
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.LocalDate

@Composable
fun GoalEditRoute(onDone: () -> Unit, viewModel: GoalEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }
    GoalEditScreen(
        state = state,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onTargetChange = viewModel::onTargetChange,
        onMonthsChange = viewModel::onMonthsChange,
        onSave = viewModel::onSave,
    )
}

/**
 * Form template: small top bar with Back, label above each field, 24dp apart, one Large primary
 * pinned at the bottom that rides the keyboard. Stateless.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalEditScreen(
    state: GoalFormState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onMonthsChange: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val c = RunwayTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg).imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        SmallTopBar(
            title = stringResource(if (state.isNew) R.string.goal_form_title_new else R.string.goal_form_title_edit),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = Dimens.contentMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenMargin)
                .padding(top = Spacing.s2, bottom = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            RunwayTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.goal_form_name),
                placeholder = stringResource(R.string.goal_form_name_placeholder),
                errorText = if (state.nameError) stringResource(R.string.goal_form_name_error) else null,
                enabled = !state.saving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )
            AmountField(
                value = state.target,
                onValueChange = onTargetChange,
                label = stringResource(R.string.goal_form_target),
                errorText = if (state.targetError) stringResource(R.string.amount_error_invalid) else null,
                enabled = !state.saving,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Text(stringResource(R.string.goal_form_when), style = RunwayType.label, color = c.ink)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    GoalFormState.MONTH_CHOICES.forEach { months ->
                        CategoryChip(
                            label = stringResource(R.string.goal_form_months, months),
                            selected = state.months == months,
                            onClick = { onMonthsChange(months) },
                        )
                    }
                }
                val current = state.currentDeadline
                val helper = if (state.months == null && current != null) {
                    stringResource(R.string.goal_form_keep, current.monthYearLabel())
                } else {
                    stringResource(R.string.goal_form_lands, state.deadline.monthYearLabel())
                }
                Text(helper, style = RunwayType.bodySm, color = c.inkMuted)
            }
            state.formError?.let { Text(stringResource(it.messageRes()), style = RunwayType.bodySm, color = c.danger) }
        }
        RunwayButton(
            text = stringResource(R.string.goal_form_save),
            onClick = onSave,
            modifier = Modifier
                .widthIn(max = Dimens.contentMax)
                .padding(horizontal = Dimens.screenMargin)
                .padding(top = Spacing.s3, bottom = Spacing.s4),
            size = ButtonSize.Large,
            fillWidth = true,
            loading = state.saving,
        )
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun GoalEditNewPreview() {
    RunwayTheme {
        GoalEditScreen(GoalFormState(today = LocalDate.of(2026, 11, 3), name = "MacBook Pro M4", target = "490000"), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun GoalEditErrorPreview() {
    RunwayTheme {
        GoalEditScreen(GoalFormState(today = LocalDate.of(2026, 11, 3), target = "abc", nameError = true, targetError = true), {}, {}, {}, {}, {})
    }
}
