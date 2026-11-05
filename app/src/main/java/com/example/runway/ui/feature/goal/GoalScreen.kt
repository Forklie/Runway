package com.example.runway.ui.feature.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.formatAmount
import com.example.runway.core.money.formatLkr
import com.example.runway.core.result.AppError
import com.example.runway.domain.usecase.GoalProjection
import com.example.runway.domain.usecase.GoalRunway
import com.example.runway.domain.usecase.GoalStatus
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.CardTitle
import com.example.runway.ui.components.CardTone
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.EdgePill
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.HeroAmount
import com.example.runway.ui.components.HeroIconButton
import com.example.runway.ui.components.HeroPanel
import com.example.runway.ui.components.HowItWorks
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.MoneyText
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayCard
import com.example.runway.ui.components.RunwaySheet
import com.example.runway.ui.components.RunwaySnackbarHost
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.SquiggleText
import com.example.runway.ui.components.StatusBarIconsOnBrand
import com.example.runway.ui.components.StatusTag
import com.example.runway.ui.components.TagTone
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.components.charts.GoalRunwayTrack
import com.example.runway.ui.components.charts.TrackTone
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.monthYearLabel
import com.example.runway.ui.components.monthYearSpoken
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.components.showUndo
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.temporal.ChronoUnit

/** Everything the Goal screen can ask for. The ViewModel answers; the screen only draws. */
data class GoalScreenActions(
    val onEditGoal: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenWhatIf: () -> Unit = {},
    val onWhatIfLabelChange: (String) -> Unit = {},
    val onWhatIfAmountChange: (String) -> Unit = {},
    val onSubmitWhatIf: () -> Unit = {},
    val onDismissWhatIf: () -> Unit = {},
    val onOpenAddToFund: () -> Unit = {},
    val onFundAmountChange: (String) -> Unit = {},
    val onFundFromChange: (String) -> Unit = {},
    val onSubmitAddToFund: () -> Unit = {},
    val onDismissSheet: () -> Unit = {},
)

/** Stateful wrapper: the only part that touches Hilt. Turns one-off events into snackbars. */
@Composable
fun GoalRoute(onEditGoal: () -> Unit, viewModel: GoalViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val forms by viewModel.forms.collectAsStateWithLifecycle()
    val whatIf by viewModel.whatIf.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GoalEvent.AddedToFund -> {
                    val result = snackbar.showUndo(
                        message = context.getString(R.string.fund_added, event.amountMinor.formatLkr(), event.fundName),
                        actionLabel = context.getString(R.string.fund_undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoAddToFund(event.transactionId)
                }
                is GoalEvent.Failed -> snackbar.showSnackbar(context.getString(event.error.messageRes()))
            }
        }
    }

    GoalScreen(
        state = state,
        forms = forms,
        whatIf = whatIf,
        snackbar = snackbar,
        actions = GoalScreenActions(
            onEditGoal = onEditGoal,
            onRetry = viewModel::retry,
            onOpenWhatIf = viewModel::onOpenWhatIf,
            onWhatIfLabelChange = viewModel::onWhatIfLabelChange,
            onWhatIfAmountChange = viewModel::onWhatIfAmountChange,
            onSubmitWhatIf = viewModel::onSubmitWhatIf,
            onDismissWhatIf = viewModel::onDismissWhatIf,
            onOpenAddToFund = viewModel::onOpenAddToFund,
            onFundAmountChange = viewModel::onFundAmountChange,
            onFundFromChange = viewModel::onFundFromChange,
            onSubmitAddToFund = viewModel::onSubmitAddToFund,
            onDismissSheet = viewModel::onDismissSheet,
        ),
    )
}

/** Stateless: previews and tests without Hilt or Firebase. */
@Composable
fun GoalScreen(
    state: UiState<GoalRunway>,
    forms: GoalForms,
    whatIf: WhatIfResult?,
    snackbar: SnackbarHostState,
    actions: GoalScreenActions,
) {
    Box(Modifier.fillMaxSize().background(RunwayTheme.colors.bg)) {
        when (state) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
                EmptyState(
                    lockup = listOf(
                        LockupWord(stringResource(R.string.lockup_set_a), WordShape.Box),
                        LockupWord(stringResource(R.string.lockup_goal), WordShape.Goal),
                    ),
                    title = stringResource(R.string.goal_empty_title),
                    body = stringResource(R.string.goal_empty_body),
                    actionLabel = stringResource(R.string.goal_empty_action),
                    onAction = actions.onEditGoal,
                )
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
                EmptyState(
                    lockup = problemLockup(),
                    title = stringResource(R.string.error_title),
                    body = stringResource(state.error.messageRes()),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = actions.onRetry,
                )
            }
            is UiState.Success -> {
                GoalContent(state.data, whatIf, actions)
                GoalSheets(state.data, forms, actions)
            }
        }
        RunwaySnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** Dashboard template: hero with the runway, two tonal actions, the what-if answer, the pace card. */
@Composable
private fun GoalContent(runway: GoalRunway, whatIf: WhatIfResult?, actions: GoalScreenActions) {
    val listState = rememberLazyListState()
    val heroGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    StatusBarIconsOnBrand(onBrand = !heroGone)

    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = Spacing.s16)) {
        item(key = "hero") { GoalHero(runway, actions.onEditGoal) }
        item(key = "actions") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenMargin).padding(top = Spacing.s4),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                RunwayButton(
                    text = stringResource(R.string.goal_add_to_fund),
                    onClick = actions.onOpenAddToFund,
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.Tonal,
                    leadingIcon = Icons.Rounded.Add,
                    enabled = runway.fromAccounts.isNotEmpty(),
                )
                RunwayButton(
                    text = stringResource(R.string.goal_what_if),
                    onClick = actions.onOpenWhatIf,
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.Tonal,
                    leadingIcon = Icons.AutoMirrored.Rounded.HelpOutline,
                )
            }
        }
        if (whatIf != null) {
            item(key = "whatif") {
                WhatIfCard(whatIf, runway.goal.name, actions.onDismissWhatIf, Modifier.padding(horizontal = Dimens.screenMargin).padding(top = Spacing.s6))
            }
        }
        item(key = "pace") {
            PaceCard(runway.projection, Modifier.padding(horizontal = Dimens.screenMargin).padding(top = if (whatIf != null) Spacing.s3 else Spacing.s6))
        }
    }
}

@Composable
private fun GoalHero(runway: GoalRunway, onEditGoal: () -> Unit) {
    val c = RunwayTheme.colors
    val p = runway.projection
    val months = ChronoUnit.MONTHS.between(p.startDate, p.deadline).toInt().coerceAtLeast(1)
    HeroPanel(
        topRow = {
            Text(
                text = stringResource(R.string.goal_title),
                style = RunwayType.title2,
                color = c.onBrand,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            HeroIconButton(Icons.Rounded.Edit, stringResource(R.string.goal_edit), onEditGoal)
        },
        pill = { EdgePill(text = pillText(p), icon = Icons.Rounded.Flag, fill = c.goal, content = c.onGoal) },
    ) {
        Text(
            text = runway.goal.name.uppercase(),
            style = RunwayType.eyebrow,
            color = c.onBrandMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.s2),
        )
        HeroAmount(
            amountMinor = p.savedMinor,
            spoken = stringResource(R.string.goal_hero_spoken, p.savedMinor.formatAmount(), p.targetMinor.formatAmount(), runway.goal.name),
            modifier = Modifier.padding(vertical = Spacing.s1),
        )
        Text(stringResource(R.string.goal_hero_of, p.targetMinor.formatAmount(), p.percent), style = RunwayType.bodySm, color = c.onBrandMuted)
        Spacer(Modifier.height(Spacing.s5))
        GoalRunwayTrack(
            progress = p.progress,
            months = months,
            startLabel = p.startDate.monthYearLabel(),
            endLabel = p.deadline.monthYearLabel(),
            description = stringResource(
                R.string.goal_track_description,
                p.savedMinor.formatAmount(),
                p.targetMinor.formatAmount(),
                p.percent,
                p.deadline.monthYearSpoken(),
            ),
            tone = TrackTone.OnBrand,
        )
    }
}

/** The coral pill: when he lands, in under 24 characters where it can be. */
@Composable
private fun pillText(p: GoalProjection): String {
    val landing = p.projectedDate
    return when {
        p.status == GoalStatus.REACHED -> stringResource(R.string.goal_pill_reached)
        p.paceMinorPerMonth == null -> stringResource(R.string.goal_pill_not_enough)
        landing == null -> stringResource(R.string.goal_pill_needs, p.requiredPerMonthMinor.formatLkr())
        else -> stringResource(R.string.goal_pill_landing, landing.monthYearLabel())
    }
}

/** Required rate, actual pace, months left, the projection line and how it was worked out (FR-14). */
@Composable
private fun PaceCard(p: GoalProjection, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    val (statusText, tone) = when (p.status) {
        GoalStatus.AHEAD -> stringResource(R.string.goal_status_ahead) to TagTone.OnPace
        GoalStatus.ON_TRACK -> stringResource(R.string.goal_status_on_track) to TagTone.OnPace
        GoalStatus.BEHIND -> stringResource(R.string.goal_status_behind) to TagTone.NeedsLook
        GoalStatus.NOT_ENOUGH_DATA -> stringResource(R.string.goal_status_not_enough) to TagTone.Pending
        GoalStatus.REACHED -> stringResource(R.string.goal_status_reached) to TagTone.Goal
    }
    val line = projectionLine(p)
    RunwayCard(modifier) {
        CardTitle(stringResource(R.string.goal_pace_title), trailing = { StatusTag(statusText, tone) })
        LabelValue(stringResource(R.string.goal_needed_label), p.requiredPerMonthMinor.formatLkr())
        LabelValue(
            label = stringResource(R.string.goal_pace_label),
            value = p.paceMinorPerMonth?.let { stringResource(R.string.goal_pace_value, it.formatLkr()) }
                ?: stringResource(R.string.goal_pace_unknown),
        )
        LabelValue(stringResource(R.string.goal_months_left_label), p.monthsLeft.toString())
        SquiggleText(text = line.text, squiggle = line.squiggle, color = c.ink)
        HowItWorks(stringResource(R.string.goal_pace_how, p.targetMinor.formatLkr(), p.savedMinor.formatLkr(), p.monthsLeft))
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    val c = RunwayTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = RunwayType.bodySm, color = c.inkMuted, modifier = Modifier.weight(1f))
        Text(value, style = RunwayType.money, color = c.ink)
    }
}

/** Goal-message card: what a purchase does to the goal date, in days, without judgement (BR-75). */
@Composable
private fun WhatIfCard(result: WhatIfResult, goalName: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    val phrase = when (result.delayDays) {
        0 -> null
        1 -> stringResource(R.string.whatif_phrase_one)
        else -> stringResource(R.string.whatif_phrase_many, result.delayDays)
    }
    val sentence = when (result.delayDays) {
        0 -> stringResource(R.string.whatif_no_effect)
        1 -> stringResource(R.string.whatif_delay_one, goalName)
        else -> stringResource(R.string.whatif_delay_many, goalName, result.delayDays)
    }
    RunwayCard(modifier, tone = CardTone.GoalMessage) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.whatif_eyebrow).uppercase(),
                style = RunwayType.eyebrow,
                color = c.ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            StatusTag(stringResource(R.string.whatif_goal_tag), TagTone.Goal)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            MoneyText(result.priceMinor, style = RunwayType.moneyLg, showCurrency = true, modifier = Modifier.alignByBaseline())
            if (result.label.isNotBlank()) {
                Text(result.label, style = RunwayType.body, color = c.ink, modifier = Modifier.alignByBaseline())
            }
        }
        SquiggleText(text = sentence, squiggle = phrase, style = RunwayType.body, color = c.ink)
        HowItWorks(stringResource(R.string.whatif_how))
        RunwayButton(stringResource(R.string.whatif_not_now), onDismiss, style = ButtonStyle.Tertiary, size = ButtonSize.Small)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalSheets(runway: GoalRunway, forms: GoalForms, actions: GoalScreenActions) {
    val c = RunwayTheme.colors
    when (forms.sheet) {
        null -> Unit
        GoalSheet.WhatIf -> RunwaySheet(title = stringResource(R.string.whatif_sheet_title), onDismiss = actions.onDismissSheet) {
            Text(stringResource(R.string.whatif_sheet_body), style = RunwayType.body, color = c.inkMuted)
            RunwayTextField(
                value = forms.whatIfLabel,
                onValueChange = actions.onWhatIfLabelChange,
                label = stringResource(R.string.whatif_label),
                placeholder = stringResource(R.string.whatif_label_placeholder),
            )
            AmountField(
                value = forms.whatIfAmount,
                onValueChange = actions.onWhatIfAmountChange,
                label = stringResource(R.string.whatif_price),
                errorText = if (forms.whatIfAmountError) stringResource(R.string.amount_error_invalid) else null,
            )
            RunwayButton(stringResource(R.string.whatif_submit), actions.onSubmitWhatIf, size = ButtonSize.Large, fillWidth = true)
        }
        GoalSheet.AddToFund -> RunwaySheet(title = stringResource(R.string.fund_sheet_title), onDismiss = actions.onDismissSheet) {
            Text(stringResource(R.string.fund_sheet_body, runway.fundName), style = RunwayType.body, color = c.inkMuted)
            AmountField(
                value = forms.fundAmount,
                onValueChange = actions.onFundAmountChange,
                label = stringResource(R.string.fund_amount),
                errorText = if (forms.fundAmountError) stringResource(R.string.amount_error_invalid) else null,
            )
            Text(stringResource(R.string.fund_from), style = RunwayType.label, color = c.ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                runway.fromAccounts.forEach { balance ->
                    CategoryChip(
                        label = balance.account.name,
                        selected = forms.fundFromId == balance.account.id,
                        onClick = { actions.onFundFromChange(balance.account.id) },
                        meta = balance.balanceMinor.formatAmount(),
                    )
                }
            }
            if (forms.fundFromError) {
                Text(stringResource(R.string.fund_error_account), style = RunwayType.bodySm, color = c.danger)
            }
            RunwayButton(stringResource(R.string.fund_submit), actions.onSubmitAddToFund, size = ButtonSize.Large, fillWidth = true)
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun GoalBehindPreview() {
    RunwayTheme {
        GoalScreen(
            state = UiState.Success(previewGoalRunway()),
            forms = GoalForms(),
            whatIf = WhatIfResult("Headphones", 1_250_000, 9),
            snackbar = remember { SnackbarHostState() },
            actions = GoalScreenActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun GoalDayZeroPreview() {
    RunwayTheme {
        GoalScreen(UiState.Success(previewGoalRunway(dayZero = true)), GoalForms(), null, remember { SnackbarHostState() }, GoalScreenActions())
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalEmptyPreview() {
    RunwayTheme { GoalScreen(UiState.Empty, GoalForms(), null, remember { SnackbarHostState() }, GoalScreenActions()) }
}

@Preview(showBackground = true)
@Composable
private fun GoalErrorPreview() {
    RunwayTheme { GoalScreen(UiState.Error(AppError.Network), GoalForms(), null, remember { SnackbarHostState() }, GoalScreenActions()) }
}

@Preview(showBackground = true)
@Composable
private fun GoalLoadingPreview() {
    RunwayTheme { GoalScreen(UiState.Loading, GoalForms(), null, remember { SnackbarHostState() }, GoalScreenActions()) }
}
