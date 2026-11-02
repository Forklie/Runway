package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.CryptoEntry
import kotlinx.coroutines.flow.Flow

interface CryptoRepository {
    fun observeEntries(): Flow<List<CryptoEntry>> // newest first

    suspend fun add(entry: CryptoEntry): AppResult<Unit>

    /** Only for Undo straight after [add]. Otherwise ledger history is kept. */
    suspend fun remove(id: String): AppResult<Unit>
}
