package com.example.runway.ui.feature.onboarding

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Onboarding screens. AppNavHost calls this in one line.
 * There is no "go home" here: finishing flips onboardingComplete, the session becomes Ready and
 * RunwayShell does the routing (same idea as auth).
 */
@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.onboardingScreens(nav: NavController) {
    composable<OnboardingDest> { OnboardingRoute() }
}
