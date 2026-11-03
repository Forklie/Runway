package com.example.runway.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.network.ConnectivityObserver
import com.example.runway.core.result.AppResult
import com.example.runway.domain.usecase.GenerateRecurring
import com.example.runway.ui.feature.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * App-wide work the shell owns that is not about routing. Activity-scoped, like SessionViewModel.
 * - Connectivity for the "Saved on this phone" chip.
 * - Recurring generation (F-19): once each time the session becomes Ready.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    connectivity: ConnectivityObserver,
    private val generateRecurring: GenerateRecurring,
) : ViewModel() {

    /** Starts as true so the chip never flashes on launch. */
    val isOnline: StateFlow<Boolean> = connectivity.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private var recurringRan = false

    /**
     * Called by RunwayShell on every session change. Ready runs generation once, however often the
     * session re-emits (rotation, profile edits). Signing out re-arms it for the next sign-in.
     * Loading, onboarding and problem states leave it alone.
     */
    fun onSessionChanged(session: SessionState) {
        when (session) {
            SessionState.Ready -> {
                if (recurringRan) return
                recurringRan = true
                viewModelScope.launch {
                    when (val result = generateRecurring()) {
                        is AppResult.Success -> if (result.value > 0) Timber.i("Generated %d recurring transactions", result.value)
                        is AppResult.Failure -> Timber.w("Recurring generation failed: %s", result.error)
                    }
                }
            }
            SessionState.LoggedOut -> recurringRan = false
            else -> Unit
        }
    }
}
