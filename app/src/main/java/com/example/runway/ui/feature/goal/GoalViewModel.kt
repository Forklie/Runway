package com.example.runway.ui.feature.goal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.GoalRunway
import com.example.runway.domain.usecase.ObserveGoalRunway
import com.example.runway.domain.usecase.ProjectGoal
import com.example.runway.domain.usecase.TransactionDraft
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GoalSheet { WhatIf, AddToFund }

/** What the two sheets hold. Strings, never Double: validated with MoneyParser on submit (BR-07). */
data class GoalForms(
    val sheet: GoalSheet? = null,
    val whatIfLabel: String = "",
    val whatIfAmount: String = "",
    val whatIfAmountError: Boolean = false,
    val fundAmount: String = "",
    val fundAmountError: Boolean = false,
    val fundFromId: String? = null,
    val fundFromError: Boolean = false,
)

/** The what-if card: the price, what it is, and how many days it moves the goal (BR-75). */
data class WhatIfResult(val label: String, val priceMinor: Long, val delayDays: Int)

sealed interface GoalEvent {
    data class AddedToFund(val transactionId: String, val amountMinor: Long, val fundName: String) : GoalEvent
    data class Failed(val error: AppError) : GoalEvent
}

/**
 * Goal runway and what-if (F-11, FR-14). The numbers come from ObserveGoalRunway and ProjectGoal;
 * this class only holds form input, validates it and turns it into one write.
 * Typed input survives rotation and process death through SavedStateHandle (AT-21).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalViewModel @Inject constructor(
    observeRunway: ObserveGoalRunway,
    private val projectGoal: ProjectGoal,
    private val addTransaction: AddTransaction,
    private val transactions: TransactionRepository,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<UiState<GoalRunway>> = retryTick
        .flatMapLatest {
            observeRunway()
                .map<GoalRunway?, UiState<GoalRunway>> { if (it == null) UiState.Empty else UiState.Success(it) }
                .onStart { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _forms = MutableStateFlow(restoreForms())
    val forms: StateFlow<GoalForms> = _forms.asStateFlow()

    /** The showing what-if, recomputed live: a new transfer into the fund changes the answer. */
    val whatIf: StateFlow<WhatIfResult?> = combine(
        uiState,
        savedState.getStateFlow(KEY_SHOWN_PRICE, 0L),
        savedState.getStateFlow(KEY_SHOWN_LABEL, ""),
    ) { state, price, label ->
        val runway = (state as? UiState.Success)?.data
        if (runway == null || price <= 0L) null else WhatIfResult(label, price, projectGoal.whatIfDelayDays(price, runway.projection))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<GoalEvent>(Channel.BUFFERED)
    val events: Flow<GoalEvent> = _events.receiveAsFlow()

    fun retry() {
        retryTick.update { it + 1 }
    }

    fun onDismissSheet() = updateForms { it.copy(sheet = null) }

    // ---- What if ----

    fun onOpenWhatIf() = updateForms { it.copy(sheet = GoalSheet.WhatIf, whatIfAmountError = false) }

    fun onWhatIfLabelChange(value: String) = updateForms { it.copy(whatIfLabel = value) }

    fun onWhatIfAmountChange(value: String) = updateForms { it.copy(whatIfAmount = value, whatIfAmountError = false) }

    /** Nothing is written: a what-if is only a question (Doc 1 section 4.3). */
    fun onSubmitWhatIf() {
        val form = _forms.value
        val price = MoneyParser.parseToMinor(form.whatIfAmount)
            ?: return updateForms { it.copy(whatIfAmountError = true) }
        savedState[KEY_SHOWN_PRICE] = price
        savedState[KEY_SHOWN_LABEL] = form.whatIfLabel.trim()
        updateForms { it.copy(sheet = null, whatIfLabel = "", whatIfAmount = "") }
    }

    fun onDismissWhatIf() {
        savedState[KEY_SHOWN_PRICE] = 0L
    }

    // ---- Add to fund ----

    fun onOpenAddToFund() {
        // One client id per sheet: a double tap or a retry writes the same document (BR-19).
        if (savedState.get<String>(KEY_FUND_TX_ID) == null) savedState[KEY_FUND_TX_ID] = UUID.randomUUID().toString()
        val firstAccount = (uiState.value as? UiState.Success)?.data?.fromAccounts?.firstOrNull()?.account?.id
        updateForms {
            it.copy(sheet = GoalSheet.AddToFund, fundFromId = it.fundFromId ?: firstAccount, fundAmountError = false, fundFromError = false)
        }
    }

    fun onFundAmountChange(value: String) = updateForms { it.copy(fundAmount = value, fundAmountError = false) }

    fun onFundFromChange(accountId: String) = updateForms { it.copy(fundFromId = accountId, fundFromError = false) }

    /** A transfer into the goal fund (BR-13, BR-33): not spending, and it raises "saved" at once. */
    fun onSubmitAddToFund() {
        val form = _forms.value
        if (form.sheet != GoalSheet.AddToFund) return // already submitted: the sheet is closing
        val runway = (uiState.value as? UiState.Success)?.data ?: return
        val amount = MoneyParser.parseToMinor(form.fundAmount)
        val from = form.fundFromId?.takeIf { id -> runway.fromAccounts.any { it.account.id == id } }
        if (amount == null || from == null) {
            updateForms { it.copy(fundAmountError = amount == null, fundFromError = from == null) }
            return
        }
        val id = savedState.get<String>(KEY_FUND_TX_ID) ?: UUID.randomUUID().toString()
        savedState.remove<String>(KEY_FUND_TX_ID)
        updateForms { it.copy(sheet = null, fundAmount = "") }

        viewModelScope.launch {
            val draft = TransactionDraft(
                id = id,
                type = TransactionType.TRANSFER,
                amountMinor = amount,
                date = time.now(),
                accountId = from,
                toAccountId = runway.goal.fundAccountId,
            )
            val event = when (val result = addTransaction(draft)) {
                is AppResult.Success -> GoalEvent.AddedToFund(id, amount, runway.fundName)
                is AppResult.Failure -> GoalEvent.Failed(result.error)
            }
            _events.send(event)
        }
    }

    /** Undo archives the transfer (BR-16, BR-17): it leaves every total, nothing is deleted. */
    fun onUndoAddToFund(transactionId: String) {
        viewModelScope.launch { transactions.archive(transactionId) }
    }

    private fun updateForms(change: (GoalForms) -> GoalForms) {
        val next = change(_forms.value)
        _forms.value = next
        savedState[KEY_SHEET] = next.sheet?.name
        savedState[KEY_WHATIF_LABEL] = next.whatIfLabel
        savedState[KEY_WHATIF_AMOUNT] = next.whatIfAmount
        savedState[KEY_FUND_AMOUNT] = next.fundAmount
        savedState[KEY_FUND_FROM] = next.fundFromId
    }

    private fun restoreForms() = GoalForms(
        sheet = savedState.get<String>(KEY_SHEET)?.let { name -> GoalSheet.entries.firstOrNull { it.name == name } },
        whatIfLabel = savedState[KEY_WHATIF_LABEL] ?: "",
        whatIfAmount = savedState[KEY_WHATIF_AMOUNT] ?: "",
        fundAmount = savedState[KEY_FUND_AMOUNT] ?: "",
        fundFromId = savedState[KEY_FUND_FROM],
    )

    private companion object {
        const val KEY_SHEET = "goal_sheet"
        const val KEY_WHATIF_LABEL = "goal_whatif_label"
        const val KEY_WHATIF_AMOUNT = "goal_whatif_amount"
        const val KEY_SHOWN_PRICE = "goal_whatif_shown_price"
        const val KEY_SHOWN_LABEL = "goal_whatif_shown_label"
        const val KEY_FUND_AMOUNT = "goal_fund_amount"
        const val KEY_FUND_FROM = "goal_fund_from"
        const val KEY_FUND_TX_ID = "goal_fund_tx_id"
    }
}
