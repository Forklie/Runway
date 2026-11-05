package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.BalanceCheckin
import kotlinx.coroutines.flow.Flow

interface BalanceCheckinRepository {
    fun observeAll(): Flow<List<BalanceCheckin>> // newest first

    /**
     * Saves the check-ins AND moves each account's checkpoint to its entered balance at the check-in
     * date, in ONE batch (BR-83). Afterwards expected balances start from the truth.
     */
    suspend fun saveCheckin(checkins: List<BalanceCheckin>): AppResult<Unit>
}
