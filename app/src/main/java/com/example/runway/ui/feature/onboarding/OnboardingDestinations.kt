package com.example.runway.ui.feature.onboarding

import kotlinx.serialization.Serializable

/**
 * One route for all five steps: the step number lives in OnboardingViewModel (and SavedStateHandle),
 * so there is one draft, one ViewModel and nothing to pass between destinations.
 * RunwayShell uses it to hide the bottom bar and to tell onboarding apart from the main app.
 */
@Serializable data object OnboardingDest
