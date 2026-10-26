package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.Account
import kotlinx.coroutines.flow.Flow

/** Writes return after the LOCAL write, never after server acknowledgement. */
interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>> // non-archived, oldest first
    fun observeById(id: String): Flow<Account?>

    suspend fun add(account: Account): AppResult<Unit>
    suspend fun addAll(accounts: List<Account>): AppResult<Unit> // one atomic batch
    suspend fun update(account: Account): AppResult<Unit>
    suspend fun archive(id: String, archived: Boolean = true): AppResult<Unit>
}