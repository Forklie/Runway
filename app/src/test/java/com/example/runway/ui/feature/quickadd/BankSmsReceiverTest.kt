package com.example.runway.ui.feature.quickadd

import com.example.runway.domain.model.TransactionType
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.tx
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsReceiverTest {

    private val day = at("2026-11-05")

    @Test
    fun `an expense of the same amount counts as already logged`() {
        assertTrue(listOf(tx(TransactionType.EXPENSE, 165_000, day)).hasExpenseOf(165_000))
    }

    @Test
    fun `another amount or type does not`() {
        val rows = listOf(tx(TransactionType.EXPENSE, 100, day), tx(TransactionType.INCOME, 165_000, day))
        assertFalse(rows.hasExpenseOf(165_000))
        assertFalse(emptyList<com.example.runway.domain.model.Transaction>().hasExpenseOf(165_000))
    }
}
