package com.example.runway.ui.feature.activity

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.time.MonthKey
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.CategoryKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.LargeTitle
import com.example.runway.ui.components.LedgerRow
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.SectionHeader
import com.example.runway.ui.components.categoryIcon
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.monthYearLabel
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.components.weekdayLabel
import com.example.runway.ui.feature.home.nothingYetLockup
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ActivityRoute(onOpen: (String) -> Unit, viewModel: ActivityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    ActivityScreen(
        state = state,
        month = month,
        canGoNext = viewModel.canGoNext(month),
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onCategorySelected = viewModel::onCategorySelected,
        onOpen = onOpen,
        onRetry = viewModel::retry,
    )
}

/** Stateless: month switcher, category filter, then the month grouped by day under sticky headers. */
@Composable
fun ActivityScreen(
    state: UiState<ActivityContent>,
    month: String,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(RunwayTheme.colors.bg)) {
        LargeTitle(stringResource(R.string.activity_title))
        MonthSwitcher(month, canGoNext, onPreviousMonth, onNextMonth)
        Box(Modifier.weight(1f)) {
            when (state) {
                UiState.Loading -> LoadingState()
                UiState.Empty -> Centred {
                    EmptyState(
                        lockup = nothingYetLockup(),
                        title = stringResource(R.string.activity_empty_title),
                        body = stringResource(R.string.activity_empty_body),
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
                is UiState.Success -> ActivityList(state.data, onCategorySelected, onOpen)
            }
        }
    }
}

@Composable
private fun MonthSwitcher(month: String, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val c = RunwayTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.activity_previous_month), tint = c.ink)
        }
        Text(
            text = MonthKey.parse(month)?.atDay(1)?.monthYearLabel().orEmpty(),
            style = RunwayType.headline,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            if (canGoNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.activity_next_month), tint = c.ink)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityList(content: ActivityContent, onCategorySelected: (String?) -> Unit, onOpen: (String) -> Unit) {
    val c = RunwayTheme.colors
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.s16)) {
        item(key = "filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.screenMargin, vertical = Spacing.s2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                item(key = "all") {
                    CategoryChip(
                        label = stringResource(R.string.activity_filter_all),
                        selected = content.selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                    )
                }
                items(content.filters, key = { it.id }) { category ->
                    CategoryChip(
                        label = category.name,
                        selected = content.selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        leadingIcon = categoryIcon(category.iconKey),
                    )
                }
            }
        }
        content.days.forEach { day ->
            stickyHeader(key = day.date.toString()) {
                SectionHeader(
                    title = day.date.weekdayLabel(),
                    totalMinor = day.spentMinor.takeIf { it > 0 },
                    modifier = Modifier
                        .background(c.bg)
                        .padding(horizontal = Dimens.screenMargin, vertical = Spacing.s2),
                )
            }
            items(day.transactions, key = { it.id }) { tx ->
                LedgerRow(
                    tx = tx,
                    categoriesById = content.categoriesById,
                    accountsById = content.accountsById,
                    sourcesById = content.sourcesById,
                    zone = content.zone,
                    modifier = Modifier.clickable(role = Role.Button) { onOpen(tx.id) },
                )
            }
        }
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private fun previewContent(): ActivityContent {
    val zone = ZoneId.of("Asia/Colombo")
    val food = Category("food", "Food", "restaurant", CategoryKind.DISCRETIONARY, 0, false)
    val date = LocalDate.of(2026, 11, 3)
    val at: Instant = date.atTime(20, 42).atZone(zone).toInstant()
    val tx = Transaction(
        "1", TransactionType.EXPENSE, 120_000, CurrencyCode.LKR, 1.0, 120_000, "food", null, null, null, null,
        "Kottu", at, "2026-11", false, at, at,
    )
    return ActivityContent(listOf(ActivityDay(date, 120_000, listOf(tx))), listOf(food), null, mapOf("food" to food), emptyMap(), emptyMap(), zone)
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun ActivityPreview() {
    RunwayTheme { ActivityScreen(UiState.Success(previewContent()), "2026-11", false, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun ActivityEmptyPreview() {
    RunwayTheme { ActivityScreen(UiState.Empty, "2026-10", true, {}, {}, {}, {}, {}) }
}
