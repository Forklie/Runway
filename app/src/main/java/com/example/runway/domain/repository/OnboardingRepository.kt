package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.OnboardingPlan

interface OnboardingRepository {
    /**
     * Writes everything onboarding collected as ONE atomic batch and marks onboardingComplete = true.
     * Returns after the LOCAL write, never after server acknowledgement (Doc 2 §5.8).
     */
    suspend fun complete(plan: OnboardingPlan): AppResult<Unit>
}
