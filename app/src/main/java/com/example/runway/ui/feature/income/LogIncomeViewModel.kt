package com.example.runway.ui.feature.income

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.money.FxMath
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.DefaultIncomeSourceIds
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.IncomeSourceKind
import com.example.runway.domain.model.Reliability
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.IncomeSourceRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.LogIncome
import com.example.runway.domain.usecase.TransactionDraft
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncomeOptions(
    val sources: List<IncomeSource>,
    val accounts: List<Account>,  // money arrives here; the goal fund only fills through a transfer
    val defaultAccountId: String?,
    val goalFundId: String?,
    val pyfPercent: Double,
    val lastUsdRate: Double?,
    val unsavedSourceIds: Set<String> = emptySet(), // offered, but no document yet: written on first use
)

/**
 * Onboarding only stores the sources ticked there. Every kind stays on offer here, so skipping AdSense in
 * onboarding never blocks logging it later (BR-42). Same ids as onboarding, so nothing is duplicated.
 */
private val defaultSources = listOf(
    IncomeSource(DefaultIncomeSourceIds.SALARY, "Salary", IncomeSourceKind.SALARY, CurrencyCode.LKR, Reliability.RELIABLE, 0L, null, null),
    IncomeSource(DefaultIncomeSourceIds.FREELANCE, "Freelance", IncomeSourceKind.FREELANCE, CurrencyCode.LKR, Reliability.VARIABLE, 0L, null, null),
    IncomeSource(DefaultIncomeSourceIds.ADSENSE, "AdSense", IncomeSourceKind.ADSENSE, CurrencyCode.USD, Reliability.VARIABLE, 0L, null, null),
    IncomeSource(DefaultIncomeSourceIds.CRYPTO, "Crypto", IncomeSourceKind.CRYPTO, CurrencyCode.LKR, Reliability.VARIABLE, 0L, null, null),
    IncomeSource(DefaultIncomeSourceIds.OTHER, "Other income", IncomeSourceKind.OTHER, CurrencyCode.LKR, Reliability.VARIABLE, 0L, null, null),
)

/** Typed input as Strings, validated on Save. Null selections mean "the default" from [IncomeOptions]. */
data class IncomeForm(
    val sourceId: String? = null,
    val currency: CurrencyCode? = null,
    val amount: String = "",
    val rate: String? = null,     // null means the last rate used
    val useReceived: Boolean = false, // enter the LKR that arrived instead of the rate (BR-06)
    val received: String = "",
    val accountId: String? = null,
    val date: LocalDate? = null,  // null means today
    val note: String = "",
    val pyfOn: Boolean = true,
    val amountError: Boolean = false,
    val rateError: Boolean = false,
    val formError: AppError? = null,
    val saving: Boolean = false,
)

fun IncomeOptions.sourceFor(form: IncomeForm): IncomeSource? =
    sources.firstOrNull { it.id == form.sourceId } ?: sources.firstOrNull()

fun IncomeOptions.currencyFor(form: IncomeForm): CurrencyCode =
    form.currency ?: sourceFor(form)?.currency ?: CurrencyCode.LKR

fun IncomeOptions.rateTextFor(form: IncomeForm): String = form.rate ?: lastUsdRate?.toString().orEmpty()

/** LKR per 1 USD from whichever the person typed. Null while it cannot be worked out. */
fun IncomeOptions.effectiveRate(form: IncomeForm): Double? = if (form.useReceived) {
    val foreign = MoneyParser.parseToMinor(form.amount)
    val lkr = MoneyParser.parseToMinor(form.received)
    if (foreign == null || lkr == null) null else FxMath.effectiveRate(lkr, foreign)
} else {
    rateTextFor(form).trim().toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }
}

/** Pay Yourself First applies to salary only, once a share is set and a goal fund exists (FR-12). */
fun IncomeOptions.offersPyf(form: IncomeForm): Boolean =
    sourceFor(form)?.kind == IncomeSourceKind.SALARY && pyfPercent > 0.0 && goalFundId != null

/** Record any source (F-09). USD stores the rate fixed at entry (BR-03, BR-05, AT-07). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LogIncomeViewModel @Inject constructor(
    private val sources: IncomeSourceRepository,
    accounts: AccountRepository,
    profile: UserProfileRepository,
    private val logIncome: LogIncome,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val options: StateFlow<UiState<IncomeOptions>> = retryTick
        .flatMapLatest {
            combine(sources.observeSources(), accounts.observeAccounts(), profile.observeProfile()) { allSources, allAccounts, userProfile ->
                val spendable = allAccounts.filter { it.kind != AccountKind.GOAL_FUND }
                val unsaved = defaultSources.filter { default -> allSources.none { it.id == default.id } }
                val options = IncomeOptions(
                    sources = allSources + unsaved,
                    accounts = spendable,
                    defaultAccountId = userProfile?.defaultAccountId?.takeIf { id -> spendable.any { it.id == id } } ?: spendable.firstOrNull()?.id,
                    goalFundId = allAccounts.firstOrNull { it.kind == AccountKind.GOAL_FUND }?.id,
                    pyfPercent = userProfile?.pyfPercent ?: 0.0,
                    lastUsdRate = userProfile?.lastFxRates?.get(CurrencyCode.USD.name),
                    unsavedSourceIds = unsaved.map { it.id }.toSet(),
                )
                UiState.Success(options)
            }
                .onStart<UiState<IncomeOptions>> { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _form = MutableStateFlow(
        IncomeForm(
            sourceId = savedState[KEY_SOURCE],
            currency = savedState.get<String>(KEY_CURRENCY)?.let(CurrencyCode::valueOf),
            amount = savedState[KEY_AMOUNT] ?: "",
            rate = savedState[KEY_RATE],
            useReceived = savedState[KEY_USE_RECEIVED] ?: false,
            received = savedState[KEY_RECEIVED] ?: "",
            accountId = savedState[KEY_ACCOUNT],
            date = savedState.get<String>(KEY_DATE)?.let(LocalDate::parse),
            note = savedState[KEY_NOTE] ?: "",
            pyfOn = savedState[KEY_PYF] ?: true,
        ),
    )
    val form: StateFlow<IncomeForm> = _form.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    /** One client id per screen: a double tap or a retry writes the same documents (BR-19). */
    private val id: String = savedState.get<String>(KEY_ID) ?: UUID.randomUUID().toString().also { savedState[KEY_ID] = it }

    val today: LocalDate get() = time.today()

    fun retry() = retryTick.update { it + 1 }

    /** A source brings its own currency: AdSense switches to USD on its own (BR-42). */
    fun onSourceChange(id: String) = update { it.copy(sourceId = id, currency = null, rateError = false) }

    fun onCurrencyChange(currency: CurrencyCode) = update { it.copy(currency = currency, rateError = false) }

    fun onAmountChange(value: String) = update { it.copy(amount = value, amountError = false) }

    fun onRateChange(value: String) = update { it.copy(rate = value, rateError = false) }

    fun onUseReceivedChange(value: Boolean) = update { it.copy(useReceived = value, rateError = false) }

    fun onReceivedChange(value: String) = update { it.copy(received = value, rateError = false) }

    fun onAccountChange(id: String) = update { it.copy(accountId = id) }

    /** Back-dating is fine, a future date is not (BR-18). */
    fun onDateChange(date: LocalDate) = update { it.copy(date = date.takeIf { d -> d < today }) }

    fun onNoteChange(value: String) = update { it.copy(note = value.take(AddTransaction.MAX_NOTE)) }

    fun onPyfChange(on: Boolean) = update { it.copy(pyfOn = on) }

    fun onSave() {
        val form = _form.value
        if (form.saving) return
        val options = (options.value as? UiState.Success)?.data ?: return
        val source = options.sourceFor(form) ?: return
        val currency = options.currencyFor(form)
        val amount = MoneyParser.parseToMinor(form.amount)
        val rate = if (currency == CurrencyCode.LKR) 1.0 else options.effectiveRate(form)
        if (amount == null || rate == null) {
            update { it.copy(amountError = amount == null, rateError = amount != null) }
            return
        }
        update { it.copy(saving = true, formError = null) }
        viewModelScope.launch {
            val draft = TransactionDraft(
                id = id,
                type = TransactionType.INCOME,
                amountMinor = amount,
                date = form.date?.atStartOfDay(time.zone)?.toInstant() ?: time.now(),
                currency = currency,
                fxRate = rate,
                accountId = form.accountId ?: options.defaultAccountId,
                incomeSourceId = source.id,
                note = form.note,
            )
            val pyf = options.offersPyf(form) && form.pyfOn
            val result = logIncome(
                income = draft,
                pyfPercent = options.pyfPercent.takeIf { pyf },
                goalFundId = options.goalFundId.takeIf { pyf },
            )
            when (result) {
                is AppResult.Success -> {
                    if (source.id in options.unsavedSourceIds) sources.add(source)
                    _done.send(Unit)
                }
                is AppResult.Failure -> update { it.copy(saving = false, formError = result.error) }
            }
        }
    }

    private fun update(change: (IncomeForm) -> IncomeForm) {
        val next = change(_form.value)
        _form.value = next
        savedState[KEY_SOURCE] = next.sourceId
        savedState[KEY_CURRENCY] = next.currency?.name
        savedState[KEY_AMOUNT] = next.amount
        savedState[KEY_RATE] = next.rate
        savedState[KEY_USE_RECEIVED] = next.useReceived
        savedState[KEY_RECEIVED] = next.received
        savedState[KEY_ACCOUNT] = next.accountId
        savedState[KEY_DATE] = next.date?.toString()
        savedState[KEY_NOTE] = next.note
        savedState[KEY_PYF] = next.pyfOn
    }

    private companion object {
        const val KEY_SOURCE = "income_source"
        const val KEY_CURRENCY = "income_currency"
        const val KEY_AMOUNT = "income_amount"
        const val KEY_RATE = "income_rate"
        const val KEY_USE_RECEIVED = "income_use_received"
        const val KEY_RECEIVED = "income_received"
        const val KEY_ACCOUNT = "income_account"
        const val KEY_DATE = "income_date"
        const val KEY_NOTE = "income_note"
        const val KEY_PYF = "income_pyf"
        const val KEY_ID = "income_id"
    }
}
