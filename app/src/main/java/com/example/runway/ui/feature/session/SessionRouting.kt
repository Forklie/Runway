package com.example.runway.ui.feature.session

import com.example.runway.ui.feature.auth.LoginDest
import com.example.runway.ui.feature.auth.RegisterDest
import com.example.runway.ui.feature.auth.ResetPasswordDest
import com.example.runway.ui.feature.onboarding.OnboardingDest
import com.example.runway.ui.navigation.HomeDest

/** The route to show for a session state, or null while there is nothing to route to yet (FR-02). */
fun SessionState.startDestination(): Any? = when (this) {
    SessionState.Loading, is SessionState.Problem -> null
    SessionState.LoggedOut -> LoginDest
    SessionState.NeedsOnboarding -> OnboardingDest
    SessionState.Ready -> HomeDest
}

/**
 * The three areas of the app. RunwayShell moves the user only when the session points at a DIFFERENT
 * group than the screen they are on: signing in leaves Auth, finishing onboarding leaves Onboarding,
 * signing out leaves Main. Moving inside a group (Home to Settings) is never touched.
 */
enum class RouteGroup { Auth, Onboarding, Main }

fun routeGroupOf(route: Any): RouteGroup = when (route) {
    LoginDest, RegisterDest, ResetPasswordDest -> RouteGroup.Auth
    OnboardingDest -> RouteGroup.Onboarding
    else -> RouteGroup.Main
}
