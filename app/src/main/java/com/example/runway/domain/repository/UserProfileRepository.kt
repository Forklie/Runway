package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    /** Re-emits on every change. Null while users/{uid} does not exist yet. */
    fun observeProfile(): Flow<UserProfile?>

    suspend fun save(profile: UserProfile): AppResult<Unit>
    suspend fun setOnboardingComplete(complete: Boolean): AppResult<Unit>
    suspend fun setPyfPercent(percent: Double): AppResult<Unit>
    suspend fun setDefaultAccount(accountId: String?): AppResult<Unit>
    suspend fun setLastFxRate(currency: CurrencyCode, rate: Double): AppResult<Unit>
}