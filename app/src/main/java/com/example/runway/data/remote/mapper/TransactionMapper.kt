package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.TransactionDto
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

/** Null for an unknown type: guessing EXPENSE or INCOME would silently change every total. */
fun TransactionDto.toDomainOrNull(id: String): Transaction? {
    val type = enumValues<TransactionType>().firstOrNull { it.name == type } ?: return null
    return Transaction(
        id = id,
        type = type,
        amountMinor = amountMinor,
        currency = currency.toEnumOr(CurrencyCode.LKR),
        fxRate = fxRate.takeIf { it > 0.0 } ?: 1.0,
        amountLkrMinor = amountLkrMinor,
        categoryId = categoryId,
        accountId = accountId,
        toAccountId = toAccountId,
        incomeSourceId = incomeSourceId,
        relatedTxId = relatedTxId,
        recurringId = recurringId,
        note = note,
        date = date.toInstantOr(),
        monthKey = monthKey,
        archived = archived,
        createdAt = createdAt.toInstantOr(),
        updatedAt = updatedAt.toInstantOr(),
    )
}

fun Transaction.toDto(): TransactionDto = TransactionDto(
    type = type.name,
    amountMinor = amountMinor,
    currency = currency.name,
    fxRate = fxRate,
    amountLkrMinor = amountLkrMinor,
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    incomeSourceId = incomeSourceId,
    relatedTxId = relatedTxId,
    recurringId = recurringId,
    note = note,
    date = date.toTimestamp(),
    monthKey = monthKey,
    archived = archived,
    createdAt = createdAt.toTimestamp(),
    updatedAt = updatedAt.toTimestamp(),
)

/** One malformed document returns null (and is logged) instead of crashing the whole list. */
fun DocumentSnapshot.toTransactionOrNull(): Transaction? =
    try {
        toObject(TransactionDto::class.java)?.toDomainOrNull(id)
            .also { if (it == null) Timber.w("Skipping transaction %s with an unknown type", id) }
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed transaction %s", id)
        null
    }
