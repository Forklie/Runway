package com.example.runway.domain.usecase

import com.example.runway.domain.model.Category
import com.example.runway.domain.model.CategoryKind
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import javax.inject.Inject

/** Net spending in one category. [category] is null for expenses logged without one. */
data class CategorySpend(val category: Category?, val netMinor: Long)

data class MonthSpending(
    val committedMinor: Long,      // net (expenses − refunds) in COMMITTED categories
    val discretionaryMinor: Long,  // net in DISCRETIONARY categories, plus uncategorised
    val loggedExpensesMinor: Long, // gross expenses, the "logged" side of Clarity (BR-82)
    val byCategory: List<CategorySpend>, // net > 0, largest first: the top-categories bar list
) {
    val totalMinor: Long get() = committedMinor + discretionaryMinor
}

/**
 * The committed vs discretionary split and the category ranking for one month (Doc 1 section 4.5).
 * Net spending = expenses − refunds (BR-15). Transfers are never spending (BR-13).
 */
class ComputeMonthSpending @Inject constructor() {

    operator fun invoke(monthTransactions: List<Transaction>, categories: List<Category>): MonthSpending {
        val live = monthTransactions.filterNot { it.archived }
        val byId = categories.associateBy { it.id }
        val expenseCategory = live.filter { it.type == TransactionType.EXPENSE }.associate { it.id to it.categoryId }

        val netByCategory = mutableMapOf<String?, Long>()
        fun add(categoryId: String?, amount: Long) {
            netByCategory[categoryId] = (netByCategory[categoryId] ?: 0L) + amount
        }
        live.forEach { tx ->
            when (tx.type) {
                TransactionType.EXPENSE -> add(tx.categoryId, tx.amountLkrMinor)
                // A refund reduces the category of the expense it belongs to (BR-14).
                TransactionType.REFUND -> add(tx.categoryId ?: expenseCategory[tx.relatedTxId], -tx.amountLkrMinor)
                TransactionType.INCOME, TransactionType.TRANSFER -> Unit
            }
        }

        val spends = netByCategory.map { (id, net) -> CategorySpend(id?.let(byId::get), net) }
        val committed = spends.filter { it.category?.kind == CategoryKind.COMMITTED }.sumOf { it.netMinor }
        val discretionary = spends.filterNot { it.category?.kind == CategoryKind.COMMITTED }.sumOf { it.netMinor }

        return MonthSpending(
            committedMinor = committed.coerceAtLeast(0L),
            discretionaryMinor = discretionary.coerceAtLeast(0L),
            loggedExpensesMinor = live.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountLkrMinor },
            byCategory = spends.filter { it.netMinor > 0 }.sortedByDescending { it.netMinor },
        )
    }
}
