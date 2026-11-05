package com.example.runway.ui.feature.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.formatAmount
import com.example.runway.core.money.formatLkr
import com.example.runway.core.result.AppError
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.CategoryKind
import com.example.runway.domain.usecase.CheckinAccount
import com.example.runway.domain.usecase.CheckinSetup
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.CardTitle
import com.example.runway.ui.components.CardTone
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.HowItWorks
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.MoneyText
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayCard
import com.example.runway.ui.components.SmallTopBar
import com.example.runway.ui.components.charts.StepTrack
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.Instant

/** Everything the check-in flow can ask for. */
data class CheckinActions(
    val onRetry: () -> Unit = {},
    val onBalanceChange: (String, String) -> Unit = { _, _ -> },
    val onContinue: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onStepBack: () -> Unit = {},
    val onLogGapsChange: (Boolean) -> Unit = {},
    val onGapCategoryChange: (String) -> Unit = {},
    val onSave: (gapNote: String) -> Unit = {},
)

@Composable
fun CheckinRoute(onDone: () -> Unit, viewModel: CheckinViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view by viewModel.view.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }
    BackHandler(enabled = form.step > 0) { viewModel.onStepBack() }
    CheckinScreen(
        state = state,
        view = view,
        form = form,
        actions = CheckinActions(
            onRetry = viewModel::retry,
            onBalanceChange = viewModel::onBalanceChange,
            onContinue = viewModel::onContinue,
            onSkip = viewModel::onSkipAccount,
            onStepBack = { if (!viewModel.onStepBack()) onDone() },
            onLogGapsChange = viewModel::onLogGapsChange,
            onGapCategoryChange = viewModel::onGapCategoryChange,
            onSave = viewModel::onSave,
        ),
    )
}

/** Flow template: small bar with Back, step track, one question, the input, one pinned primary. */
@Composable
fun CheckinScreen(state: UiState<CheckinSetup>, view: CheckinView?, form: CheckinForm, actions: CheckinActions) {
    val c = RunwayTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg).imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        SmallTopBar(title = stringResource(R.string.checkin_title), onBack = actions.onStepBack)
        when (state) {
            UiState.Loading -> LoadingState(Modifier.weight(1f))
            UiState.Empty -> Message {
                EmptyState(
                    lockup = problemLockup(),
                    title = stringResource(R.string.checkin_empty_title),
                    body = stringResource(R.string.checkin_empty_body),
                )
            }
            is UiState.Error -> Message {
                EmptyState(
                    lockup = problemLockup(),
                    title = stringResource(R.string.error_title),
                    body = stringResource(state.error.messageRes()),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = actions.onRetry,
                )
            }
            is UiState.Success -> if (view == null) LoadingState(Modifier.weight(1f)) else CheckinFlow(state.data, view, form, actions)
        }
    }
}

@Composable
private fun ColumnScope.Message(content: @Composable () -> Unit) {
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ColumnScope.CheckinFlow(setup: CheckinSetup, view: CheckinView, form: CheckinForm, actions: CheckinActions) {
    val rows = view.rows
    val step = form.step.coerceIn(0, rows.size)
    val labels = rows.map { it.item.account.name } + stringResource(R.string.checkin_review_step)
    val gapNote = stringResource(R.string.checkin_gap_note)

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
        if (labels.size <= MAX_STEP_NODES) {
            StepTrack(
                labels = labels,
                current = step,
                description = stringResource(R.string.checkin_steps_description, step + 1, labels.size, labels[step]),
            )
        }
        if (step < rows.size) {
            AccountStep(rows[step], form.entered[rows[step].item.account.id].orEmpty(), form.inputError, actions.onBalanceChange)
        } else {
            ReviewStep(setup, view, form, actions)
        }
    }

    Column(
        modifier = Modifier
            .widthIn(max = Dimens.contentMax)
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenMargin)
            .padding(top = Spacing.s3, bottom = Spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        if (step < rows.size) {
            RunwayButton(stringResource(R.string.checkin_continue), actions.onContinue, size = ButtonSize.Large, fillWidth = true)
            RunwayButton(stringResource(R.string.checkin_skip), actions.onSkip, style = ButtonStyle.Tertiary, size = ButtonSize.Medium)
        } else {
            RunwayButton(
                text = stringResource(R.string.checkin_save),
                onClick = { actions.onSave(gapNote) },
                size = ButtonSize.Large,
                fillWidth = true,
                loading = form.saving,
            )
        }
    }
}

/** One question per screen. The difference is explained in plain words under the field. */
@Composable
private fun AccountStep(row: CheckinRow, typed: String, inputError: Boolean, onBalanceChange: (String, String) -> Unit) {
    val c = RunwayTheme.colors
    val account = row.item.account
    val question = when (account.kind) {
        AccountKind.CASH -> stringResource(R.string.checkin_question_cash)
        AccountKind.GOAL_FUND -> stringResource(R.string.checkin_question_fund, account.name)
        AccountKind.BANK, AccountKind.SECONDARY -> stringResource(R.string.checkin_question_bank, account.name)
    }
    val gap = row.gapMinor
    val explanation = if (gap == null) stringResource(R.string.checkin_expected, row.item.expectedMinor.formatLkr()) else gapSentence(gap, row.item.expectedMinor)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(question, style = RunwayType.title1, color = c.ink, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.checkin_hint), style = RunwayType.body, color = c.inkMuted)
    }
    AmountField(
        value = typed,
        onValueChange = { onBalanceChange(account.id, it) },
        label = stringResource(R.string.checkin_balance_label),
        errorText = if (inputError) stringResource(R.string.checkin_balance_error) else null,
        helperText = explanation,
    )
}

@Composable
private fun gapSentence(gap: Long, expectedMinor: Long): String = when {
    gap > 0 -> stringResource(R.string.checkin_gap_positive, expectedMinor.formatAmount(), gap.formatAmount())
    gap < 0 -> stringResource(R.string.checkin_gap_negative, (-gap).formatAmount())
    else -> stringResource(R.string.checkin_gap_none)
}

/** All accounts, their gaps, and the one decision: log the unaccounted amount, or leave it (BR-84). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewStep(setup: CheckinSetup, view: CheckinView, form: CheckinForm, actions: CheckinActions) {
    val c = RunwayTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(stringResource(R.string.checkin_review_title), style = RunwayType.title1, color = c.ink, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.checkin_review_body), style = RunwayType.body, color = c.inkMuted)
    }
    RunwayCard {
        view.rows.forEach { row ->
            val entered = row.enteredMinor
            val gap = row.gapMinor
            val line = when {
                entered == null || gap == null -> stringResource(R.string.checkin_row_skipped)
                gap > 0 -> stringResource(R.string.checkin_row_gap, entered.formatLkr(), gap.formatAmount())
                gap < 0 -> stringResource(R.string.checkin_row_extra, entered.formatLkr(), (-gap).formatAmount())
                else -> stringResource(R.string.checkin_row_match, entered.formatLkr())
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.item.account.name, style = RunwayType.label, color = c.ink)
                    Text(line, style = RunwayType.bodySm, color = c.inkMuted)
                }
                if (entered != null) MoneyText(entered)
            }
        }
        HowItWorks(stringResource(R.string.checkin_how))
    }

    if (view.unloggedMinor > 0) {
        RunwayCard(tone = CardTone.Note) {
            CardTitle(stringResource(R.string.checkin_total_gap, view.unloggedMinor.formatLkr()))
            Text(stringResource(R.string.checkin_choice_question), style = RunwayType.bodySm, color = c.ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                CategoryChip(stringResource(R.string.checkin_choice_leave), selected = !form.logGaps, onClick = { actions.onLogGapsChange(false) })
                CategoryChip(stringResource(R.string.checkin_choice_log), selected = form.logGaps, onClick = { actions.onLogGapsChange(true) })
            }
            if (form.logGaps) {
                Text(stringResource(R.string.checkin_category_label), style = RunwayType.label, color = c.ink)
                val chosen = form.gapCategoryId ?: CheckinViewModel.OTHER_CATEGORY
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    setup.categories.filter { it.kind == CategoryKind.DISCRETIONARY }.forEach { category ->
                        CategoryChip(category.name, selected = chosen == category.id, onClick = { actions.onGapCategoryChange(category.id) })
                    }
                }
            }
        }
    }
    form.saveError?.let {
        Text(
            text = stringResource(it.messageRes()),
            style = RunwayType.bodySm,
            color = c.danger,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

private const val MAX_STEP_NODES = 5

// ---- Previews: Kavindu's Sunday check-in (the design system's cash-check example) ----

private val previewAt: Instant = Instant.parse("2026-11-01T03:00:00Z")

private fun previewAccount(id: String, name: String, kind: AccountKind, expected: Long) =
    CheckinAccount(Account(id, name, kind, 0, 0, previewAt, false, previewAt), expected)

private val previewAccounts = listOf(
    previewAccount("bank-main", "Main bank", AccountKind.BANK, 9_010_000),
    previewAccount("cash", "Cash", AccountKind.CASH, 570_000),
    previewAccount("goal-fund", "MacBook fund", AccountKind.GOAL_FUND, 18_640_000),
)

private val previewSetup = CheckinSetup(
    accounts = previewAccounts,
    categories = listOf(
        Category("food-coffee", "Food and Coffee", "restaurant", CategoryKind.DISCRETIONARY, 4, false),
        Category("other", "Other", "more_horiz", CategoryKind.DISCRETIONARY, 8, false),
    ),
    lastCheckin = null,
)

private fun previewView(vararg entered: Long?) = CheckinView(
    rows = previewAccounts.mapIndexed { i, item ->
        val typed = entered.getOrNull(i)
        CheckinRow(item, typed, typed?.let { item.expectedMinor - it }, skipped = false)
    },
    unloggedMinor = previewAccounts.mapIndexed { i, item -> entered.getOrNull(i)?.let { item.expectedMinor - it } ?: 0L }
        .sumOf { it.coerceAtLeast(0L) },
)

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun CheckinCashStepPreview() {
    RunwayTheme {
        CheckinScreen(UiState.Success(previewSetup), previewView(null, 450_000), CheckinForm(step = 1, entered = mapOf("cash" to "4500")), CheckinActions())
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun CheckinReviewPreview() {
    RunwayTheme {
        CheckinScreen(
            UiState.Success(previewSetup),
            previewView(8_400_000, 450_000, 18_640_000),
            CheckinForm(step = 3, logGaps = true),
            CheckinActions(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckinErrorPreview() {
    RunwayTheme { CheckinScreen(UiState.Error(AppError.Network), null, CheckinForm(), CheckinActions()) }
}
