package com.example.runway.ui.feature.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.TransactionDraft
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransferForm(
    val amount: String = "",
    val fromId: String? = null, // null means the default: the first bank account
    val toId: String? = null,   // null means the default: Cash, so an ATM withdrawal takes two taps
    val note: String = "",
    val amountError: Boolean = false,
    val formError: AppError? = null,
    val saving: Boolean = false,
)

/** Moves money between two accounts (BR-13). Never income or expense. An ATM withdrawal is bank to Cash (AT-15). */
@HiltViewModel
class TransferViewModel @Inject constructor(
    accounts: AccountRepository,
    private val addTransaction: AddTransaction,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    val accounts: StateFlow<UiState<List<Account>>> = accounts.observeAccounts()
        .map<List<Account>, UiState<List<Account>>> { if (it.size < 2) UiState.Empty else UiState.Success(it) }
        .onStart { emit(UiState.Loading) }
        .catch { emit(UiState.Error(it.toAppError())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _form = MutableStateFlow(
        TransferForm(
            amount = savedState[KEY_AMOUNT] ?: "",
            fromId = savedState[KEY_FROM],
            toId = savedState[KEY_TO],
            note = savedState[KEY_NOTE] ?: "",
        ),
    )
    val form: StateFlow<TransferForm> = _form.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    /** One client id per screen: a double tap or a retry writes the same document (BR-19). */
    private val id: String = savedState.get<String>(KEY_ID) ?: UUID.randomUUID().toString().also { savedState[KEY_ID] = it }

    fun onAmountChange(value: String) = update { it.copy(amount = value, amountError = false) }

    fun onFromChange(id: String) = update { it.copy(fromId = id) }

    fun onToChange(id: String) = update { it.copy(toId = id) }

    fun onNoteChange(value: String) = update { it.copy(note = value.take(AddTransaction.MAX_NOTE)) }

    fun onSave() {
        val form = _form.value
        if (form.saving) return
        val all = (accounts.value as? UiState.Success)?.data ?: return
        val amount = MoneyParser.parseToMinor(form.amount)
        if (amount == null) {
            update { it.copy(amountError = true) }
            return
        }
        val (from, to) = selected(form, all)
        update { it.copy(saving = true, formError = null) }
        viewModelScope.launch {
            val draft = TransactionDraft(
                id = id,
                type = TransactionType.TRANSFER,
                amountMinor = amount,
                date = time.now(),
                accountId = from,
                toAccountId = to,
                note = form.note,
            )
            when (val result = addTransaction(draft)) {
                is AppResult.Success -> _done.send(Unit)
                is AppResult.Failure -> update { it.copy(saving = false, formError = result.error) }
            }
        }
    }

    private fun update(change: (TransferForm) -> TransferForm) {
        val next = change(_form.value)
        _form.value = next
        savedState[KEY_AMOUNT] = next.amount
        savedState[KEY_FROM] = next.fromId
        savedState[KEY_TO] = next.toId
        savedState[KEY_NOTE] = next.note
    }

    private companion object {
        const val KEY_AMOUNT = "transfer_amount"
        const val KEY_FROM = "transfer_from"
        const val KEY_TO = "transfer_to"
        const val KEY_NOTE = "transfer_note"
        const val KEY_ID = "transfer_id"
    }
}

/** The accounts the form points at, with defaults filled in: the first bank account to Cash. */
fun selected(form: TransferForm, accounts: List<Account>): Pair<String?, String?> {
    val from = form.fromId ?: (accounts.firstOrNull { it.kind == AccountKind.BANK } ?: accounts.firstOrNull())?.id
    val to = form.toId ?: (accounts.firstOrNull { it.kind == AccountKind.CASH && it.id != from } ?: accounts.firstOrNull { it.id != from })?.id
    return from to to
}
