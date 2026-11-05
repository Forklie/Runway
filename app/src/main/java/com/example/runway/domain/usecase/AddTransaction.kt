package com.example.runway.domain.usecase

import com.example.runway.core.money.FxMath
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.MonthKey
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.TransactionRepository
import java.time.Instant
import javax.inject.Inject

/** What a screen knows when it creates a transaction. Everything derived is filled by [AddTransaction]. */
data class TransactionDraft(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val date: Instant,
    val currency: CurrencyCode = CurrencyCode.LKR,
    val fxRate: Double? = null, // required for anything but LKR: no rate must never save as 1 USD = 1 LKR
    val categoryId: String? = null,
    val accountId: String? = null,
    val toAccountId: String? = null,
    val incomeSourceId: String? = null,
    val relatedTxId: String? = null,
    val note: String = "",
)

/**
 * The one way a transaction is created (Doc 2 section 3.2). Fills monthKey (BR-20), the LKR value (BR-03)
 * and the timestamps, checks the same limits as the Firestore rules, then writes to the local cache.
 * The caller keeps one client id for the whole form, so a double tap writes one document (BR-19).
 */
class AddTransaction @Inject constructor(
    private val repository: TransactionRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(draft: TransactionDraft): AppResult<Transaction> {
        val tx = when (val built = build(draft)) {
            is AppResult.Failure -> return built
            is AppResult.Success -> built.value
        }
        return when (val written = repository.add(tx)) {
            is AppResult.Success -> AppResult.Success(tx)
            is AppResult.Failure -> written
        }
    }

    /** Pure: no write. A draft the server rules would reject fails here first, with the field to blame. */
    fun build(draft: TransactionDraft): AppResult<Transaction> {
        val fxRate = if (draft.currency == CurrencyCode.LKR) 1.0 else draft.fxRate
        val lkr = fxRate?.let { rate -> runCatching { FxMath.toLkrMinor(draft.amountMinor, rate) }.getOrNull() }
        val problem = when {
            draft.amountMinor !in 1..MAX_MINOR -> AppError.Validation("amount", "out of range")
            fxRate == null || fxRate <= 0.0 || lkr == null || lkr !in 1..MAX_MINOR -> AppError.Validation("fxRate", "out of range")
            draft.note.length > MAX_NOTE -> AppError.Validation("note", "too long")
            draft.type == TransactionType.TRANSFER &&
                (draft.accountId == null || draft.toAccountId == null || draft.accountId == draft.toAccountId) ->
                AppError.Validation("account", "a transfer needs two different accounts")
            else -> null
        }
        if (problem != null || lkr == null || fxRate == null) return AppResult.Failure(problem ?: AppError.Unknown())

        val now = time.now()
        return AppResult.Success(
            Transaction(
                id = draft.id,
                type = draft.type,
                amountMinor = draft.amountMinor,
                currency = draft.currency,
                fxRate = fxRate,
                amountLkrMinor = lkr,
                categoryId = draft.categoryId,
                accountId = draft.accountId,
                toAccountId = draft.toAccountId,
                incomeSourceId = draft.incomeSourceId,
                relatedTxId = draft.relatedTxId,
                recurringId = null,
                note = draft.note.trim(),
                date = draft.date,
                monthKey = MonthKey.of(draft.date, time.zone),
                archived = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    companion object {
        const val MAX_MINOR = 10_000_000_000L // LKR 100,000,000: same cap as MoneyParser and the rules
        const val MAX_NOTE = 200
    }
}
