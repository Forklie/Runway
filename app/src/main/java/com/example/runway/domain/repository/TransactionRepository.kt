package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.Transaction

interface TransactionRepository {
    suspend fun addAll(transactions: List<Transaction>): AppResult<Unit>
}
