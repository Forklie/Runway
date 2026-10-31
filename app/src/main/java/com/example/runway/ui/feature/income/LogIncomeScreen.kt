package com.example.runway.ui.feature.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.FxMath
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.money.formatLkr
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.IncomeSourceKind
import com.example.runway.domain.model.Reliability
import com.example.runway.domain.usecase.LogIncome
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.ErrorState
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayDatePicker
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.SmallTopBar
import com.example.runway.ui.components.dayMonthLabel
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.feature.settings.toPercentText
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.LocalDate

data class LogIncomeActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onSourceChange: (String) -> Unit = {},
    val onCurrencyChange: (CurrencyCode) -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onRateChange: (String) -> Unit = {},
    val onUseReceivedChange: (Boolean) -> Unit = {},
    val onReceivedChange: (String) -> Unit = {},
    val onAccountChange: (String) -> Unit = {},
    val onDateChange: (LocalDate) -> Unit = {},
    val onPyfChange: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
fun LogIncomeRoute(onDone: () -> Unit, viewModel: LogIncomeViewModel = hiltViewModel()) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }
    LogIncomeScreen(
        options = options,
        form = form,
        today = viewModel.today,
        actions = LogIncomeActions(
            onBack = onDone,
            onRetry = viewModel::retry,
            onSourceChange = viewModel::onSourceChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onAmountChange = viewModel::onAmountChange,
            onRateChange = viewModel::onRateChange,
            onUseReceivedChange = viewModel::onUseReceivedChange,
            onReceivedChange = viewModel::onReceivedChange,
            onAccountChange = viewModel::onAccountChange,
            onDateChange = viewModel::onDateChange,
            onPyfChange = viewModel::onPyfChange,
            onSave = viewModel::onSave,
        ),
    )
}

/** Form template: small top bar, labelled fields, one Large primary that rides the keyboard. Stateless. */
@Composable
fun LogIncomeScreen(options: UiState<IncomeOptions>, form: IncomeForm, today: LocalDate, actions: LogIncomeActions) {
    Column(
        Modifier.fillMaxSize().background(RunwayTheme.colors.bg).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmallTopBar(title = stringResource(R.string.income_title), onBack = actions.onBack)
        when (options) {
            UiState.Loading, UiState.Empty -> LoadingState()
            is UiState.Error -> ErrorState(options.error, stringResource(R.string.action_retry), actions.onRetry)
            is UiState.Success -> IncomeFields(options.data, form, today, actions)
        }
    }
}

@Composable
private fun ColumnScope.IncomeFields(
    options: IncomeOptions,
    form: IncomeForm,
    today: LocalDate,
    actions: LogIncomeActions,
) {
    val c = RunwayTheme.colors
    val currency = options.currencyFor(form)
    val source = options.sourceFor(form)
    var pickingDate by rememberSaveable { mutableStateOf(false) }

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
        Chips(stringResource(R.string.income_source), options.sources.map { it.id to it.name }, source?.id, actions.onSourceChange)
        Chips(
            stringResource(R.string.income_currency),
            CurrencyCode.entries.map { it.name to it.name },
            currency.name,
        ) { actions.onCurrencyChange(CurrencyCode.valueOf(it)) }
        AmountField(
            value = form.amount,
            onValueChange = actions.onAmountChange,
            label = stringResource(R.string.income_amount),
            currencyCode = currency.name,
            errorText = if (form.amountError) stringResource(R.string.amount_error_invalid) else null,
            enabled = !form.saving,
        )
        if (currency != CurrencyCode.LKR) RateFields(options, form, actions)
        if (options.accounts.isNotEmpty()) {
            Chips(
                stringResource(R.string.income_account),
                options.accounts.map { it.id to it.name },
                form.accountId ?: options.defaultAccountId,
                actions.onAccountChange,
            )
        }
        if (options.offersPyf(form)) PyfRow(options, form, currency, actions.onPyfChange)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Text(stringResource(R.string.income_date), style = RunwayType.label, color = c.ink)
            RunwayButton(
                text = form.date?.dayMonthLabel() ?: stringResource(R.string.income_today),
                onClick = { pickingDate = true },
                style = ButtonStyle.Secondary,
                size = ButtonSize.Small,
            )
        }
        form.formError?.let { Text(stringResource(it.messageRes()), style = RunwayType.bodySm, color = c.danger) }
    }
    RunwayButton(
        text = stringResource(R.string.income_save),
        onClick = actions.onSave,
        modifier = Modifier
            .widthIn(max = Dimens.contentMax)
            .padding(horizontal = Dimens.screenMargin)
            .padding(top = Spacing.s3, bottom = Spacing.s4),
        size = ButtonSize.Large,
        fillWidth = true,
        loading = form.saving,
    )
    if (pickingDate) {
        RunwayDatePicker(
            selected = form.date ?: today,
            latest = today,
            onPick = {
                actions.onDateChange(it)
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}

/** The rate or the LKR that arrived, whichever the person has to hand (BR-06), with the result worked out live. */
@Composable
private fun RateFields(options: IncomeOptions, form: IncomeForm, actions: LogIncomeActions) {
    val c = RunwayTheme.colors
    val amount = MoneyParser.parseToMinor(form.amount)
    val rate = options.effectiveRate(form)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Chips(
            stringResource(R.string.income_rate_mode),
            listOf(
                "rate" to stringResource(R.string.income_mode_rate),
                "received" to stringResource(R.string.income_mode_received),
            ),
            if (form.useReceived) "received" else "rate",
        ) { actions.onUseReceivedChange(it == "received") }
        if (form.useReceived) {
            AmountField(
                value = form.received,
                onValueChange = actions.onReceivedChange,
                label = stringResource(R.string.income_received),
                errorText = if (form.rateError) stringResource(R.string.amount_error_invalid) else null,
            )
        } else {
            RunwayTextField(
                value = options.rateTextFor(form),
                onValueChange = actions.onRateChange,
                label = stringResource(R.string.income_rate),
                prefix = stringResource(R.string.ui_currency_lkr),
                errorText = if (form.rateError) stringResource(R.string.income_rate_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        if (amount != null && rate != null) {
            Text(
                stringResource(R.string.income_worked_out, FxMath.toLkrMinor(amount, rate).formatLkr(), rate.toString()),
                style = RunwayType.bodySm,
                color = c.inkMuted,
            )
        }
    }
}

@Composable
private fun PyfRow(options: IncomeOptions, form: IncomeForm, currency: CurrencyCode, onChange: (Boolean) -> Unit) {
    val c = RunwayTheme.colors
    val amount = MoneyParser.parseToMinor(form.amount)
    val rate = if (currency == CurrencyCode.LKR) 1.0 else options.effectiveRate(form)
    val share = if (amount != null && rate != null) LogIncome.pyfMinor(FxMath.toLkrMinor(amount, rate), options.pyfPercent) else null
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(stringResource(R.string.income_pyf), style = RunwayType.label, color = c.ink)
            Text(
                text = if (share != null) {
                    stringResource(R.string.income_pyf_amount, share.formatLkr())
                } else {
                    stringResource(R.string.income_pyf_helper, options.pyfPercent.toPercentText())
                },
                style = RunwayType.bodySm,
                color = c.inkMuted,
            )
        }
        Switch(checked = form.pyfOn, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(label: String, choices: List<Pair<String, String>>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(label, style = RunwayType.label, color = RunwayTheme.colors.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            choices.forEach { (id, name) ->
                CategoryChip(label = name, selected = id == selected, onClick = { onSelect(id) })
            }
        }
    }
}

private fun previewOptions() = IncomeOptions(
    sources = listOf(
        IncomeSource("salary", "Salary", IncomeSourceKind.SALARY, CurrencyCode.LKR, Reliability.RELIABLE, 12_000_000, 25, null),
        IncomeSource("adsense", "AdSense", IncomeSourceKind.ADSENSE, CurrencyCode.USD, Reliability.VARIABLE, 0, null, null),
    ),
    accounts = emptyList(),
    defaultAccountId = null,
    goalFundId = "goal-fund",
    pyfPercent = 30.0,
    lastUsdRate = 305.0,
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LogIncomeSalaryPreview() {
    RunwayTheme {
        LogIncomeScreen(UiState.Success(previewOptions()), IncomeForm(amount = "120000"), LocalDate.of(2026, 11, 4), LogIncomeActions())
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LogIncomeUsdPreview() {
    RunwayTheme {
        LogIncomeScreen(UiState.Success(previewOptions()), IncomeForm(sourceId = "adsense", amount = "120"), LocalDate.of(2026, 11, 4), LogIncomeActions())
    }
}
