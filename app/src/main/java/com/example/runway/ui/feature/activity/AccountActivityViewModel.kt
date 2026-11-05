package com.example.runway.ui.feature.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.MonthKey
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.AccountMovement
import com.example.runway.domain.repository.BankSmsRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.AccountActivity
import com.example.runway.domain.usecase.summarizeAccountActivity
import com.example.runway.domain.usecase.unloggedDebits
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class AccountActivityContent(
    val accounts: List<AccountActivity>,
    val toAdd: List<AccountMovement>, // debits not logged in Runway yet
    val zone: ZoneId,
)

/**
 * One month of bank SMS, measured per account, with the debits not logged in Runway yet. The inbox is read only while uiState is collected,
 * so the screen collects it only once READ_SMS is granted. Coming back to the screen reads it again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountActivityViewModel @Inject constructor(
    private val sms: BankSmsRepository,
    private val transactions: TransactionRepository,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val month: StateFlow<String> = savedState.getStateFlow(KEY_MONTH, time.currentMonthKey())

    val uiState: StateFlow<UiState<AccountActivityContent>> = combine(retryTick, month) { _, key -> key }
        .flatMapLatest { key ->
            val movements = flow {
                val from = checkNotNull(MonthKey.startOf(key, time.zone))
                val until = checkNotNull(MonthKey.next(key)?.let { MonthKey.startOf(it, time.zone) })
                emit(sms.movements(from, until))
            }
            // Without the ledger every debit is offered, which beats losing the measurements
            val logged = transactions.observeMonth(key).catch { emit(emptyList()) }
            combine(movements, logged) { moves, ledger ->
                val accounts = summarizeAccountActivity(moves, time.zone)
                if (accounts.isEmpty()) UiState.Empty
                else UiState.Success(AccountActivityContent(accounts, unloggedDebits(moves, ledger, time.zone), time.zone))
            }
                .onStart<UiState<AccountActivityContent>> { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun canGoNext(key: String): Boolean = key < time.currentMonthKey()

    fun onPreviousMonth() = moveTo(MonthKey.previous(month.value))

    fun onNextMonth() = moveTo(MonthKey.next(month.value)?.takeIf { canGoNext(month.value) })

    fun retry() = retryTick.update { it + 1 }

    private fun moveTo(key: String?) {
        if (key != null) savedState[KEY_MONTH] = key
    }

    private companion object {
        const val KEY_MONTH = "account_activity_month"
    }
}
