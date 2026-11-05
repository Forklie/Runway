package com.example.runway.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyParserTest {

    @Test
    fun `parses a number with a thousands comma and two decimals`() {
        assertEquals(125_050L, MoneyParser.parseToMinor("1,250.50"))
    }

    @Test
    fun `parses whole numbers and one decimal`() {
        assertEquals(120_000L, MoneyParser.parseToMinor("1200"))
        assertEquals(1_250L, MoneyParser.parseToMinor("12.5"))
    }

    @Test
    fun `ignores spaces around the input`() {
        assertEquals(1_200L, MoneyParser.parseToMinor("  12 "))
    }

    @Test
    fun `rejects text (AT-03)`() {
        assertNull(MoneyParser.parseToMinor("abc"))
        assertNull(MoneyParser.parseToMinor(""))
    }

    @Test
    fun `rejects zero and negative amounts (AT-03)`() {
        assertNull(MoneyParser.parseToMinor("0"))
        assertNull(MoneyParser.parseToMinor("0.00"))
        assertNull(MoneyParser.parseToMinor("-5"))
    }

    @Test
    fun `rejects more than two decimals (AT-04)`() {
        assertNull(MoneyParser.parseToMinor("12.345"))
    }

    @Test
    fun `accepts exactly the cap and rejects anything above it (BR-07)`() {
        assertEquals(10_000_000_000L, MoneyParser.parseToMinor("100000000"))
        assertNull(MoneyParser.parseToMinor("100000000.01"))
        assertNull(MoneyParser.parseToMinor("999999999999999999999"))
    }

    @Test
    fun `sanitizeInput keeps digits and one dot with two decimals`() {
        assertEquals("123.45", MoneyParser.sanitizeInput("1a2,3.456"))
        assertEquals("123456789", MoneyParser.sanitizeInput("1234567890"))
        assertEquals("", MoneyParser.sanitizeInput("abc"))
    }
}
