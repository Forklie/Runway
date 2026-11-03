package com.example.runway.ui.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.result.AppError
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.GoalSettings
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.MoneyText
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.feature.auth.AuthErrorBanner
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Every user action on Settings, so the screen stays stateless and previewable. */
data class SettingsActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onSaveName: () -> Unit = {},
    val onEditGoal: () -> Unit = {},
    val onGoalNameChange: (String) -> Unit = {},
    val onGoalTargetChange: (String) -> Unit = {},
    val onGoalMonthsChange: (Int) -> Unit = {},
    val onSaveGoal: () -> Unit = {},
    val onDismissGoal: () -> Unit = {},
    val onDefaultAccountSelected: (String) -> Unit = {},
    val onPyfChange: (String) -> Unit = {},
    val onSavePyf: () -> Unit = {},
    val onSignOut: () -> Unit = {},
)

/** Stateful wrapper: the only part that touches Hilt. */
@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        form = form,
        actions = SettingsActions(
            onBack = onBack,
            onNameChange = viewModel::onNameChange,
            onSaveName = viewModel::onSaveName,
            onEditGoal = viewModel::onEditGoal,
            onGoalNameChange = viewModel::onGoalNameChange,
            onGoalTargetChange = viewModel::onGoalTargetChange,
            onGoalMonthsChange = viewModel::onGoalMonthsChange,
            onSaveGoal = viewModel::onSaveGoal,
            onDismissGoal = viewModel::onDismissGoal,
            onDefaultAccountSelected = viewModel::onDefaultAccountSelected,
            onPyfChange = viewModel::onPyfChange,
            onSavePyf = viewModel::onSavePyf,
            onSignOut = viewModel::onSignOut,
        ),
    )
}

/** Draws all four UiState cases. The goal sheet sits on top when the form has an edit open. */
@Composable
fun SettingsScreen(state: UiState<SettingsContent>, form: SettingsForm, actions: SettingsActions) {
    val colors = RunwayTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        SettingsTopBar(onBack = actions.onBack)
        when (state) {
            UiState.Loading, UiState.Empty -> LoadingState()
            is UiState.Error -> EmptyState(
                lockup = problemLockup(),
                title = stringResource(R.string.error_title),
                body = stringResource(state.error.messageRes()),
                actionLabel = stringResource(R.string.settings_back),
                onAction = actions.onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            is UiState.Success -> SettingsContentView(state.data, form, actions)
        }
    }

    form.goalEdit?.let { GoalEditSheet(it, actions) }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val colors = RunwayTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(Dimens.topBar).padding(horizontal = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = colors.ink,
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = RunwayType.title2,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContentView(content: SettingsContent, form: SettingsForm, actions: SettingsActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .widthIn(max = Dimens.contentMax)
            .padding(horizontal = Dimens.screenMargin)
            .padding(bottom = Spacing.s8),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap),
    ) {
        form.actionError?.let { AuthErrorBanner(stringResource(it.messageRes())) }

        // Profile
        Section(R.string.settings_profile_heading) {
            RunwayTextField(
                value = form.nameDraft ?: content.displayName,
                onValueChange = actions.onNameChange,
                label = stringResource(R.string.settings_name_label),
                helperText = content.email,
                errorText = if (form.nameError) stringResource(R.string.settings_name_error) else null,
            )
            RunwayButton(
                text = stringResource(R.string.settings_save),
                onClick = actions.onSaveName,
                style = ButtonStyle.Tonal,
                enabled = form.nameDraft != null,
            )
        }

        // Goal
        Section(R.string.settings_goal_heading) {
            val goal = content.goal
            if (goal == null) {
                Text(stringResource(R.string.settings_goal_none), style = RunwayType.body, color = RunwayTheme.colors.inkMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    Text(goal.name, style = RunwayType.headline, color = RunwayTheme.colors.ink)
                    MoneyText(goal.targetMinor, style = RunwayType.moneyLg, showCurrency = true)
                    Text(
                        text = stringResource(R.string.settings_goal_deadline, goal.deadline.toMonthYear()),
                        style = RunwayType.bodySm,
                        color = RunwayTheme.colors.inkMuted,
                    )
                }
                RunwayButton(
                    text = stringResource(R.string.settings_goal_edit),
                    onClick = actions.onEditGoal,
                    style = ButtonStyle.Secondary,
                )
            }
        }

        // Default account
        Section(R.string.settings_account_heading, helper = R.string.settings_account_helper) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                content.accounts.forEach { account ->
                    CategoryChip(
                        label = account.name,
                        selected = account.id == content.defaultAccountId,
                        onClick = { actions.onDefaultAccountSelected(account.id) },
                    )
                }
            }
        }

        // PYF
        Section(R.string.settings_pyf_heading) {
            RunwayTextField(
                value = form.pyfDraft ?: content.pyfPercent.toPercentText(),
                onValueChange = actions.onPyfChange,
                label = stringResource(R.string.settings_pyf_label),
                helperText = stringResource(R.string.settings_pyf_helper),
                errorText = if (form.pyfError) stringResource(R.string.settings_pyf_error) else null,
                prefix = "%",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            RunwayButton(
                text = stringResource(R.string.settings_save),
                onClick = actions.onSavePyf,
                style = ButtonStyle.Tonal,
                enabled = form.pyfDraft != null,
            )
        }

        RunwayButton(
            text = stringResource(R.string.settings_sign_out),
            onClick = actions.onSignOut,
            style = ButtonStyle.Danger,
            size = ButtonSize.Large,
            fillWidth = true,
        )
    }
}

/** A titled group with an optional one-line helper. */
@Composable
private fun Section(
    @StringRes titleRes: Int,
    @StringRes helper: Int? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Text(
            text = stringResource(titleRes),
            style = RunwayType.title2,
            color = RunwayTheme.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        if (helper != null) {
            Text(text = stringResource(helper), style = RunwayType.bodySm, color = RunwayTheme.colors.inkMuted)
        }
        content()
    }
}

/** The goal editor: name, target and an optional new deadline. Nothing is written until Save. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GoalEditSheet(edit: GoalEditForm, actions: SettingsActions) {
    val colors = RunwayTheme.colors
    ModalBottomSheet(
        onDismissRequest = actions.onDismissGoal,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RunwayShape.Sheet,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screenMargin)
                .padding(bottom = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            Text(
                text = stringResource(R.string.settings_goal_sheet_title),
                style = RunwayType.title1,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            RunwayTextField(
                value = edit.name,
                onValueChange = actions.onGoalNameChange,
                label = stringResource(R.string.settings_goal_name_label),
                errorText = if (edit.nameError) stringResource(R.string.onboarding_error_goal_name) else null,
            )
            AmountField(
                value = edit.target,
                onValueChange = actions.onGoalTargetChange,
                label = stringResource(R.string.settings_goal_target_label),
                errorText = if (edit.targetError) stringResource(R.string.amount_error_invalid) else null,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Text(stringResource(R.string.settings_goal_months_label), style = RunwayType.label, color = colors.ink)
                Text(stringResource(R.string.settings_goal_months_helper), style = RunwayType.bodySm, color = colors.inkMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    listOf(6, 12, 18, 24).forEach { months ->
                        CategoryChip(
                            label = stringResource(R.string.settings_goal_months_chip, months),
                            selected = edit.months == months,
                            onClick = { actions.onGoalMonthsChange(months) },
                        )
                    }
                }
            }
            RunwayButton(
                text = stringResource(R.string.settings_save),
                onClick = actions.onSaveGoal,
                size = ButtonSize.Large,
                fillWidth = true,
            )
        }
    }
}

private val DeadlineFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
private val AppZone = ZoneId.of("Asia/Colombo") // same fixed zone as the injected Clock

private fun Instant.toMonthYear(): String = DeadlineFormat.format(atZone(AppZone))

/** 30.0 -> "30", 12.5 -> "12.5": what a person would type. */
internal fun Double.toPercentText(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

// ---- Previews: Success, Loading, Error, no goal ----

private val previewContent = SettingsContent(
    displayName = "Kavindu",
    email = "kavindu@example.com",
    goal = GoalSettings(
        name = "MacBook",
        targetMinor = 49_000_000,
        startDate = Instant.parse("2026-10-30T00:00:00Z"),
        deadline = Instant.parse("2027-10-30T00:00:00Z"),
        fundAccountId = "goal-fund",
        active = true,
    ),
    accounts = listOf(
        Account("bank-main", "Main bank", AccountKind.BANK, 0, 0, Instant.EPOCH, false, Instant.EPOCH),
        Account("cash", "Cash", AccountKind.CASH, 0, 0, Instant.EPOCH, false, Instant.EPOCH),
    ),
    defaultAccountId = "bank-main",
    pyfPercent = 30.0,
)

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun SettingsSuccessPreview() {
    RunwayTheme { SettingsScreen(UiState.Success(previewContent), SettingsForm(), SettingsActions()) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun SettingsEditingPreview() {
    RunwayTheme {
        SettingsScreen(
            UiState.Success(previewContent),
            SettingsForm(nameDraft = "Kavindu S", pyfDraft = "150", pyfError = true),
            SettingsActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun SettingsNoGoalPreview() {
    RunwayTheme { SettingsScreen(UiState.Success(previewContent.copy(goal = null)), SettingsForm(), SettingsActions()) }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun SettingsLoadingPreview() {
    RunwayTheme { SettingsScreen(UiState.Loading, SettingsForm(), SettingsActions()) }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun SettingsErrorPreview() {
    RunwayTheme { SettingsScreen(UiState.Error(AppError.Network), SettingsForm(), SettingsActions()) }
}
