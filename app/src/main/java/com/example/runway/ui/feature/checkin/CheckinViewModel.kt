package com.example.runway.ui.feature.checkin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.repository.BalanceCheckinRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.CheckinAccount
import com.example.runway.domain.usecase.CheckinSetup
import com.example.runway.domain.usecase.ComputeUnaccounted
import com.example.runway.domain.usecase.EnteredBalance
import com.example.runway.domain.usecase.ObserveCheckinSetup
import com.example.runway.domain.usecase.PlanCheckin
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

/**
 * Where the flow is. One step per account, then Review. Typed text is kept per account, as a String,
 * so nothing is lost going back and forth, rotating, or after process death (AT-21).
 */
data class CheckinForm(
    val step: Int = 0,
    val entered: Map<String, String> = emptyMap(),
    val skipped: Set<String> = emptySet(),
    val inputError: Boolean = false,
    val logGaps: Boolean = false,
    val gapCategoryId: String? = null,
    val saving: Boolean = false,
    val saveError: AppError? = null,
)

/** One account as the screen shows it: what was typed (parsed), and the gap it leaves. */
data class CheckinRow(
    val item: CheckinAccount,
    val enteredMinor: Long?,
    val gapMinor: Long?,
    val skipped: Boolean,
)

/** Everything the steps draw, already worked out: the screen does no arithmetic (Z8). */
data class CheckinView(val rows: List<CheckinRow>, val unloggedMinor: Long)

/**
 * Weekly check-in (F-13, FR-16). The rules live in PlanCheckin; this class walks the steps, keeps the
 * input and writes the plan: gap expenses through AddTransaction, then the check-ins and their new
 * checkpoints in one batch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CheckinViewModel @Inject constructor(
    observeSetup: ObserveCheckinSetup,
    private val planCheckin: PlanCheckin,
    private val unaccounted: ComputeUnaccounted,
    private val addTransaction: AddTransaction,
    private val checkins: BalanceCheckinRepository,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<UiState<CheckinSetup>> = retryTick
        .flatMapLatest {
            observeSetup()
                .map<CheckinSetup, UiState<CheckinSetup>> { if (it.accounts.isEmpty()) UiState.Empty else UiState.Success(it) }
                .onStart { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _form = MutableStateFlow(restore())
    val form: StateFlow<CheckinForm> = _form.asStateFlow()

    /** Live gaps as he types: expected − entered for every account (BR-81). */
    val view: StateFlow<CheckinView?> = combine(uiState, _form) { state, form ->
        val setup = (state as? UiState.Success)?.data ?: return@combine null
        val rows = setup.accounts.map { item ->
            val skipped = item.account.id in form.skipped
            val entered = if (skipped) null else planCheckin.parseBalance(form.entered[item.account.id].orEmpty())
            CheckinRow(item, entered, entered?.let { unaccounted.gap(item.expectedMinor, it) }, skipped)
        }
        CheckinView(rows, rows.sumOf { (it.gapMinor ?: 0L).coerceAtLeast(0L) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    /** One id per check-in: every document it writes is derived from it (BR-19). */
    private val sessionId: String = savedState[KEY_SESSION] ?: UUID.randomUUID().toString().also { savedState[KEY_SESSION] = it }

    fun retry() {
        retryTick.update { it + 1 }
    }

    fun onBalanceChange(accountId: String, value: String) = update {
        it.copy(entered = it.entered + (accountId to value), skipped = it.skipped - accountId, inputError = false)
    }

    /** Validates the current account on Continue, never per keystroke. Zero is a fine balance. */
    fun onContinue() {
        val setup = setup() ?: return
        val form = _form.value
        val account = setup.accounts.getOrNull(form.step)?.account ?: return
        if (planCheckin.parseBalance(form.entered[account.id].orEmpty()) == null) {
            update { it.copy(inputError = true) }
            return
        }
        update { it.copy(step = it.step + 1, inputError = false) }
    }

    /** "I don't know right now": that account keeps its checkpoint and is left out of this check-in. */
    fun onSkipAccount() {
        val account = setup()?.accounts?.getOrNull(_form.value.step)?.account ?: return
        update { it.copy(step = it.step + 1, skipped = it.skipped + account.id, inputError = false) }
    }

    /** Back inside the flow. Returns false on the first step, so the screen leaves instead. */
    fun onStepBack(): Boolean {
        if (_form.value.step == 0) return false
        update { it.copy(step = it.step - 1, inputError = false) }
        return true
    }

    fun onLogGapsChange(log: Boolean) = update { it.copy(logGaps = log) }

    fun onGapCategoryChange(categoryId: String) = update { it.copy(gapCategoryId = categoryId) }

    /** [gapNote] comes from the screen's strings ("Unlogged spending"): the ViewModel holds no copy. */
    fun onSave(gapNote: String) {
        val setup = setup() ?: return
        val form = _form.value
        if (form.saving) return
        val entered = setup.accounts.mapNotNull { item ->
            if (item.account.id in form.skipped) return@mapNotNull null
            planCheckin.parseBalance(form.entered[item.account.id].orEmpty())?.let { EnteredBalance(item, it) }
        }
        val category = if (form.logGaps) form.gapCategoryId ?: defaultGapCategory(setup) else null
        val plan = planCheckin.plan(sessionId, entered, category, gapNote, time.now())
        update { it.copy(saving = true, saveError = null) }

        viewModelScope.launch {
            plan.gapExpenses.forEach { draft ->
                val logged = addTransaction(draft)
                if (logged is AppResult.Failure) return@launch update { it.copy(saving = false, saveError = logged.error) }
            }
            when (val saved = checkins.saveCheckin(plan.checkins)) {
                is AppResult.Success -> {
                    savedState.remove<String>(KEY_SESSION)
                    _done.send(Unit)
                }
                is AppResult.Failure -> update { it.copy(saving = false, saveError = saved.error) }
            }
        }
    }

    private fun setup(): CheckinSetup? = (uiState.value as? UiState.Success)?.data

    /** "Other" if it exists, else the first discretionary-looking category: an unlogged gap is a choice. */
    private fun defaultGapCategory(setup: CheckinSetup): String? =
        setup.categories.firstOrNull { it.id == OTHER_CATEGORY }?.id ?: setup.categories.lastOrNull()?.id

    private fun update(change: (CheckinForm) -> CheckinForm) {
        val next = change(_form.value)
        _form.value = next
        savedState[KEY_STEP] = next.step
        savedState[KEY_ENTERED] = HashMap(next.entered)
        savedState[KEY_SKIPPED] = ArrayList(next.skipped)
        savedState[KEY_LOG] = next.logGaps
        savedState[KEY_CATEGORY] = next.gapCategoryId
    }

    private fun restore() = CheckinForm(
        step = savedState[KEY_STEP] ?: 0,
        entered = savedState.get<HashMap<String, String>>(KEY_ENTERED).orEmpty(),
        skipped = savedState.get<ArrayList<String>>(KEY_SKIPPED).orEmpty().toSet(),
        logGaps = savedState[KEY_LOG] ?: false,
        gapCategoryId = savedState[KEY_CATEGORY],
    )

    companion object {
        const val OTHER_CATEGORY = "other" // seeded by data/seed/CategorySeed.kt
        private const val KEY_SESSION = "checkin_session"
        private const val KEY_STEP = "checkin_step"
        private const val KEY_ENTERED = "checkin_entered"
        private const val KEY_SKIPPED = "checkin_skipped"
        private const val KEY_LOG = "checkin_log_gaps"
        private const val KEY_CATEGORY = "checkin_gap_category"
    }
}
