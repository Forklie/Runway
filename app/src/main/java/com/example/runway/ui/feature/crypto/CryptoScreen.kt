package com.example.runway.ui.feature.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.formatAmount
import com.example.runway.core.money.formatLkr
import com.example.runway.core.money.spokenAmount
import com.example.runway.core.result.AppError
import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.model.CryptoEntryType
import com.example.runway.domain.usecase.ComputeCryptoPosition
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.CardTitle
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.HowItWorks
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.MoneyKind
import com.example.runway.ui.components.MoneyText
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayCard
import com.example.runway.ui.components.RunwaySheet
import com.example.runway.ui.components.RunwaySnackbarHost
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.SectionHeader
import com.example.runway.ui.components.SmallTopBar
import com.example.runway.ui.components.TransactionRow
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.components.dayMonthLabel
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.components.showUndo
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId

data class CryptoActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenAdd: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onTypeChange: (CryptoEntryType) -> Unit = {},
    val onAssetChange: (String) -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun CryptoRoute(onBack: () -> Unit, viewModel: CryptoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CryptoEvent.Added -> {
                    val result = snackbar.showUndo(
                        message = context.getString(R.string.crypto_added, event.amountMinor.formatLkr()),
                        actionLabel = context.getString(R.string.crypto_undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndo(event.id)
                }
                is CryptoEvent.Failed -> snackbar.showSnackbar(context.getString(event.error.messageRes()))
            }
        }
    }

    CryptoScreen(
        state = state,
        form = form,
        snackbar = snackbar,
        actions = CryptoActions(
            onBack = onBack,
            onRetry = viewModel::retry,
            onOpenAdd = viewModel::onOpenAdd,
            onDismiss = viewModel::onDismiss,
            onTypeChange = viewModel::onTypeChange,
            onAssetChange = viewModel::onAssetChange,
            onAmountChange = viewModel::onAmountChange,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::onSave,
        ),
    )
}

/** List template: net position, then entries newest first, one pinned primary to add. Stateless. */
@Composable
fun CryptoScreen(state: UiState<CryptoLedger>, form: CryptoForm, snackbar: SnackbarHostState, actions: CryptoActions) {
    val c = RunwayTheme.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            SmallTopBar(title = stringResource(R.string.crypto_title), onBack = actions.onBack)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state) {
                    UiState.Loading -> LoadingState()
                    UiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            lockup = listOf(
                                LockupWord(stringResource(R.string.lockup_no), WordShape.Box),
                                LockupWord(stringResource(R.string.lockup_crypto), WordShape.Pill),
                                LockupWord(stringResource(R.string.lockup_yet), WordShape.Arrow),
                            ),
                            title = stringResource(R.string.crypto_empty_title),
                            body = stringResource(R.string.crypto_empty_body),
                        )
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            lockup = problemLockup(),
                            title = stringResource(R.string.error_title),
                            body = stringResource(state.error.messageRes()),
                            actionLabel = stringResource(R.string.action_retry),
                            onAction = actions.onRetry,
                        )
                    }
                    is UiState.Success -> LedgerList(state.data)
                }
                RunwaySnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
            if (state !is UiState.Error) {
                RunwayButton(
                    text = stringResource(R.string.crypto_add),
                    onClick = actions.onOpenAdd,
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMax)
                        .padding(horizontal = Dimens.screenMargin)
                        .padding(top = Spacing.s3, bottom = Spacing.s4),
                    size = ButtonSize.Large,
                    fillWidth = true,
                )
            }
        }
        if (form.open) AddEntrySheet(form, actions)
    }
}

@Composable
private fun LedgerList(ledger: CryptoLedger) {
    val c = RunwayTheme.colors
    val position = ledger.position
    LazyColumn(contentPadding = PaddingValues(start = Dimens.screenMargin, end = Dimens.screenMargin, bottom = Spacing.s16)) {
        item(key = "position") {
            RunwayCard {
                CardTitle(stringResource(R.string.crypto_net))
                MoneyText(
                    amountMinor = position.netMinor,
                    kind = when {
                        position.netMinor > 0 -> MoneyKind.Income
                        position.netMinor < 0 -> MoneyKind.Expense
                        else -> MoneyKind.Plain
                    },
                    style = RunwayType.moneyLg,
                )
                val summary = when {
                    position.netMinor > 0 -> stringResource(R.string.crypto_net_up, position.netMinor.formatLkr())
                    position.netMinor < 0 -> stringResource(R.string.crypto_net_down, (-position.netMinor).formatLkr())
                    else -> stringResource(R.string.crypto_net_even)
                }
                Text(summary, style = RunwayType.bodySm, color = c.ink)
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
        }
        item(key = "header") {
            SectionHeader(stringResource(R.string.crypto_entries), Modifier.padding(top = Dimens.sectionGap, bottom = Spacing.s3))
        }
        // One surface group with radius-lg (design system, List row): only the ends are rounded.
        itemsIndexed(ledger.entries, key = { _, entry -> entry.id }) { index, entry ->
            val first = index == 0
            val last = index == ledger.entries.lastIndex
            val shape: Shape = when {
                first && last -> RunwayShape.Card
                first -> RunwayShape.Card.copy(bottomStart = CornerSize(0), bottomEnd = CornerSize(0))
                last -> RunwayShape.Card.copy(topStart = CornerSize(0), topEnd = CornerSize(0))
                else -> RectangleShape
            }
            Column(Modifier.fillMaxWidth().clip(shape).background(c.surface)) {
                if (!first) Box(Modifier.padding(start = 72.dp).fillMaxWidth().height(Dimens.hairline).background(c.line))
                EntryRow(entry, ledger.zone)
            }
        }
    }
}

@Composable
private fun EntryRow(entry: CryptoEntry, zone: ZoneId) {
    val typeLabel = typeLabel(entry.type)
    val title = entry.asset.ifBlank { typeLabel }
    val subtitle = listOf(typeLabel, entry.date.atZone(zone).toLocalDate().dayMonthLabel(), entry.note)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val (kind, icon) = when (entry.type) {
        CryptoEntryType.DEPOSIT -> MoneyKind.Expense to Icons.Rounded.Upload
        CryptoEntryType.WITHDRAWAL -> MoneyKind.Income to Icons.Rounded.Download
        CryptoEntryType.HOLDING_SNAPSHOT -> MoneyKind.Plain to Icons.Rounded.Insights
    }
    TransactionRow(
        title = title,
        subtitle = subtitle,
        amountMinor = entry.amountLkrMinor,
        kind = kind,
        icon = icon,
        spoken = listOf(title, spokenAmount(entry.amountLkrMinor, expense = entry.type == CryptoEntryType.DEPOSIT), subtitle).joinToString(", "),
        caption = if (entry.type == CryptoEntryType.HOLDING_SNAPSHOT) stringResource(R.string.crypto_caption_snapshot) else null,
    )
}

@Composable
private fun typeLabel(type: CryptoEntryType): String = when (type) {
    CryptoEntryType.DEPOSIT -> stringResource(R.string.crypto_type_deposit)
    CryptoEntryType.WITHDRAWAL -> stringResource(R.string.crypto_type_withdrawal)
    CryptoEntryType.HOLDING_SNAPSHOT -> stringResource(R.string.crypto_type_snapshot)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEntrySheet(form: CryptoForm, actions: CryptoActions) {
    val c = RunwayTheme.colors
    RunwaySheet(title = stringResource(R.string.crypto_sheet_title), onDismiss = actions.onDismiss) {
        Text(stringResource(R.string.crypto_sheet_body), style = RunwayType.body, color = c.inkMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            CryptoEntryType.entries.forEach { type ->
                CategoryChip(typeLabel(type), selected = form.type == type, onClick = { actions.onTypeChange(type) })
            }
        }
        RunwayTextField(
            value = form.asset,
            onValueChange = actions.onAssetChange,
            label = stringResource(R.string.crypto_asset),
            placeholder = stringResource(R.string.crypto_asset_placeholder),
        )
        AmountField(
            value = form.amount,
            onValueChange = actions.onAmountChange,
            label = stringResource(R.string.crypto_amount),
            errorText = if (form.amountError) stringResource(R.string.amount_error_invalid) else null,
        )
        RunwayTextField(value = form.note, onValueChange = actions.onNoteChange, label = stringResource(R.string.crypto_note))
        RunwayButton(stringResource(R.string.crypto_save), actions.onSave, size = ButtonSize.Large, fillWidth = true)
    }
}

// ---- Previews: a Q1 gain and a Q3 loss, as in the scenario ----

private val previewZone: ZoneId = ZoneId.of("Asia/Colombo")

private fun previewEntry(id: String, type: CryptoEntryType, asset: String, amount: Long, date: String, note: String = "") =
    CryptoEntry(id, type, asset, amount, Instant.parse("${date}T06:00:00Z"), note)

private val previewEntries = listOf(
    previewEntry("e5", CryptoEntryType.HOLDING_SNAPSHOT, "USDT + ETH", 18_000_000, "2026-10-01", "After the Q3 dip"),
    previewEntry("e4", CryptoEntryType.WITHDRAWAL, "USDT", 5_000_000, "2026-05-20", "Cashed out to BOC"),
    previewEntry("e3", CryptoEntryType.HOLDING_SNAPSHOT, "USDT + ETH", 26_000_000, "2026-03-31"),
    previewEntry("e2", CryptoEntryType.DEPOSIT, "ETH", 5_000_000, "2026-03-02"),
    previewEntry("e1", CryptoEntryType.DEPOSIT, "USDT", 15_000_000, "2026-01-10"),
)

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun CryptoLedgerPreview() {
    RunwayTheme {
        CryptoScreen(
            UiState.Success(CryptoLedger(previewEntries, ComputeCryptoPosition()(previewEntries), previewZone)),
            CryptoForm(),
            remember { SnackbarHostState() },
            CryptoActions(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CryptoEmptyPreview() {
    RunwayTheme { CryptoScreen(UiState.Empty, CryptoForm(), remember { SnackbarHostState() }, CryptoActions()) }
}

@Preview(showBackground = true)
@Composable
private fun CryptoErrorPreview() {
    RunwayTheme { CryptoScreen(UiState.Error(AppError.Network), CryptoForm(), remember { SnackbarHostState() }, CryptoActions()) }
}
