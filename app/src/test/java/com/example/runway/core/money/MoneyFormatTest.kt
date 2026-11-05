package com.example.runway.core.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {

    @Test
    fun `whole amounts drop the decimals (BR-08)`() {
        assertEquals("LKR 39,900", 3_990_000L.formatLkr())
    }

    @Test
    fun `detail screens show cents (BR-08)`() {
        assertEquals("LKR 1,250.50", 125_050L.formatLkr(cents = true))
        assertEquals("LKR 1,476.19", 147_619L.formatLkr(cents = true))
    }

    @Test
    fun `thousands use commas, never lakhs`() {
        assertEquals("LKR 12,000,000", 1_200_000_000L.formatLkr())
    }

    @Test
    fun `negative amounts use a true minus sign`() {
        assertEquals("\u22121,200", (-120_000L).formatAmount())
    }

    @Test
    fun `expense and income rows`() {
        assertEquals("\u2212\u20091,200", expenseText(120_000L))
        assertEquals("+\u200936,240", incomeText(3_624_000L))
    }
}
