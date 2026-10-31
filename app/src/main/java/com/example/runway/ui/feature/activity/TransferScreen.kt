package com.example.runway.ui.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ChipGroup
import com.example.runway.ui.components.ErrorState
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.SmallTopBar
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.Instant

@Composable
fun TransferRoute(onDone: () -> Unit, viewModel: TransferViewModel = hiltViewModel()) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }
    TransferScreen(
        accounts = accounts,
        form = form,
        onBack = onDone,
        onAmountChange = viewModel::onAmountChange,
        onFromChange = viewModel::onFromChange,
        onToChange = viewModel::onToChange,
        onSave = viewModel::onSave,
    )
}

/** Form template: small top bar, labelled fields, one Large primary that rides the keyboard. Stateless. */
@Composable
fun TransferScreen(
    accounts: UiState<List<Account>>,
    form: TransferForm,
    onBack: () -> Unit,
    onAmountChange: (String) -> Unit,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val c = RunwayTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg).imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        SmallTopBar(title = stringResource(R.string.transfer_title), onBack = onBack)
        when (accounts) {
            UiState.Loading, UiState.Empty -> LoadingState()
            is UiState.Error -> ErrorState(accounts.error, stringResource(R.string.ui_back), onBack)
            is UiState.Success -> {
                val (fromId, toId) = selected(form, accounts.data)
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
                    AmountField(
                        value = form.amount,
                        onValueChange = onAmountChange,
                        label = stringResource(R.string.activity_amount),
                        errorText = if (form.amountError) stringResource(R.string.amount_error_invalid) else null,
                        enabled = !form.saving,
                    )
                    ChipGroup(stringResource(R.string.transfer_from), accounts.data.filter { it.id != toId }.map { it.id to it.name }, fromId, onFromChange)
                    ChipGroup(stringResource(R.string.transfer_to), accounts.data.filter { it.id != fromId }.map { it.id to it.name }, toId, onToChange)
                    Text(stringResource(R.string.transfer_helper), style = RunwayType.bodySm, color = c.inkMuted)
                    form.formError?.let { Text(stringResource(it.messageRes()), style = RunwayType.bodySm, color = c.danger) }
                }
                RunwayButton(
                    text = stringResource(R.string.transfer_save),
                    onClick = onSave,
                    modifier = Modifier
                        .widthIn(max = Dimens.contentMax)
                        .padding(horizontal = Dimens.screenMargin)
                        .padding(top = Spacing.s3, bottom = Spacing.s4),
                    size = ButtonSize.Large,
                    fillWidth = true,
                    loading = form.saving,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun TransferPreview() {
    val at = Instant.parse("2026-11-03T04:00:00Z")
    val accounts = listOf(
        Account("bank", "Bank", AccountKind.BANK, 0, 0, at, false, at),
        Account("cash", "Cash", AccountKind.CASH, 0, 0, at, false, at),
    )
    RunwayTheme { TransferScreen(UiState.Success(accounts), TransferForm(amount = "10000"), {}, {}, {}, {}, {}) }
}
