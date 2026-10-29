package com.example.runway.ui.feature.session

import com.example.runway.ui.feature.auth.LoginDest
import com.example.runway.ui.navigation.HomeDest

/**
 * The route to show for a session state, or null while there is nothing to route to yet.
 * Day 3: NeedsOnboarding will route to OnboardingDest. Until it exists it goes Home,
 * so teammates signing in are never stuck on a placeholder.
 */
fun SessionState.startDestination(): Any? = when (this) {
    SessionState.Loading, is SessionState.Problem -> null
    SessionState.LoggedOut -> LoginDest
    SessionState.NeedsOnboarding -> HomeDest // Day 3: OnboardingDest
    SessionState.Ready -> HomeDest
}
