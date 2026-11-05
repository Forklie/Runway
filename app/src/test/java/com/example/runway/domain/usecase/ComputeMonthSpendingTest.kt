package com.example.runway.domain.usecase

import com.example.runway.domain.model.CategoryKind.COMMITTED
import com.example.runway.domain.model.CategoryKind.DISCRETIONARY
import com.example.runway.domain.model.TransactionType.EXPENSE
import com.example.runway.domain.model.TransactionType.INCOME
import com.example.runway.domain.model.TransactionType.REFUND
import com.example.runway.domain.model.TransactionType.TRANSFER
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.category
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeMonthSpendingTest {

    private val compute = ComputeMonthSpending()
    private val categories = listOf(
        category("rent", COMMITTED),
        category("gym", COMMITTED),
        category("food-coffee", DISCRETIONARY),
        category("transport", DISCRETIONARY),
    )

    @Test
    fun `splits committed from discretionary and ranks categories`() {
        val month = listOf(
            tx(EXPENSE, lkr(34_000), at("2026-10-01"), category = "rent"),
            tx(EXPENSE, lkr(5_000), at("2026-10-02"), category = "gym"),
            tx(EXPENSE, lkr(24_000), at("2026-10-12"), category = "food-coffee"),
            tx(EXPENSE, lkr(3_500), at("2026-10-14"), category = "transport"),
        )
        val result = compute(month, categories)

        assertEquals(lkr(39_000), result.committedMinor)
        assertEquals(lkr(27_500), result.discretionaryMinor)
        assertEquals(listOf("rent", "food-coffee", "gym", "transport"), result.byCategory.map { it.category?.id })
    }

    @Test
    fun `a refund reduces net spending in its category (AT-16, BR-15)`() {
        val food = tx(EXPENSE, lkr(6_000), at("2026-10-12"), category = "food-coffee", id = "dinner")
        val refund = tx(REFUND, lkr(2_000), at("2026-10-13"), category = null, related = "dinner")

        val result = compute(listOf(food, refund), categories)

        assertEquals(lkr(4_000), result.discretionaryMinor)
        assertEquals(lkr(4_000), result.byCategory.single().netMinor)
        assertEquals(lkr(6_000), result.loggedExpensesMinor) // Clarity counts what was logged
    }

    @Test
    fun `transfers and income are never spending (BR-13)`() {
        val month = listOf(
            tx(TRANSFER, lkr(10_000), at("2026-10-05"), to = "cash"),
            tx(INCOME, lkr(120_000), at("2026-10-25")),
        )
        val result = compute(month, categories)
        assertEquals(0L, result.totalMinor)
        assertEquals(emptyList<CategorySpend>(), result.byCategory)
    }

    @Test
    fun `uncategorised spending counts as discretionary`() {
        val result = compute(listOf(tx(EXPENSE, lkr(800), at("2026-10-05"))), categories)
        assertEquals(lkr(800), result.discretionaryMinor)
        assertNull(result.byCategory.single().category)
    }

    @Test
    fun `archived expenses are excluded (BR-16, AT-06)`() {
        val undone = tx(EXPENSE, lkr(1_200), at("2026-10-05"), category = "food-coffee", archived = true)
        assertEquals(0L, compute(listOf(undone), categories).totalMinor)
    }
}
