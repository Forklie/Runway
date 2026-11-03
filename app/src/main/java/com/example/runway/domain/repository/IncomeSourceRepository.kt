package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.IncomeSource
import kotlinx.coroutines.flow.Flow

interface IncomeSourceRepository {
    fun observeSources(): Flow<List<IncomeSource>>

    suspend fun add(source: IncomeSource): AppResult<Unit>
    suspend fun addAll(sources: List<IncomeSource>): AppResult<Unit> // one atomic batch
    suspend fun update(source: IncomeSource): AppResult<Unit>
}
