package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.GoalSettings
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface GoalSettingsRepository {
    /** goals/main. Re-emits on every change. Null while it does not exist (onboarding not finished). */
    fun observeGoal(): Flow<GoalSettings?>

    /**
     * Edits the goal's name and target, and its deadline when [deadline] is not null. Other fields
     * (startDate, fundAccountId, active) are left alone. Returns after the LOCAL write.
     */
    suspend fun updateGoal(name: String, targetMinor: Long, deadline: Instant?): AppResult<Unit>
}
