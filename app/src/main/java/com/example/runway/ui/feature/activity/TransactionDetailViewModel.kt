package com.example.runway.ui.feature.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.money.FxMath
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.MonthKey
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.TransactionDraft
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailForm(
    val amount: String = "",
    val categoryId: String? = null,
    val note: String = "",
    val date: LocalDate? = null,
    val amountError: Boolean = false,
    val refund: String? = null, // null while the refund sheet is closed
    val refundError: Boolean = false,
    val formError: AppError? = null,
)

data class DetailContent(val tx: Transaction, val categories: List<Category>)

/**
 * Fix a mistake (F-08): edit, archive or refund one transaction. An edit keeps the stored rate (BR-05, AT-22).
 * The route's id arrives through SavedStateHandle. Typed input survives process death there too.
 */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    private val addTransaction: AddTransaction,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val id: String = savedState[KEY_ID] ?: ""

    private val _uiState = MutableStateFlow<UiState<DetailContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<DetailContent>> = _uiState.asStateFlow()

    private val _form = MutableStateFlow(
        DetailForm(
            amount = savedState[KEY_AMOUNT] ?: "",
            categoryId = savedState[KEY_CATEGORY],
            note = savedState[KEY_NOTE] ?: "",
            date = savedState.get<String>(KEY_DATE)?.let(LocalDate::parse),
            refund = savedState[KEY_REFUND],
        ),
    )
    val form: StateFlow<DetailForm> = _form.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    private var finishing = false

    val today: LocalDate get() = time.today()

    init {
        viewModelScope.launch {
            combine(transactions.observeById(id), categories.observeCategories()) { tx, all ->
                if (tx == null || tx.archived) UiState.Error(AppError.NotFound) else UiState.Success(DetailContent(tx, all))
            }
                .catch { emit(UiState.Error(it.toAppError())) }
                .collect { state ->
                    (state as? UiState.Success)?.data?.tx?.let(::fillOnce)
                    _uiState.value = state
                }
        }
    }

    /** First load only: the listener re-emits, and a re-emit must not overwrite what is being typed. */
    private fun fillOnce(tx: Transaction) {
        if (savedState.contains(KEY_AMOUNT)) return
        update {
            it.copy(
                amount = BigDecimal.valueOf(tx.amountMinor, 2).stripTrailingZeros().toPlainString(),
                categoryId = tx.categoryId,
                note = tx.note,
                date = tx.date.atZone(time.zone).toLocalDate(),
            )
        }
    }

    fun onAmountChange(value: String) = update { it.copy(amount = value, amountError = false) }

    fun onCategoryChange(id: String) = update { it.copy(categoryId = id) }

    fun onNoteChange(value: String) = update { it.copy(note = value.take(AddTransaction.MAX_NOTE)) }

    fun onDateChange(date: LocalDate) = update { it.copy(date = date.takeIf { d -> d <= today } ?: it.date) }

    fun onSave() {
        val tx = (_uiState.value as? UiState.Success)?.data?.tx ?: return
        val form = _form.value
        val amount = MoneyParser.parseToMinor(form.amount)
        if (amount == null || FxMath.toLkrMinor(amount, tx.fxRate) !in 1..AddTransaction.MAX_MINOR) {
            update { it.copy(amountError = true) }
            return
        }
        val date = form.date ?: return
        val movedDay = date != tx.date.atZone(time.zone).toLocalDate()
        val instant = if (movedDay) date.atStartOfDay(time.zone).toInstant() else tx.date
        val edited = tx.withAmount(amount, time.now()).copy(
            categoryId = form.categoryId,
            note = form.note.trim(),
            date = instant,
            monthKey = MonthKey.of(instant, time.zone),
        )
        finish { transactions.update(edited) }
    }

    /** Archived, never deleted (BR-16): it leaves every total and every list. */
    fun onArchive() = finish { transactions.archive(id) }

    fun onRefundOpen() {
        if (savedState.get<String>(KEY_REFUND_ID) == null) savedState[KEY_REFUND_ID] = UUID.randomUUID().toString()
        update { it.copy(refund = "", refundError = false) }
    }

    fun onRefundDismiss() = update { it.copy(refund = null, refundError = false) }

    fun onRefundChange(value: String) = update { it.copy(refund = value, refundError = false) }

    /**
     * Linked to the expense, so net spending in its category drops (BR-14, AT-16).
     * ponytail: checks each refund against the original only, so two refunds can add up to more. Sum the
     * relatedTxId rows here if partial refunds become common.
     */
    fun onRefundSave() {
        val tx = (_uiState.value as? UiState.Success)?.data?.tx ?: return
        val refundId = savedState.get<String>(KEY_REFUND_ID) ?: return
        val amount = MoneyParser.parseToMinor(_form.value.refund.orEmpty())
        if (tx.type != TransactionType.EXPENSE || amount == null || amount > tx.amountLkrMinor) {
            update { it.copy(refundError = true) }
            return
        }
        finish {
            val written = addTransaction(
                TransactionDraft(
                    id = refundId,
                    type = TransactionType.REFUND,
                    amountMinor = amount,
                    date = time.now(),
                    categoryId = tx.categoryId,
                    accountId = tx.accountId,
                    relatedTxId = tx.id,
                ),
            )
            if (written is AppResult.Failure) written else AppResult.Success(Unit)
        }
    }

    private fun finish(write: suspend () -> AppResult<Unit>) {
        if (finishing) return // a double tap must not leave this screen twice
        finishing = true
        viewModelScope.launch {
            when (val result = write()) {
                is AppResult.Success -> _done.send(Unit)
                is AppResult.Failure -> {
                    finishing = false
                    update { it.copy(formError = result.error) }
                }
            }
        }
    }

    private fun update(change: (DetailForm) -> DetailForm) {
        val next = change(_form.value)
        _form.value = next
        savedState[KEY_AMOUNT] = next.amount
        savedState[KEY_CATEGORY] = next.categoryId
        savedState[KEY_NOTE] = next.note
        savedState[KEY_DATE] = next.date?.toString()
        savedState[KEY_REFUND] = next.refund
    }

    private companion object {
        const val KEY_ID = "id" // the route's argument name (TransactionDetailDest.id)
        const val KEY_AMOUNT = "detail_amount"
        const val KEY_CATEGORY = "detail_category"
        const val KEY_NOTE = "detail_note"
        const val KEY_DATE = "detail_date"
        const val KEY_REFUND = "detail_refund"
        const val KEY_REFUND_ID = "detail_refund_id"
    }
}
