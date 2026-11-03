package com.example.runway.domain.model

import java.time.Instant

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: CurrencyCode,
    val fxRate: Double,
    val amountLkrMinor: Long,
    val categoryId: String?,
    val accountId: String?,
    val toAccountId: String?,
    val incomeSourceId: String?,
    val relatedTxId: String?,
    val note: String,
    val date: Instant,
    val monthKey: String,
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
