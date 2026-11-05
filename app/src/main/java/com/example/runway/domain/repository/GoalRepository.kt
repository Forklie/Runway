package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.Goal
import kotlinx.coroutines.flow.Flow

/** Writes return after the LOCAL write, never after server acknowledgement. */
interface GoalRepository {
    /** The one active goal (BR-70), or null until onboarding or the goal form saves one. Re-emits on change. */
    fun observeActiveGoal(): Flow<Goal?>

    /** Creates or replaces the goal document. */
    suspend fun save(goal: Goal): AppResult<Unit>
}
