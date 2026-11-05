package com.example.runway.domain.usecase

import com.example.runway.domain.model.CategoryKind.DISCRETIONARY
import com.example.runway.domain.model.TransactionType.EXPENSE
import com.example.runway.domain.model.TransactionType.REFUND
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.category
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.tx
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestFavouritesTest {
    private val now = at("2026-11-03")
    private val categories = (1..8).map { category("c$it", DISCRETIONARY).copy(sortOrder = it) }

    private fun expense(category: String, date: String) = tx(EXPENSE, lkr(100), at(date), category = category, id = "$category-$date")

    @Test
    fun `no history falls back to the first six by sortOrder`() {
        assertEquals(listOf("c1", "c2", "c3", "c4", "c5", "c6"), suggestFavourites(categories, emptyList(), now).map { it.id })
    }

    @Test
    fun `most used comes first, then the most recent on a tie (BR-27)`() {
        val history = listOf(
            expense("c8", "2026-11-01"), expense("c8", "2026-11-02"),
            expense("c5", "2026-10-20"),
            expense("c7", "2026-10-25"),
        )
        assertEquals("c8,c7,c5,c1,c2,c3", suggestFavourites(categories, history, now).joinToString(",") { it.id })
    }

    @Test
    fun `only expenses inside the 60 day window count`() {
        val history = listOf(
            expense("c8", "2026-09-03"), // 61 days old
            tx(REFUND, lkr(100), at("2026-11-01"), category = "c7"),
            expense("c6", "2026-09-05"), // 59 days old
        )
        assertEquals("c6,c1,c2,c3,c4,c5", suggestFavourites(categories, history, now).joinToString(",") { it.id })
    }
}
