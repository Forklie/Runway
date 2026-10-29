package com.example.runway.ui.feature.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.formatLkr
import com.example.runway.core.result.AppError
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.CategoryKind
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwaySheet
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.categoryIcon
import com.example.runway.ui.components.dayMonthLabel
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.showUndo
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

/**
 * Stays in the shell while the sheet is closed, so the Undo snackbar outlives the sheet (BR-17).
 * Each event gets its own coroutine: a new save replaces the showing Undo instead of queueing behind it.
 */
@Composable
fun QuickAddHost(viewModel: QuickAddViewModel, snackbar: SnackbarHostState, visible: Boolean) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            launch {
                when (event) {
                    is QuickAddEvent.Saved -> {
                        val amount = event.amountMinor.formatLkr(cents = event.amountMinor % 100 != 0L)
                        val result = snackbar.showUndo(
                            message = context.getString(R.string.quickadd_saved, amount, event.categoryName),
                            actionLabel = context.getString(R.string.quickadd_undo),
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.onUndo(event.transactionId)
                    }
                    is QuickAddEvent.Failed -> snackbar.showSnackbar(context.getString(event.error.messageRes()))
                }
            }
        }
    }

    if (form.open && visible) {
        val options by viewModel.options.collectAsStateWithLifecycle()
        RunwaySheet(title = stringResource(R.string.quickadd_title), onDismiss = viewModel::onDismiss) {
            QuickAddBody(
                options = options,
                form = form,
                today = viewModel.today,
                actions = QuickAddActions(
                    onAmountChange = viewModel::onAmountChange,
                    onCategoryChange = viewModel::onCategoryChange,
                    onNoteChange = viewModel::onNoteChange,
                    onDateChange = viewModel::onDateChange,
                    onAccountChange = viewModel::onAccountChange,
                    onSave = viewModel::onSave,
                    onRetry = viewModel::retry,
                ),
            )
        }
    }
}

data class QuickAddActions(
    val onAmountChange: (String) -> Unit = {},
    val onCategoryChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onDateChange: (LocalDate) -> Unit = {},
    val onAccountChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
private fun ColumnScope.QuickAddBody(
    options: UiState<QuickAddOptions>,
    form: QuickAddForm,
    today: LocalDate,
    actions: QuickAddActions,
) {
    val c = RunwayTheme.colors
    when (options) {
        UiState.Loading -> LoadingState(Modifier.height(160.dp))
        UiState.Empty -> Text(stringResource(R.string.quickadd_empty), style = RunwayType.body, color = c.inkMuted)
        is UiState.Error -> {
            Text(stringResource(options.error.messageRes()), style = RunwayType.body, color = c.inkMuted)
            RunwayButton(stringResource(R.string.action_retry), actions.onRetry, size = ButtonSize.Large, fillWidth = true)
        }
        is UiState.Success -> {
            // Scrolls under the keyboard on small phones; Save below stays on screen.
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                AmountAndCategory(options.data, form, actions)
                MoreOptions(options.data, form, today, actions)
            }
            RunwayButton(
                text = stringResource(R.string.quickadd_save),
                onClick = actions.onSave,
                size = ButtonSize.Large,
                enabled = form.canSave,
                fillWidth = true,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountAndCategory(options: QuickAddOptions, form: QuickAddForm, actions: QuickAddActions) {
    val c = RunwayTheme.colors
    val focus = remember { FocusRequester() }
    var showAll by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() } // amount first, keypad up (Doc 1 section 9.3)

    AmountField(
        value = form.amount,
        onValueChange = actions.onAmountChange,
        label = stringResource(R.string.quickadd_amount),
        modifier = Modifier.focusRequester(focus),
        errorText = if (form.amountError) stringResource(R.string.amount_error_invalid) else null,
    )
    Text(stringResource(R.string.quickadd_category), style = RunwayType.label, color = c.ink)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        val shown = if (showAll) options.allCategories else options.favourites
        shown.forEach { category ->
            CategoryChip(
                label = category.name,
                selected = form.categoryId == category.id,
                onClick = { actions.onCategoryChange(category.id) },
                leadingIcon = categoryIcon(category.iconKey),
            )
        }
        if (!showAll && options.otherCategories.isNotEmpty()) {
            CategoryChip(
                label = stringResource(R.string.quickadd_more_categories),
                selected = false,
                onClick = { showAll = true },
            )
        }
    }
    options.allCategories.firstOrNull { it.id == form.categoryId }?.let { selected ->
        val kind = when (selected.kind) {
            CategoryKind.COMMITTED -> R.string.quickadd_kind_committed
            CategoryKind.DISCRETIONARY -> R.string.quickadd_kind_discretionary
        }
        Text(stringResource(kind), style = RunwayType.bodySm, color = c.inkMuted)
    }
}

/** Note, date and account: hidden, never required (Doc 1 section 9.3). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreOptions(options: QuickAddOptions, form: QuickAddForm, today: LocalDate, actions: QuickAddActions) {
    val c = RunwayTheme.colors
    var open by rememberSaveable { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }

    if (!open) {
        RunwayButton(
            text = stringResource(R.string.quickadd_more_options),
            onClick = { open = true },
            style = ButtonStyle.Tertiary,
            size = ButtonSize.Small,
            trailingIcon = Icons.Rounded.ExpandMore,
        )
        return
    }
    RunwayTextField(
        value = form.note,
        onValueChange = actions.onNoteChange,
        label = stringResource(R.string.quickadd_note),
    )
    Text(stringResource(R.string.quickadd_date), style = RunwayType.label, color = c.ink)
    RunwayButton(
        text = form.date?.dayMonthLabel() ?: stringResource(R.string.quickadd_today),
        onClick = { pickingDate = true },
        style = ButtonStyle.Secondary,
        size = ButtonSize.Small,
    )
    if (options.accounts.isNotEmpty()) {
        val selectedAccount = form.accountId ?: options.defaultAccountId
        Text(stringResource(R.string.quickadd_account), style = RunwayType.label, color = c.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            options.accounts.forEach { account ->
                CategoryChip(
                    label = account.name,
                    selected = selectedAccount == account.id,
                    onClick = { actions.onAccountChange(account.id) },
                )
            }
        }
    }
    if (pickingDate) {
        PastDatePicker(
            selected = form.date ?: today,
            today = today,
            onPick = {
                actions.onDateChange(it)
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}

/** Material's picker works in UTC midnights whatever the phone's zone, so the conversion stays in UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastDatePicker(selected: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            RunwayButton(
                text = stringResource(R.string.quickadd_date_confirm),
                onClick = {
                    state.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        ?.let(onPick) ?: onDismiss()
                },
                style = ButtonStyle.Tertiary,
                size = ButtonSize.Small,
            )
        },
        dismissButton = {
            RunwayButton(
                text = stringResource(R.string.quickadd_date_cancel),
                onClick = onDismiss,
                style = ButtonStyle.Tertiary,
                size = ButtonSize.Small,
            )
        },
    ) {
        DatePicker(state = state)
    }
}

private val previewCategories = listOf(
    Category("rent", "Rent", "home", CategoryKind.COMMITTED, 0, false),
    Category("food-coffee", "Food and Coffee", "restaurant", CategoryKind.DISCRETIONARY, 1, false),
    Category("transport", "Transport", "directions_car", CategoryKind.DISCRETIONARY, 2, false),
)

private val previewOptions = QuickAddOptions(
    favourites = previewCategories.drop(1),
    otherCategories = previewCategories.take(1),
    accounts = listOf(
        Account("bank", "Bank", AccountKind.BANK, 0, 0, Instant.EPOCH, false, Instant.EPOCH),
        Account("cash", "Cash", AccountKind.CASH, 0, 0, Instant.EPOCH, false, Instant.EPOCH),
    ),
    defaultAccountId = "bank",
)

@Composable
private fun QuickAddPreview(options: UiState<QuickAddOptions>, form: QuickAddForm = QuickAddForm(open = true)) {
    RunwayTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            QuickAddBody(options, form, LocalDate.of(2026, 11, 3), QuickAddActions())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddSuccessPreview() = QuickAddPreview(
    UiState.Success(previewOptions),
    QuickAddForm(open = true, amount = "1200", categoryId = "food-coffee"),
)

@Preview(showBackground = true)
@Composable
private fun QuickAddInvalidPreview() = QuickAddPreview(UiState.Success(previewOptions), QuickAddForm(open = true, amount = "0"))

@Preview(showBackground = true)
@Composable
private fun QuickAddLoadingPreview() = QuickAddPreview(UiState.Loading)

@Preview(showBackground = true)
@Composable
private fun QuickAddEmptyPreview() = QuickAddPreview(UiState.Empty)

@Preview(showBackground = true)
@Composable
private fun QuickAddErrorPreview() = QuickAddPreview(UiState.Error(AppError.Network))
