package com.example.runway.ui.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.repository.RecurringRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.domain.usecase.ComputeSubscriptionCost
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One subscription on the audit screen. Costs are null when a foreign currency has no known rate. */
data class SubscriptionRow(
    val id: String,
    val name: String,
    val monthlyLkrMinor: Long?,
    val yearlyLkrMinor: Long?,
    /** Answered "yes, keep it" in this session. Not stored: keeping changes nothing. */
    val kept: Boolean,
)

data class SubscriptionsContent(
    /** Active subscriptions, still to review or kept. */
    val active: List<SubscriptionRow>,
    /** Answered "no": active = false (BR-93). */
    val cancelled: List<SubscriptionRow>,
    val monthlyTotalMinor: Long,
    val yearlyTotalMinor: Long,
    /** Yearly cost of everything answered "no": the annual saving. */
    val yearlySavedMinor: Long,
)

/**
 * Subscription audit (F-16, BR-92, BR-93). Reads the live list, asks "still using this?" per item.
 * "No" sets active = false and the row moves to "marked as cancelled" with its annual saving.
 */
@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    profileRepository: UserProfileRepository,
    private val computeCost: ComputeSubscriptionCost,
) : ViewModel() {

    private val keptIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<UiState<SubscriptionsContent>> = combine(
        recurringRepository.observeSubscriptions(),
        profileRepository.observeProfile(),
        keptIds,
    ) { items, profile, kept ->
        val state: UiState<SubscriptionsContent> =
            if (items.isEmpty()) UiState.Empty else UiState.Success(build(items, profile?.lastFxRates.orEmpty(), kept))
        state
    }
        .catch { emit(UiState.Error(it.toAppError())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _actionError = MutableStateFlow<AppError?>(null)

    /** A change that could not even be queued. Shown as a banner. */
    val actionError: StateFlow<AppError?> = _actionError.asStateFlow()

    /** "Yes, keep it": nothing is written. Only this session remembers, so the prompt goes away. */
    fun onKeep(id: String) = keptIds.update { it + id }

    /** "No, cancel it": BR-93. The saving appears as soon as the local write lands. */
    fun onCancel(id: String) = setActive(id, active = false)

    /** Undo a "no". */
    fun onRestore(id: String) {
        keptIds.update { it - id }
        setActive(id, active = true)
    }

    fun onDismissError() {
        _actionError.value = null
    }

    private fun setActive(id: String, active: Boolean) {
        _actionError.value = null
        viewModelScope.launch {
            val result = recurringRepository.setActive(id, active)
            if (result is AppResult.Failure) _actionError.value = result.error
        }
    }

    private fun build(items: List<RecurringItem>, rates: Map<String, Double>, kept: Set<String>): SubscriptionsContent {
        fun row(item: RecurringItem): SubscriptionRow {
            val cost = computeCost(item, rates)
            return SubscriptionRow(item.id, item.name, cost?.monthlyLkrMinor, cost?.yearlyLkrMinor, item.id in kept)
        }
        val (activeItems, cancelledItems) = items.partition { it.active }
        val active = activeItems.map(::row)
        val cancelled = cancelledItems.map(::row)
        return SubscriptionsContent(
            active = active,
            cancelled = cancelled,
            monthlyTotalMinor = active.sumOf { it.monthlyLkrMinor ?: 0L },
            yearlyTotalMinor = active.sumOf { it.yearlyLkrMinor ?: 0L },
            yearlySavedMinor = cancelled.sumOf { it.yearlyLkrMinor ?: 0L },
        )
    }
}
