package com.example.runway.ui.feature.session

import com.example.runway.core.result.AppError

/** Where the app should send the user right now (FR-02). */
sealed interface SessionState {
    /** Auth or profile not known yet. The system splash screen stays up. */
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data object NeedsOnboarding : SessionState
    data object Ready : SessionState

    /** Signed in, but the profile could not be loaded or repaired (usually offline). Offer Try again. */
    data class Problem(val error: AppError) : SessionState
}
