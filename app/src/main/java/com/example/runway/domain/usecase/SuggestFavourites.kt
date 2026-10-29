package com.example.runway.domain.usecase

import com.example.runway.domain.model.Category
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Quick Add's first chips (BR-27): the 6 categories used most in the last 60 days, ties to the most
 * recent. A new user has no history yet, so the list fills up in sortOrder and is always full.
 */
class SuggestFavourites @Inject constructor() {
    operator fun invoke(categories: List<Category>, transactions: List<Transaction>, now: Instant): List<Category> {
        val cutoff = windowStart(now)
        val uses = transactions.filter { it.type == TransactionType.EXPENSE && it.date >= cutoff }.groupBy { it.categoryId }
        return categories.sortedWith(
            compareByDescending<Category> { uses[it.id]?.size ?: 0 }
                .thenByDescending { uses[it.id]?.maxOf { tx -> tx.date } ?: Instant.MIN }
                .thenBy { it.sortOrder },
        ).take(FAVOURITE_COUNT)
    }

    companion object {
        const val FAVOURITE_COUNT = 6
        fun windowStart(now: Instant): Instant = now.minus(60, ChronoUnit.DAYS)
    }
}
