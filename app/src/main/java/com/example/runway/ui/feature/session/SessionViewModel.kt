package com.example.runway.ui.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.domain.repository.AuthRepository
import com.example.runway.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update

/**
 * Combines "who is signed in" with "has onboarding finished" into one routing state.
 * Activity-scoped: it survives rotation, so the splash screen never comes back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: UserProfileRepository,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val state: StateFlow<SessionState> = retryTick
        .flatMapLatest { sessionFlow().catch { emit(SessionState.Problem(it.toAppError())) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)

    /** "Try again" on the problem screen: restarts the whole session flow. */
    fun retry() {
        retryTick.update { it + 1 }
    }

    private fun sessionFlow(): Flow<SessionState> =
        authRepository.authState.flatMapLatest { user ->
            if (user == null) {
                flowOf(SessionState.LoggedOut)
            } else {
                signedInFlow().catch { emit(SessionState.Problem(it.toAppError())) }
            }
        }

    private fun signedInFlow(): Flow<SessionState> = flow {
        var repairStarted = false // once per sign-in: ask the server, re-seed only if really missing
        emitAll(
            profileRepository.observeProfile().transform { profile ->
                when {
                    profile != null ->
                        emit(if (profile.onboardingComplete) SessionState.Ready else SessionState.NeedsOnboarding)

                    !repairStarted -> {
                        repairStarted = true
                        val repair = authRepository.ensureProfile()
                        if (repair is AppResult.Failure) emit(SessionState.Problem(repair.error))
                    }
                    // else: the seeded doc is on its way; the listener will emit it
                }
            },
        )
    }
}
