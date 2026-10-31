package com.example.runway.ui.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.formatLkr
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.CategoryKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.ChipGroup
import com.example.runway.ui.components.DateField
import com.example.runway.ui.components.ErrorState
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwaySheet
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.SmallTopBar
import com.example.runway.ui.components.dayMonthLabel
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate

data class DetailActions(
    val onBack: () -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onCategoryChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onDateChange: (LocalDate) -> Unit = {},
    val onSave: () -> Unit = {},
    val onArchive: () -> Unit = {},
    val onRefundOpen: () -> Unit = {},
    val onRefundDismiss: () -> Unit = {},
    val onRefundChange: (String) -> Unit = {},
    val onRefundSave: () -> Unit = {},
)

@Composable
fun TransactionDetailRoute(onDone: () -> Unit, viewModel: TransactionDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }
    TransactionDetailScreen(
        state = state,
        form = form,
        today = viewModel.today,
        actions = DetailActions(
            onBack = onDone,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onNoteChange = viewModel::onNoteChange,
            onDateChange = viewModel::onDateChange,
            onSave = viewModel::onSave,
            onArchive = viewModel::onArchive,
            onRefundOpen = viewModel::onRefundOpen,
            onRefundDismiss = viewModel::onRefundDismiss,
            onRefundChange = viewModel::onRefundChange,
            onRefundSave = viewModel::onRefundSave,
        ),
    )
}

@Composable
fun TransactionDetailScreen(state: UiState<DetailContent>, form: DetailForm, today: LocalDate, actions: DetailActions) {
    val c = RunwayTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg).imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        SmallTopBar(title = stringResource(R.string.activity_detail_title), onBack = actions.onBack)
        when (state) {
            is UiState.Success -> DetailForm(state.data, form, today, actions)
            is UiState.Error -> ErrorState(state.error, stringResource(R.string.ui_back), actions.onBack)
            else -> LoadingState()
        }
    }
}

@Composable
private fun DetailForm(content: DetailContent, form: DetailForm, today: LocalDate, actions: DetailActions) {
    val c = RunwayTheme.colors
    val tx = content.tx
    var confirmingArchive by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = Dimens.contentMax)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenMargin)
            .padding(top = Spacing.s2, bottom = Spacing.s6),
        verticalArrangement = Arrangement.spacedBy(Spacing.s6),
    ) {
        AmountField(
            value = form.amount,
            onValueChange = actions.onAmountChange,
            label = stringResource(R.string.activity_amount),
            currencyCode = tx.currency.name,
            helperText = if (tx.currency != CurrencyCode.LKR) {
                stringResource(R.string.activity_rate_kept, tx.currency.name, tx.fxRate.toString())
            } else {
                null
            },
            errorText = if (form.amountError) stringResource(R.string.amount_error_invalid) else null,
        )
        if (tx.type == TransactionType.EXPENSE && content.categories.isNotEmpty()) {
            ChipGroup(
                label = stringResource(R.string.activity_category),
                choices = content.categories.map { it.id to it.name },
                selected = form.categoryId,
                onSelect = actions.onCategoryChange,
            )
        }
        RunwayTextField(value = form.note, onValueChange = actions.onNoteChange, label = stringResource(R.string.activity_note))
        DateField(
            label = stringResource(R.string.activity_date),
            text = (form.date ?: today).dayMonthLabel(),
            selected = form.date ?: today,
            latest = today,
            onPick = actions.onDateChange,
        )
        form.formError?.let { Text(stringResource(it.messageRes()), style = RunwayType.bodySm, color = c.danger) }
        RunwayButton(
            text = stringResource(R.string.activity_save),
            onClick = actions.onSave,
            size = ButtonSize.Large,
            fillWidth = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            if (tx.type == TransactionType.EXPENSE) {
                RunwayButton(
                    text = stringResource(R.string.activity_refund),
                    onClick = actions.onRefundOpen,
                    modifier = Modifier.weight(1f),
                    style = ButtonStyle.Secondary,
                )
            }
            RunwayButton(
                text = stringResource(R.string.activity_archive),
                onClick = { confirmingArchive = true },
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Tertiary,
            )
        }
    }

    if (confirmingArchive) {
        AlertDialog(
            onDismissRequest = { confirmingArchive = false },
            title = { Text(stringResource(R.string.activity_archive_title)) },
            text = { Text(stringResource(R.string.activity_archive_body)) },
            confirmButton = {
                RunwayButton(
                    text = stringResource(R.string.activity_archive),
                    onClick = {
                        confirmingArchive = false
                        actions.onArchive()
                    },
                    style = ButtonStyle.Tertiary,
                    size = ButtonSize.Small,
                )
            },
            dismissButton = {
                RunwayButton(
                    text = stringResource(R.string.activity_archive_keep),
                    onClick = { confirmingArchive = false },
                    style = ButtonStyle.Tertiary,
                    size = ButtonSize.Small,
                )
            },
        )
    }
    form.refund?.let { refund ->
        val original = tx.amountLkrMinor.formatLkr(cents = tx.amountLkrMinor % 100 != 0L)
        RunwaySheet(title = stringResource(R.string.activity_refund_title), onDismiss = actions.onRefundDismiss) {
            AmountField(
                value = refund,
                onValueChange = actions.onRefundChange,
                label = stringResource(R.string.activity_amount),
                helperText = stringResource(R.string.activity_refund_helper, original),
                errorText = if (form.refundError) stringResource(R.string.activity_refund_error, original) else null,
            )
            RunwayButton(
                text = stringResource(R.string.activity_refund_save),
                onClick = actions.onRefundSave,
                size = ButtonSize.Large,
                fillWidth = true,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun DetailPreview() {
    val at = Instant.parse("2026-11-03T04:00:00Z")
    val tx = Transaction(
        "1", TransactionType.EXPENSE, 120_000, CurrencyCode.LKR, 1.0, 120_000, "food", null, null, null, null,
        "Kottu", at, "2026-11", false, at, at,
    )
    val food = Category("food", "Food", "restaurant", CategoryKind.DISCRETIONARY, 0, false)
    RunwayTheme {
        TransactionDetailScreen(
            UiState.Success(DetailContent(tx, listOf(food))),
            DetailForm(amount = "1200", categoryId = "food", note = "Kottu", date = LocalDate.of(2026, 11, 3)),
            LocalDate.of(2026, 11, 4),
            DetailActions(),
        )
    }
}
