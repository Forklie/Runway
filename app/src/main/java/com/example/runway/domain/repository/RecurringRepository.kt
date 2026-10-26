package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.RecurringItem
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun observeAll(): Flow<List<RecurringItem>>
    fun observeSubscriptions(): Flow<List<RecurringItem>> // kind == SUBSCRIPTION, active and inactive

    /** One-off read for GenerateRecurring: active items with nextDueDate <= until. */
    suspend fun getDue(until: Instant): AppResult<List<RecurringItem>>

    suspend fun add(item: RecurringItem): AppResult<Unit>
    suspend fun addAll(items: List<RecurringItem>): AppResult<Unit> // one atomic batch
    suspend fun update(item: RecurringItem): AppResult<Unit>
    suspend fun setActive(id: String, active: Boolean): AppResult<Unit>
}