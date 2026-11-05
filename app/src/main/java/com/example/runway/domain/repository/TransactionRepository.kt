package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.Transaction
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Doc 2 section 5.6. observe* flows stay active and re-emit on every change; archived rows are already
 * filtered out. Writes return after the LOCAL write, never after server acknowledgement.
 */
interface TransactionRepository {
    fun observeMonth(monthKey: String): Flow<List<Transaction>>   // date desc
    fun observeSince(from: Instant): Flow<List<Transaction>>      // balances and goal pace, date desc
    fun observeIncomeSince(from: Instant): Flow<List<Transaction>> // INCOME only, date desc
    fun observeById(id: String): Flow<Transaction?>

    suspend fun add(tx: Transaction): AppResult<Unit>
    suspend fun addAll(transactions: List<Transaction>): AppResult<Unit> // one atomic batch (GenerateRecurring)
    suspend fun update(tx: Transaction): AppResult<Unit>
    suspend fun archive(id: String, archived: Boolean = true): AppResult<Unit>
}
