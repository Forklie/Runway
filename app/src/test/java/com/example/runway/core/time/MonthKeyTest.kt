package com.example.runway.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthKeyTest {

    private val colombo = ZoneId.of("Asia/Colombo") // UTC+05:30

    @Test
    fun `31 Oct 19h00 UTC is already November in Colombo (BR-20)`() {
        val instant = Instant.parse("2026-10-31T19:00:00Z")
        assertEquals("2026-11", MonthKey.of(instant, colombo))
        assertEquals("2026-10", MonthKey.of(instant, ZoneId.of("UTC")))
    }

    @Test
    fun `the last second of October in Colombo is still October`() {
        assertEquals("2026-10", MonthKey.of(Instant.parse("2026-10-31T18:29:59Z"), colombo))
        assertEquals("2026-11", MonthKey.of(Instant.parse("2026-10-31T18:30:00Z"), colombo))
    }

    @Test
    fun `key from a local date has the yyyy-MM shape`() {
        assertEquals("2026-01", MonthKey.of(LocalDate.of(2026, 1, 15)))
    }

    @Test
    fun `previous and next cross the year boundary`() {
        assertEquals("2025-12", MonthKey.previous("2026-01"))
        assertEquals("2027-01", MonthKey.next("2026-12"))
    }

    @Test
    fun `bad keys return null instead of throwing`() {
        assertNull(MonthKey.parse("not-a-month"))
        assertNull(MonthKey.previous("2026-13"))
    }

    @Test
    fun `start of a month is midnight in the given zone`() {
        assertEquals(Instant.parse("2026-10-31T18:30:00Z"), MonthKey.startOf("2026-11", colombo))
    }
}
