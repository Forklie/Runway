package com.example.runway.ui.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.formatLkr
import com.example.runway.core.result.AppError
import com.example.runway.domain.usecase.HomeSummary
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.StatusBarIconsOnBrand
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/** Every way out of Home. A null action hides its button until the feature behind it exists. */
data class HomeActions(
    val onOpenGoal: () -> Unit = {},
    val onSeeActivity: () -> Unit = {},
    val onAddExpense: (() -> Unit)? = null,
    val onCheckin: (() -> Unit)? = null,
    val onIncome: (() -> Unit)? = null,
    val onTransfer: (() -> Unit)? = null,
    val onFreelance: (() -> Unit)? = null,
    val onSettings: (() -> Unit)? = null,
    val overdueCard: (@Composable () -> Unit)? = null, // A's overdue invoices (BR-53); draws nothing when none are overdue
)

/** Stateful wrapper: the only part that touches Hilt. */
@Composable
fun HomeRoute(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, actions = actions, onRetry = viewModel::retry)
}

/** Stateless: plain state and lambdas, so it previews and tests without Hilt or Firebase. */
@Composable
fun HomeScreen(
    state: UiState<HomeSummary>,
    actions: HomeActions,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(RunwayTheme.colors.bg)) {
        when (state) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> Centred {
                EmptyState(
                    lockup = nothingYetLockup(),
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    actionLabel = actions.onAddExpense?.let { stringResource(R.string.home_empty_action) },
                    onAction = actions.onAddExpense,
                )
            }
            is UiState.Error -> Centred {
                EmptyState(
                    lockup = problemLockup(),
                    title = stringResource(R.string.error_title),
                    body = stringResource(state.error.messageRes()),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry,
                )
            }
            is UiState.Success -> HomeContent(state.data, actions)
        }
    }
}

/**
 * Dashboard template, in the design system's fixed order: Safe to Spend, actions, prompt, goal,
 * this month, today. When the hero scrolls away a compact bar keeps the number on screen.
 */
@Composable
private fun HomeContent(summary: HomeSummary, actions: HomeActions) {
    val listState = rememberLazyListState()
    val heroGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val onCheckin = actions.onCheckin
    val prompt = summary.prompt.takeIf { onCheckin != null }
    StatusBarIconsOnBrand(onBrand = !heroGone)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = Spacing.s16)) {
            item(key = "hero") { SafeToSpendHero(summary, actions.onSettings) }
            item(key = "actions") { ActionRow(actions) }
            if (prompt != null && onCheckin != null) {
                item(key = "prompt") {
                    PromptCard(prompt, onCheckin, Modifier.padding(horizontal = Dimens.screenMargin).padding(top = Spacing.s6))
                }
            }
            actions.overdueCard?.let { card -> item(key = "overdue") { card() } }
            item(key = "goal") {
                val topGap = if (prompt != null) Spacing.s3 else Spacing.s6
                Box(Modifier.padding(horizontal = Dimens.screenMargin).padding(top = topGap)) {
                    val goal = summary.goal
                    val projection = summary.projection
                    if (goal != null && projection != null) {
                        GoalSummaryCard(goal.name, projection, actions.onOpenGoal)
                    } else {
                        SetGoalCard(actions.onOpenGoal)
                    }
                }
            }
            item(key = "month") { MonthSection(summary) }
            item(key = "today") { TodaySection(summary, actions) }
        }
        AnimatedVisibility(visible = heroGone, enter = fadeIn(), exit = fadeOut()) {
            CompactSafeBar(summary.safeToSpend.perDay)
        }
    }
}

/** The 64dp surface bar that fades in once the hero has scrolled away, so the number never leaves. */
@Composable
private fun CompactSafeBar(perDay: Long) {
    val c = RunwayTheme.colors
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        Box(
            modifier = Modifier.statusBarsPadding().height(Dimens.topBar).padding(horizontal = Dimens.screenMargin),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.home_safe_compact, perDay.coerceAtLeast(0L).formatLkr()),
                style = RunwayType.title2,
                color = c.ink,
            )
        }
        Box(Modifier.fillMaxWidth().height(Dimens.hairline).background(c.line))
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) { content() }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun HomeSuccessPreview() {
    RunwayTheme {
        HomeScreen(UiState.Success(previewHomeSummary()), actions = previewHomeActions(), onRetry = {})
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeDayZeroPreview() {
    RunwayTheme {
        HomeScreen(UiState.Success(previewHomeSummary(dayZero = true)), actions = previewHomeActions(), onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeLoadingPreview() {
    RunwayTheme { HomeScreen(UiState.Loading, actions = HomeActions(), onRetry = {}) }
}

@Preview(showBackground = true)
@Composable
private fun HomeEmptyPreview() {
    RunwayTheme { HomeScreen(UiState.Empty, actions = previewHomeActions(), onRetry = {}) }
}

@Preview(showBackground = true)
@Composable
private fun HomeErrorPreview() {
    RunwayTheme { HomeScreen(UiState.Error(AppError.Network), actions = HomeActions(), onRetry = {}) }
}
