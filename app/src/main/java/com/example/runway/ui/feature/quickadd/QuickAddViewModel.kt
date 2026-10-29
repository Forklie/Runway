package com.example.runway.ui.feature.quickadd

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
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.SuggestFavourites
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

/** What the sheet offers: chips in Quick Add order and the accounts money can leave from. */
data class QuickAddOptions(
    val favourites: List<Category>,
    val otherCategories: List<Category>,
    val accounts: List<Account>,
    val defaultAccountId: String?,
) {
    val allCategories: List<Category> get() = favourites + otherCategories
}

/** Typed input. Amount is a String, never a Double: it is validated with MoneyParser (BR-07). */
data class QuickAddForm(
    val open: Boolean = false,
    val amount: String = "",
    val categoryId: String? = null,
    val note: String = "",
    val date: LocalDate? = null,   // null means today
    val accountId: String? = null, // null means the default account
) {
    val amountError: Boolean get() = amount.isNotBlank() && MoneyParser.parseToMinor(amount) == null
    val canSave: Boolean get() = categoryId != null && MoneyParser.parseToMinor(amount) != null
}

sealed interface QuickAddEvent {
    data class Saved(val transactionId: String, val amountMinor: Long, val categoryName: String) : QuickAddEvent
    data class Failed(val error: AppError) : QuickAddEvent
}

/**
 * Quick Add (F-07, Doc 1 section 9.3). Activity scoped, so it outlives the sheet and Undo still works.
 * Typed input survives rotation and process death through SavedStateHandle (AT-21).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    categories: CategoryRepository,
    accounts: AccountRepository,
    profile: UserProfileRepository,
    suggestFavourites: SuggestFavourites,
    private val transactions: TransactionRepository,
    private val addTransaction: AddTransaction,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val options: StateFlow<UiState<QuickAddOptions>> = retryTick
        .flatMapLatest {
            combine(
                categories.observeCategories(),
                transactions.observeSince(SuggestFavourites.windowStart(time.now())),
                accounts.observeAccounts(),
                profile.observeProfile(),
            ) { allCategories, recent, allAccounts, userProfile ->
                val favourites = suggestFavourites(allCategories, recent, time.now())
                val spendable = allAccounts.filter { it.kind != AccountKind.GOAL_FUND }
                val options = QuickAddOptions(
                    favourites = favourites,
                    otherCategories = allCategories - favourites.toSet(),
                    accounts = spendable,
                    defaultAccountId = defaultAccountId(recent, userProfile?.defaultAccountId, spendable),
                )
                if (allCategories.isEmpty()) UiState.Empty else UiState.Success(options)
            }
                .onStart<UiState<QuickAddOptions>> { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _form = MutableStateFlow(restoreForm())
    val form: StateFlow<QuickAddForm> = _form.asStateFlow()

    private val _events = Channel<QuickAddEvent>(Channel.BUFFERED)
    val events: Flow<QuickAddEvent> = _events.receiveAsFlow()

    val today: LocalDate get() = time.today()

    fun retry() {
        retryTick.update { it + 1 }
    }

    fun onOpen() {
        // One client id per sheet: a double tap or a retry writes the same document (BR-19).
        if (savedState.get<String>(KEY_TX_ID) == null) savedState[KEY_TX_ID] = UUID.randomUUID().toString()
        updateForm { it.copy(open = true) }
    }

    fun onDismiss() = updateForm { QuickAddForm() }

    fun onAmountChange(value: String) = updateForm { it.copy(amount = value) }

    fun onCategoryChange(id: String) = updateForm { it.copy(categoryId = id) }

    fun onNoteChange(value: String) = updateForm { it.copy(note = value.take(AddTransaction.MAX_NOTE)) }

    /** Back-dating is fine, a future date is not (BR-18). */
    fun onDateChange(date: LocalDate) = updateForm { it.copy(date = date.takeIf { d -> d < today }) }

    fun onAccountChange(id: String) = updateForm { it.copy(accountId = id) }

    /** Closes the sheet at once and writes to the local cache, so it never waits on the network (NFR-01). */
    fun onSave() {
        val form = _form.value
        if (!form.open) return // already saved: a second tap on the closing sheet
        val options = (options.value as? UiState.Success)?.data ?: return
        val amount = MoneyParser.parseToMinor(form.amount) ?: return
        val category = options.allCategories.firstOrNull { it.id == form.categoryId } ?: return
        val id = savedState.get<String>(KEY_TX_ID) ?: return
        updateForm { it.copy(open = false) }

        viewModelScope.launch {
            val draft = TransactionDraft(
                id = id,
                type = TransactionType.EXPENSE,
                amountMinor = amount,
                date = form.date?.atStartOfDay(time.zone)?.toInstant() ?: time.now(),
                categoryId = category.id,
                accountId = form.accountId ?: options.defaultAccountId,
                note = form.note,
            )
            when (val result = addTransaction(draft)) {
                is AppResult.Success -> {
                    savedState.remove<String>(KEY_TX_ID)
                    updateForm { QuickAddForm() }
                    _events.send(QuickAddEvent.Saved(id, amount, category.name))
                }
                is AppResult.Failure -> {
                    updateForm { it.copy(open = true) } // the typed input is still there to fix or retry
                    _events.send(QuickAddEvent.Failed(result.error))
                }
            }
        }
    }

    /** Undo archives the new expense (BR-17): it leaves every total, nothing is deleted. */
    fun onUndo(transactionId: String) {
        viewModelScope.launch { transactions.archive(transactionId) }
    }

    /** Last used account first (Doc 1 section 4.4), then the profile's default, then the first one. */
    private fun defaultAccountId(recent: List<Transaction>, profileDefault: String?, accounts: List<Account>): String? {
        val ids = accounts.map { it.id }
        val lastUsed = recent.firstOrNull { it.type == TransactionType.EXPENSE && it.accountId in ids }?.accountId
        return lastUsed ?: profileDefault?.takeIf { it in ids } ?: ids.firstOrNull()
    }

    private fun updateForm(change: (QuickAddForm) -> QuickAddForm) {
        val next = change(_form.value)
        _form.value = next
        savedState[KEY_OPEN] = next.open
        savedState[KEY_AMOUNT] = next.amount
        savedState[KEY_CATEGORY] = next.categoryId
        savedState[KEY_NOTE] = next.note
        savedState[KEY_DATE] = next.date?.toString()
        savedState[KEY_ACCOUNT] = next.accountId
    }

    private fun restoreForm() = QuickAddForm(
        open = savedState[KEY_OPEN] ?: false,
        amount = savedState[KEY_AMOUNT] ?: "",
        categoryId = savedState[KEY_CATEGORY],
        note = savedState[KEY_NOTE] ?: "",
        date = savedState.get<String>(KEY_DATE)?.let(LocalDate::parse),
        accountId = savedState[KEY_ACCOUNT],
    )

    private companion object {
        const val KEY_OPEN = "quickadd_open"
        const val KEY_AMOUNT = "quickadd_amount"
        const val KEY_CATEGORY = "quickadd_category"
        const val KEY_NOTE = "quickadd_note"
        const val KEY_DATE = "quickadd_date"
        const val KEY_ACCOUNT = "quickadd_account"
        const val KEY_TX_ID = "quickadd_tx_id"
    }
}
