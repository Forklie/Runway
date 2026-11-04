package com.example.runway.ui.feature.crypto

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.model.CryptoEntryType
import com.example.runway.domain.repository.CryptoRepository
import com.example.runway.domain.usecase.ComputeCryptoPosition
import com.example.runway.domain.usecase.CryptoPosition
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CryptoLedger(val entries: List<CryptoEntry>, val position: CryptoPosition, val zone: ZoneId)

/** The add-entry sheet. Amount as typed, validated on save (BR-07). */
data class CryptoForm(
    val open: Boolean = false,
    val type: CryptoEntryType = CryptoEntryType.DEPOSIT,
    val asset: String = "",
    val amount: String = "",
    val note: String = "",
    val amountError: Boolean = false,
)

sealed interface CryptoEvent {
    data class Added(val id: String, val amountMinor: Long) : CryptoEvent
    data class Failed(val error: AppError) : CryptoEvent
}

/** Crypto ledger (F-17, FR-21): entries, the net position (BR-61) and one way to add an entry. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val repository: CryptoRepository,
    private val computePosition: ComputeCryptoPosition,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<UiState<CryptoLedger>> = retryTick
        .flatMapLatest {
            repository.observeEntries()
                .map<List<CryptoEntry>, UiState<CryptoLedger>> { entries ->
                    if (entries.isEmpty()) UiState.Empty else UiState.Success(CryptoLedger(entries, computePosition(entries), time.zone))
                }
                .onStart { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _form = MutableStateFlow(
        CryptoForm(
            open = savedState[KEY_OPEN] ?: false,
            type = savedState.get<String>(KEY_TYPE)?.let { name -> CryptoEntryType.entries.firstOrNull { it.name == name } }
                ?: CryptoEntryType.DEPOSIT,
            asset = savedState[KEY_ASSET] ?: "",
            amount = savedState[KEY_AMOUNT] ?: "",
            note = savedState[KEY_NOTE] ?: "",
        ),
    )
    val form: StateFlow<CryptoForm> = _form.asStateFlow()

    private val _events = Channel<CryptoEvent>(Channel.BUFFERED)
    val events: Flow<CryptoEvent> = _events.receiveAsFlow()

    fun retry() {
        retryTick.update { it + 1 }
    }

    fun onOpenAdd() {
        if (savedState.get<String>(KEY_ENTRY_ID) == null) savedState[KEY_ENTRY_ID] = UUID.randomUUID().toString()
        update { it.copy(open = true, amountError = false) }
    }

    fun onDismiss() = update { it.copy(open = false) }
    fun onTypeChange(type: CryptoEntryType) = update { it.copy(type = type) }
    fun onAssetChange(value: String) = update { it.copy(asset = value) }
    fun onAmountChange(value: String) = update { it.copy(amount = value, amountError = false) }
    fun onNoteChange(value: String) = update { it.copy(note = value.take(MAX_NOTE)) }

    fun onSave() {
        val form = _form.value
        if (!form.open) return // already saved: the sheet is closing
        val amount = MoneyParser.parseToMinor(form.amount) ?: return update { it.copy(amountError = true) }
        val id = savedState.get<String>(KEY_ENTRY_ID) ?: UUID.randomUUID().toString()
        savedState.remove<String>(KEY_ENTRY_ID)
        update { CryptoForm(type = it.type, asset = it.asset) } // keep type and asset for the next entry

        viewModelScope.launch {
            val entry = CryptoEntry(
                id = id,
                type = form.type,
                asset = form.asset.trim(),
                amountLkrMinor = amount,
                date = time.now(),
                note = form.note.trim(),
            )
            val event = when (val result = repository.add(entry)) {
                is AppResult.Success -> CryptoEvent.Added(id, amount)
                is AppResult.Failure -> CryptoEvent.Failed(result.error)
            }
            _events.send(event)
        }
    }

    /** Undo straight after adding: the only time a ledger entry is removed. */
    fun onUndo(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }

    private fun update(change: (CryptoForm) -> CryptoForm) {
        val next = change(_form.value)
        _form.value = next
        savedState[KEY_OPEN] = next.open
        savedState[KEY_TYPE] = next.type.name
        savedState[KEY_ASSET] = next.asset
        savedState[KEY_AMOUNT] = next.amount
        savedState[KEY_NOTE] = next.note
    }

    private companion object {
        const val MAX_NOTE = 200
        const val KEY_OPEN = "crypto_open"
        const val KEY_TYPE = "crypto_type"
        const val KEY_ASSET = "crypto_asset"
        const val KEY_AMOUNT = "crypto_amount"
        const val KEY_NOTE = "crypto_note"
        const val KEY_ENTRY_ID = "crypto_entry_id"
    }
}
