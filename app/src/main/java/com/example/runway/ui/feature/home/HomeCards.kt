package com.example.runway.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.core.money.formatAmount
import com.example.runway.core.money.formatLkr
import com.example.runway.domain.usecase.GoalProjection
import com.example.runway.domain.usecase.HomePrompt
import com.example.runway.domain.usecase.HomeSummary
import com.example.runway.domain.usecase.MonthSpending
import com.example.runway.domain.usecase.SpendPace
import com.example.runway.domain.usecase.spentMinor
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.CardTitle
import com.example.runway.ui.components.CardTone
import com.example.runway.ui.components.EdgePill
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.HeroAmount
import com.example.runway.ui.components.HeroIconButton
import com.example.runway.ui.components.HeroPanel
import com.example.runway.ui.components.HowItWorks
import com.example.runway.ui.components.InitialKeycap
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.LedgerRow
import com.example.runway.ui.components.MoneyText
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayCard
import com.example.runway.ui.components.SectionHeader
import com.example.runway.ui.components.SquiggleText
import com.example.runway.ui.components.StatusTag
import com.example.runway.ui.components.TagTone
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.components.categoryIcon
import com.example.runway.ui.components.charts.BarItem
import com.example.runway.ui.components.charts.BarList
import com.example.runway.ui.components.charts.DonutChart
import com.example.runway.ui.components.charts.DonutSlice
import com.example.runway.ui.components.charts.GoalRunwayTrack
import com.example.runway.ui.components.dayMonthLabel
import com.example.runway.ui.components.dayMonthSpoken
import com.example.runway.ui.components.monthYearLabel
import com.example.runway.ui.components.monthYearSpoken
import com.example.runway.ui.components.shortDayLabel
import com.example.runway.ui.components.weekdayLabel
import com.example.runway.ui.feature.goal.projectionLine
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.temporal.ChronoUnit

private const val TOP_CATEGORIES = 5

// ---- Hero ----

/** Safe to Spend today (FR-13), the screen's one loud thing. Negative never shows: the pill says it. */
@Composable
internal fun SafeToSpendHero(summary: HomeSummary, onSettings: (() -> Unit)?) {
    val c = RunwayTheme.colors
    val perDay = summary.safeToSpend.perDay.coerceAtLeast(0L)
    val monthEnd = summary.today.withDayOfMonth(summary.today.lengthOfMonth())
    val greeting = if (summary.firstName.isBlank()) {
        stringResource(R.string.home_greeting_anonymous)
    } else {
        stringResource(R.string.home_greeting, summary.firstName)
    }

    HeroPanel(
        topRow = {
            if (summary.firstName.isNotBlank()) InitialKeycap(summary.firstName.take(1).uppercase())
            Column(Modifier.weight(1f)) {
                Text(greeting, style = RunwayType.title2, color = c.onBrand, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(summary.today.weekdayLabel(), style = RunwayType.caption, color = c.onBrandMuted)
            }
            if (onSettings != null) HeroIconButton(Icons.Rounded.Settings, stringResource(R.string.home_settings), onSettings)
        },
        pill = { PacePill(summary) },
    ) {
        Text(
            text = stringResource(R.string.home_safe_eyebrow).uppercase(),
            style = RunwayType.eyebrow,
            color = c.onBrandMuted,
            modifier = Modifier.padding(top = Spacing.s4),
        )
        HeroAmount(
            amountMinor = perDay,
            spoken = stringResource(R.string.home_safe_spoken, perDay.formatAmount(), monthEnd.dayMonthSpoken()),
            modifier = Modifier.padding(vertical = Spacing.s1),
        )
        Text(stringResource(R.string.home_safe_context, monthEnd.dayMonthLabel()), style = RunwayType.bodySm, color = c.onBrandMuted)
    }
}

/** The screen's one yellow mark. Never red, never "overspent" (Doc 1 section 3.1). */
@Composable
private fun PacePill(summary: HomeSummary) {
    val c = RunwayTheme.colors
    val (text, icon) = when (summary.pace) {
        SpendPace.ON_PACE -> stringResource(R.string.home_pace_on) to Icons.AutoMirrored.Rounded.TrendingUp
        SpendPace.FASTER -> stringResource(R.string.home_pace_faster) to Icons.Rounded.Speed
        SpendPace.OVER ->
            stringResource(R.string.home_pace_over, (-summary.safeToSpend.remaining).formatLkr()) to Icons.Rounded.Speed
    }
    EdgePill(text = text, icon = icon, fill = c.highlight, content = c.onHighlight)
}

// ---- Actions and prompt ----

/** Tonal, never Primary: the hero is already the loud thing. Buttons appear as their features land. */
@Composable
internal fun ActionRow(actions: HomeActions) {
    val income = actions.onIncome
    val transfer = actions.onTransfer
    val checkin = actions.onCheckin
    if (income == null && transfer == null && checkin == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenMargin).padding(top = Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (income != null) {
            RunwayButton(
                text = stringResource(R.string.home_action_income),
                onClick = income,
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Tonal,
                leadingIcon = Icons.Rounded.Add,
            )
        }
        if (transfer != null) SquareIconButton(Icons.Rounded.SwapHoriz, stringResource(R.string.home_action_transfer), transfer)
        if (checkin != null) {
            RunwayButton(
                text = stringResource(R.string.home_action_checkin),
                onClick = checkin,
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Tonal,
                leadingIcon = Icons.Rounded.Check,
            )
        }
    }
}

@Composable
private fun SquareIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = RunwayTheme.colors
    Box(
        modifier = Modifier
            .size(Dimens.buttonMd)
            .clip(RunwayShape.Field)
            .background(c.accentSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconMd), tint = c.accent)
    }
}

/** Welcome back (BR-85) or a weekly check-in nudge. Balance check first, never "you missed". */
@Composable
internal fun PromptCard(prompt: HomePrompt, onCheckin: () -> Unit, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    val title: String
    val body: String
    val action: String
    when (prompt) {
        is HomePrompt.WelcomeBack -> {
            title = stringResource(R.string.home_welcome_title)
            body = stringResource(R.string.home_welcome_body, prompt.daysAway)
            action = stringResource(R.string.home_welcome_action)
        }
        is HomePrompt.CheckinDue -> {
            title = stringResource(R.string.home_checkin_title)
            body = prompt.daysSince?.let { stringResource(R.string.home_checkin_body, it) }
                ?: stringResource(R.string.home_checkin_body_first)
            action = stringResource(R.string.home_checkin_action)
        }
    }
    RunwayCard(modifier, tone = CardTone.Note) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(Dimens.iconSm), tint = c.ink)
            CardTitle(title)
        }
        Text(body, style = RunwayType.bodySm, color = c.ink)
        RunwayButton(text = action, onClick = onCheckin, style = ButtonStyle.Secondary, size = ButtonSize.Small)
    }
}

// ---- Goal ----

/** Goal card (surface + coral): saved of target, the runway and one projection line (FR-13). */
@Composable
internal fun GoalSummaryCard(name: String, projection: GoalProjection, onOpen: () -> Unit) {
    val c = RunwayTheme.colors
    val months = ChronoUnit.MONTHS.between(projection.startDate, projection.deadline).toInt().coerceAtLeast(1)
    val line = projectionLine(projection)
    RunwayCard(Modifier.clip(RunwayShape.Card).clickable(onClick = onOpen)) {
        CardTitle(name, trailing = { StatusTag(stringResource(R.string.goal_percent, projection.percent), TagTone.Goal) })
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            MoneyText(projection.savedMinor, style = RunwayType.moneyLg, modifier = Modifier.alignByBaseline())
            Text(
                text = stringResource(R.string.goal_saved_of, projection.targetMinor.formatAmount()),
                style = RunwayType.body,
                color = c.inkMuted,
                modifier = Modifier.alignByBaseline(),
            )
        }
        GoalRunwayTrack(
            progress = projection.progress,
            months = months,
            startLabel = projection.startDate.monthYearLabel(),
            endLabel = projection.deadline.monthYearLabel(),
            description = stringResource(
                R.string.goal_track_description,
                projection.savedMinor.formatAmount(),
                projection.targetMinor.formatAmount(),
                projection.percent,
                projection.deadline.monthYearSpoken(),
            ),
        )
        SquiggleText(text = line.text, squiggle = line.squiggle)
    }
}

/** Day zero without a goal: say what the goal does for him, then one way to set it. */
@Composable
internal fun SetGoalCard(onSetGoal: () -> Unit) {
    RunwayCard {
        CardTitle(stringResource(R.string.home_set_goal_title))
        Text(stringResource(R.string.home_set_goal_body), style = RunwayType.bodySm, color = RunwayTheme.colors.inkMuted)
        RunwayButton(
            text = stringResource(R.string.home_set_goal_action),
            onClick = onSetGoal,
            style = ButtonStyle.Tonal,
            size = ButtonSize.Small,
        )
    }
}

// ---- This month ----

/** Committed vs discretionary (donut) and top categories (bars): the self-awareness views (Doc 1 section 4.5). */
@Composable
internal fun MonthSection(summary: HomeSummary) {
    val c = RunwayTheme.colors
    val month = summary.month
    val input = summary.safeToSpendInput
    Column(
        modifier = Modifier.padding(horizontal = Dimens.screenMargin).padding(top = Dimens.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        SectionHeader(stringResource(R.string.home_month_title), totalMinor = month.totalMinor)
        RunwayCard {
            CardTitle(stringResource(R.string.home_split_title))
            if (month.totalMinor == 0L) {
                Text(stringResource(R.string.home_split_empty), style = RunwayType.bodySm, color = c.inkMuted)
            } else {
                SplitChart(month)
            }
            summary.clarityPercent?.let {
                Text(stringResource(R.string.home_clarity, it), style = RunwayType.bodySm, color = c.inkMuted)
            }
            HowItWorks(
                stringResource(
                    R.string.home_safe_how,
                    input.reliableIncomeExpected.formatLkr(),
                    input.variableIncomeReceived.formatLkr(),
                    input.committedCosts.formatLkr(),
                    input.goalContribution.formatLkr(),
                    summary.safeToSpend.monthBudget.formatLkr(),
                    input.discretionarySpent.formatLkr(),
                    summary.safeToSpend.daysLeft,
                ),
            )
        }
        if (month.byCategory.isNotEmpty()) {
            RunwayCard {
                CardTitle(stringResource(R.string.home_top_title))
                val uncategorised = stringResource(R.string.home_uncategorised)
                BarList(
                    items = month.byCategory.take(TOP_CATEGORIES).map {
                        BarItem(it.category?.name ?: uncategorised, it.netMinor, categoryIcon(it.category?.iconKey))
                    },
                )
            }
        }
    }
}

@Composable
private fun SplitChart(month: MonthSpending) {
    val c = RunwayTheme.colors
    val committedShare = (month.committedMinor * 100 / month.totalMinor).toInt()
    val discretionaryShare = 100 - committedShare
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s5)) {
        DonutChart(
            slices = listOf(DonutSlice(month.committedMinor, c.accent), DonutSlice(month.discretionaryMinor, c.highlight)),
            centerValue = month.totalMinor.formatAmount(),
            centerLabel = stringResource(R.string.home_split_center),
            description = stringResource(
                R.string.home_split_description,
                month.committedMinor.formatAmount(),
                committedShare,
                month.discretionaryMinor.formatAmount(),
                discretionaryShare,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            LegendItem(
                color = c.accent,
                label = stringResource(R.string.home_split_committed),
                value = stringResource(R.string.home_split_value, month.committedMinor.formatAmount(), committedShare),
            )
            LegendItem(
                color = c.highlight,
                label = stringResource(R.string.home_split_discretionary),
                value = stringResource(R.string.home_split_value, month.discretionaryMinor.formatAmount(), discretionaryShare),
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    val c = RunwayTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Box(Modifier.padding(top = 5.dp).size(10.dp).background(color, CircleShape))
        Column {
            Text(label, style = RunwayType.label, color = c.ink)
            Text(value, style = RunwayType.bodySm, color = c.inkMuted)
        }
    }
}

// ---- Today ----

@Composable
internal fun TodaySection(summary: HomeSummary, actions: HomeActions) {
    val c = RunwayTheme.colors
    Column(
        modifier = Modifier.padding(horizontal = Dimens.screenMargin).padding(top = Dimens.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        SectionHeader(
            title = stringResource(R.string.home_today, summary.today.shortDayLabel()),
            totalMinor = summary.todayActivity.spentMinor().takeIf { it > 0 },
        )
        if (summary.todayActivity.isEmpty()) {
            RunwayCard {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        lockup = nothingYetLockup(),
                        title = stringResource(R.string.home_today_empty_title),
                        body = stringResource(R.string.home_today_empty_body),
                        actionLabel = actions.onAddExpense?.let { stringResource(R.string.home_add_expense) },
                        onAction = actions.onAddExpense,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().clip(RunwayShape.Card).background(c.surface)) {
                summary.todayActivity.forEachIndexed { index, tx ->
                    key(tx.id) {
                        if (index > 0) {
                            Box(Modifier.padding(start = 72.dp).fillMaxWidth().height(Dimens.hairline).background(c.line))
                        }
                        LedgerRow(tx, summary.categoriesById, summary.accountsById, summary.sourcesById, summary.zone)
                    }
                }
            }
        }
        RunwayButton(
            text = stringResource(R.string.home_see_activity),
            onClick = actions.onSeeActivity,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = ButtonStyle.Tertiary,
            size = ButtonSize.Small,
        )
    }
}

@Composable
internal fun nothingYetLockup(): List<LockupWord> = listOf(
    LockupWord(stringResource(R.string.lockup_nothing), WordShape.Box),
    LockupWord(stringResource(R.string.lockup_here), WordShape.Pill),
    LockupWord(stringResource(R.string.lockup_yet), WordShape.Arrow),
)
