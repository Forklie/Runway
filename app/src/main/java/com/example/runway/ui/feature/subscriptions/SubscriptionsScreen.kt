package com.example.runway.ui.feature.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.formatLkr
import com.example.runway.core.result.AppError
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.feature.auth.AuthErrorBanner
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

data class SubscriptionsActions(
    val onBack: () -> Unit = {},
    val onKeep: (String) -> Unit = {},
    val onCancel: (String) -> Unit = {},
    val onRestore: (String) -> Unit = {},
    val onDismissError: () -> Unit = {},
)

/** Stateful wrapper: the only part that touches Hilt. */
@Composable
fun SubscriptionsRoute(onBack: () -> Unit, viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val error by viewModel.actionError.collectAsStateWithLifecycle()
    SubscriptionsScreen(
        state = state,
        actionError = error,
        actions = SubscriptionsActions(
            onBack = onBack,
            onKeep = viewModel::onKeep,
            onCancel = viewModel::onCancel,
            onRestore = viewModel::onRestore,
            onDismissError = viewModel::onDismissError,
        ),
    )
}

/** Draws all four UiState cases. */
@Composable
fun SubscriptionsScreen(
    state: UiState<SubscriptionsContent>,
    actionError: AppError?,
    actions: SubscriptionsActions,
) {
    val colors = RunwayTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(Dimens.topBar).padding(horizontal = Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = actions.onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.subscriptions_back),
                    tint = colors.ink,
                )
            }
            Text(
                text = stringResource(R.string.subscriptions_title),
                style = RunwayType.title2,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
        }

        when (state) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> EmptyState(
                lockup = listOf(
                    LockupWord(stringResource(R.string.subscriptions_lockup_no), WordShape.Box),
                    LockupWord(stringResource(R.string.subscriptions_lockup_subs), WordShape.PillOutline),
                ),
                title = stringResource(R.string.subscriptions_empty_title),
                body = stringResource(R.string.subscriptions_empty_body),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            is UiState.Error -> EmptyState(
                lockup = problemLockup(),
                title = stringResource(R.string.error_title),
                body = stringResource(state.error.messageRes()),
                actionLabel = stringResource(R.string.subscriptions_back),
                onAction = actions.onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            is UiState.Success -> AuditList(state.data, actionError, actions)
        }
    }
}

@Composable
private fun AuditList(content: SubscriptionsContent, actionError: AppError?, actions: SubscriptionsActions) {
    val colors = RunwayTheme.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = Dimens.contentMax)
            .padding(horizontal = Dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Spacing.s8),
    ) {
        if (actionError != null) {
            item(key = "error") { AuthErrorBanner(stringResource(actionError.messageRes())) }
        }

        item(key = "summary") { SummaryCard(content) }

        if (content.active.isNotEmpty()) {
            item(key = "active-heading") {
                SectionTitle(stringResource(R.string.subscriptions_active_heading), Modifier.padding(top = Spacing.s3))
            }
            items(content.active, key = { it.id }) { row ->
                ActiveRow(row, actions)
            }
        }

        if (content.cancelled.isNotEmpty()) {
            item(key = "cancelled-heading") {
                SectionTitle(stringResource(R.string.subscriptions_cancelled_heading), Modifier.padding(top = Spacing.s3))
            }
            items(content.cancelled, key = { it.id }) { row ->
                CancelledRow(row, actions)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RunwayType.title2,
        color = RunwayTheme.colors.ink,
        modifier = modifier.semantics { heading() },
    )
}

/** Total of what is still active, and, once something is marked cancelled, the annual saving (BR-92, BR-93). */
@Composable
private fun SummaryCard(content: SubscriptionsContent) {
    val colors = RunwayTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.accentSoft, RunwayShape.Card)
            .padding(Dimens.cardPadding)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        Text(
            text = stringResource(R.string.subscriptions_summary_monthly, content.monthlyTotalMinor.formatLkr()),
            style = RunwayType.title1,
            color = colors.ink,
        )
        Text(
            text = stringResource(R.string.subscriptions_summary_yearly, content.yearlyTotalMinor.formatLkr()),
            style = RunwayType.body,
            color = colors.inkMuted,
        )
        if (content.yearlySavedMinor > 0L) {
            Text(
                text = stringResource(R.string.subscriptions_saved_total, content.yearlySavedMinor.formatLkr()),
                style = RunwayType.label,
                color = colors.accent,
                modifier = Modifier.padding(top = Spacing.s2),
            )
        }
    }
}

@Composable
private fun RowCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RunwayTheme.colors.surface, RunwayShape.Card)
            .padding(Dimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) { content() }
}

@Composable
private fun CostLine(row: SubscriptionRow) {
    val monthly = row.monthlyLkrMinor
    val yearly = row.yearlyLkrMinor
    Text(
        text = if (monthly != null && yearly != null) {
            stringResource(R.string.subscriptions_row_cost, monthly.formatLkr(), yearly.formatLkr())
        } else {
            stringResource(R.string.subscriptions_cost_unknown)
        },
        style = RunwayType.bodySm,
        color = RunwayTheme.colors.inkMuted,
    )
}

/** The "still using this?" prompt (BR-93). Yes writes nothing. No marks it cancelled. */
@Composable
private fun ActiveRow(row: SubscriptionRow, actions: SubscriptionsActions) {
    val colors = RunwayTheme.colors
    RowCard {
        Text(row.name, style = RunwayType.headline, color = colors.ink)
        CostLine(row)
        if (row.kept) {
            Text(stringResource(R.string.subscriptions_keeping), style = RunwayType.label, color = colors.accent)
            RunwayButton(
                text = stringResource(R.string.subscriptions_cancel_instead),
                onClick = { actions.onCancel(row.id) },
                style = ButtonStyle.Tertiary,
                size = ButtonSize.Small,
            )
        } else {
            Text(stringResource(R.string.subscriptions_prompt), style = RunwayType.label, color = colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                RunwayButton(
                    text = stringResource(R.string.subscriptions_yes),
                    onClick = { actions.onKeep(row.id) },
                    style = ButtonStyle.Tonal,
                    size = ButtonSize.Small,
                )
                RunwayButton(
                    text = stringResource(R.string.subscriptions_no),
                    onClick = { actions.onCancel(row.id) },
                    style = ButtonStyle.Secondary,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

/** Honest copy: the app only records the decision, the provider still has to be told. */
@Composable
private fun CancelledRow(row: SubscriptionRow, actions: SubscriptionsActions) {
    val colors = RunwayTheme.colors
    RowCard {
        Text(row.name, style = RunwayType.headline, color = colors.inkMuted)
        row.yearlyLkrMinor?.let {
            Text(
                text = stringResource(R.string.subscriptions_cancelled_row, it.formatLkr()),
                style = RunwayType.bodySm,
                color = colors.ink,
            )
        }
        RunwayButton(
            text = stringResource(R.string.subscriptions_undo),
            onClick = { actions.onRestore(row.id) },
            style = ButtonStyle.Tertiary,
            size = ButtonSize.Small,
        )
    }
}

// ---- Previews: Success, saving shown, Empty, Loading, Error ----

private val previewContent = SubscriptionsContent(
    active = listOf(
        SubscriptionRow("a", "Streaming", 150_000, 1_800_000, kept = false),
        SubscriptionRow("b", "Music", 50_000, 600_000, kept = true),
        SubscriptionRow("c", "Design tool", null, null, kept = false),
    ),
    cancelled = listOf(SubscriptionRow("d", "News", 90_000, 1_080_000, kept = false)),
    monthlyTotalMinor = 200_000,
    yearlyTotalMinor = 2_400_000,
    yearlySavedMinor = 1_080_000,
)

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SubscriptionsSuccessPreview() {
    RunwayTheme { SubscriptionsScreen(UiState.Success(previewContent), null, SubscriptionsActions()) }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun SubscriptionsEmptyPreview() {
    RunwayTheme { SubscriptionsScreen(UiState.Empty, null, SubscriptionsActions()) }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun SubscriptionsLoadingPreview() {
    RunwayTheme { SubscriptionsScreen(UiState.Loading, null, SubscriptionsActions()) }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun SubscriptionsErrorPreview() {
    RunwayTheme { SubscriptionsScreen(UiState.Error(AppError.Network), null, SubscriptionsActions()) }
}
