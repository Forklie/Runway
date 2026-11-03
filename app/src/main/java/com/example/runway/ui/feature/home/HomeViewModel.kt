package com.example.runway.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.result.toAppError
import com.example.runway.domain.usecase.HomeSummary
import com.example.runway.domain.usecase.ObserveHomeSummary
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

/**
 * Maps the live Home summary to UiState. No formulas here: they live in ObserveHomeSummary.
 * WhileSubscribed(5_000) detaches the Firestore listeners 5 seconds after Home leaves the screen.
 * The "Saved on this phone" chip is the shell's (RunwayShell), shown on every screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHome: ObserveHomeSummary,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<UiState<HomeSummary>> = retryTick
        .flatMapLatest {
            observeHome()
                .map<HomeSummary, UiState<HomeSummary>> { if (it.isEmpty) UiState.Empty else UiState.Success(it) }
                .onStart { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** "Try again" on the error state: restarts every listener. */
    fun retry() {
        retryTick.update { it + 1 }
    }
}
