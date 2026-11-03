package com.example.runway.ui.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.formatAmount
import com.example.runway.core.money.formatLkr
import com.example.runway.core.result.AppError
import com.example.runway.domain.usecase.IncomeReality
import com.example.runway.domain.usecase.Insights
import com.example.runway.domain.usecase.SourceReality
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.CardTitle
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.HowItWorks
import com.example.runway.ui.components.LargeTitle
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.MoneyKind
import com.example.runway.ui.components.MoneyText
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayCard
import com.example.runway.ui.components.StatusTag
import com.example.runway.ui.components.TagTone
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/** A null action hides its button until the screen behind it exists. */
data class InsightsActions(
    val onRetry: () -> Unit = {},
    val onCheckin: (() -> Unit)? = null,
    val onCrypto: (() -> Unit)? = null,
)

@Composable
fun InsightsRoute(onCheckin: (() -> Unit)?, onCrypto: (() -> Unit)?, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(state, InsightsActions(onRetry = viewModel::retry, onCheckin = onCheckin, onCrypto = onCrypto))
}

/** Stateless. Large title, then one card per insight; each card owns its empty state. */
@Composable
fun InsightsScreen(state: UiState<Insights>, actions: InsightsActions) {
    val c = RunwayTheme.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        when (state) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> Unit // never emitted: each card handles "nothing yet" itself
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
                val cardModifier = Modifier.padding(horizontal = Dimens.screenMargin)
                LazyColumn(contentPadding = PaddingValues(bottom = Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                    item(key = "title") { LargeTitle(stringResource(R.string.insights_title)) }
                    item(key = "reality") { IncomeRealityCard(state.data.incomeReality, cardModifier) }
                    item(key = "balances") { BalancesCard(state.data, actions.onCheckin, cardModifier) }
                    item(key = "crypto") { CryptoCard(state.data, actions.onCrypto, cardModifier) }
                }
                // Content scrolls under a bg strip, so the status-bar icons always sit on bg.
                Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(c.bg))
            }
        }
    }
}

/** What he actually earns vs what he thinks he earns (F-12, FR-15). Plain numbers, no judgement. */
@Composable
private fun IncomeRealityCard(reality: IncomeReality, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    val months = reality.monthsCounted
    RunwayCard(modifier) {
        CardTitle(
            title = stringResource(R.string.reality_title),
            trailing = if (months == 0) null else {
                {
                    val tag = when (months) {
                        3 -> stringResource(R.string.reality_tag_full)
                        1 -> stringResource(R.string.reality_tag_one)
                        else -> stringResource(R.string.reality_tag_partial, months)
                    }
                    StatusTag(tag, if (months == 3) TagTone.Done else TagTone.Pending)
                }
            },
        )
        if (months == 0) {
            Text(stringResource(R.string.reality_empty), style = RunwayType.bodySm, color = c.inkMuted)
            return@RunwayCard
        }
        reality.sources.forEach { SourceRow(it) }
        Box(Modifier.fillMaxWidth().height(Dimens.hairline).background(c.line))
        AmountRow(stringResource(R.string.reality_total), reality.totalAverageMinor)
        val estimate = reality.ownEstimateMinor
        val gap = reality.estimateGapMinor
        if (estimate != null && gap != null) {
            AmountRow(stringResource(R.string.reality_estimate), estimate, muted = true)
            val comparison = when {
                gap > 0 -> stringResource(R.string.reality_estimate_above, gap.formatLkr())
                gap < 0 -> stringResource(R.string.reality_estimate_below, (-gap).formatLkr())
                else -> stringResource(R.string.reality_estimate_match)
            }
            Text(comparison, style = RunwayType.bodySm, color = c.ink)
        }
        Text(stringResource(R.string.reality_footer), style = RunwayType.bodySm, color = c.inkMuted)
        HowItWorks(stringResource(R.string.reality_how))
    }
}

@Composable
private fun SourceRow(reality: SourceReality) {
    val c = RunwayTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(reality.source?.name ?: stringResource(R.string.reality_other), style = RunwayType.label, color = c.ink)
            Text(
                stringResource(R.string.reality_range, reality.minMinor.formatAmount(), reality.maxMinor.formatAmount()),
                style = RunwayType.caption,
                color = c.inkMuted,
            )
        }
        MoneyText(reality.averageMinor)
    }
}

@Composable
private fun AmountRow(label: String, amountMinor: Long, muted: Boolean = false) {
    val c = RunwayTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = RunwayType.label, color = if (muted) c.inkMuted else c.ink, modifier = Modifier.weight(1f))
        MoneyText(amountMinor, color = if (muted) c.inkMuted else c.ink)
    }
}

/** Derived balances and the way into the weekly check-in (F-13): the screen's one primary action. */
@Composable
private fun BalancesCard(insights: Insights, onCheckin: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    val days = insights.daysSinceCheckin
    val checked = when (days) {
        null -> stringResource(R.string.balances_never)
        0 -> stringResource(R.string.balances_checked_today)
        1 -> stringResource(R.string.balances_checked_yesterday)
        else -> stringResource(R.string.balances_checked_days, days)
    }
    RunwayCard(modifier) {
        CardTitle(
            title = stringResource(R.string.balances_title),
            trailing = { StatusTag(checked, if (days == null || days >= 7) TagTone.NeedsLook else TagTone.Done) },
        )
        insights.balances.forEach { AmountRow(it.account.name, it.balanceMinor) }
        insights.clarityPercent?.let {
            Text(stringResource(R.string.balances_clarity, it), style = RunwayType.bodySm, color = c.inkMuted)
        }
        HowItWorks(stringResource(R.string.balances_how))
        if (onCheckin != null) {
            RunwayButton(stringResource(R.string.balances_checkin), onCheckin, fillWidth = true)
        }
    }
}

/** Crypto net position (F-17): its own number, never part of Safe to Spend (BR-62). */
@Composable
private fun CryptoCard(insights: Insights, onCrypto: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    val position = insights.crypto
    RunwayCard(modifier) {
        CardTitle(stringResource(R.string.crypto_card_title))
        if (!insights.hasCryptoEntries) {
            Text(stringResource(R.string.crypto_card_empty), style = RunwayType.bodySm, color = c.inkMuted)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.crypto_net), style = RunwayType.label, color = c.ink, modifier = Modifier.weight(1f))
                MoneyText(
                    amountMinor = position.netMinor,
                    kind = when {
                        position.netMinor > 0 -> MoneyKind.Income
                        position.netMinor < 0 -> MoneyKind.Expense
                        else -> MoneyKind.Plain
                    },
                    style = RunwayType.moneyLg,
                )
            }
            Text(
                stringResource(
                    R.string.crypto_breakdown,
                    position.depositsMinor.formatAmount(),
                    position.withdrawalsMinor.formatAmount(),
                    position.holdingsMinor?.formatAmount() ?: "–",
                ),
                style = RunwayType.bodySm,
                color = c.inkMuted,
            )
            if (position.holdingsMinor == null) {
                Text(stringResource(R.string.crypto_no_snapshot), style = RunwayType.bodySm, color = c.inkMuted)
            }
            Text(stringResource(R.string.crypto_kept_out), style = RunwayType.caption, color = c.inkMuted)
            HowItWorks(stringResource(R.string.crypto_how))
        }
        if (onCrypto != null) {
            RunwayButton(stringResource(R.string.crypto_open), onCrypto, style = ButtonStyle.Tonal, size = ButtonSize.Small)
        }
    }
}

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun InsightsSuccessPreview() {
    RunwayTheme { InsightsScreen(UiState.Success(previewInsights()), InsightsActions(onCheckin = {}, onCrypto = {})) }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun InsightsNewUserPreview() {
    RunwayTheme { InsightsScreen(UiState.Success(previewInsights(newUser = true)), InsightsActions(onCheckin = {}, onCrypto = {})) }
}

@Preview(showBackground = true)
@Composable
private fun InsightsLoadingPreview() {
    RunwayTheme { InsightsScreen(UiState.Loading, InsightsActions()) }
}

@Preview(showBackground = true)
@Composable
private fun InsightsErrorPreview() {
    RunwayTheme { InsightsScreen(UiState.Error(AppError.Network), InsightsActions()) }
}
