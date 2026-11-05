package com.example.runway.ui.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.result.toAppError
import com.example.runway.domain.usecase.Insights
import com.example.runway.domain.usecase.ObserveInsights
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Insights tab. Each card has its own empty state, so the screen itself is never Empty. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    observeInsights: ObserveInsights,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<UiState<Insights>> = retryTick
        .flatMapLatest {
            observeInsights()
                .map<Insights, UiState<Insights>> { UiState.Success(it) }
                .onStart { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun retry() {
        retryTick.update { it + 1 }
    }
}
