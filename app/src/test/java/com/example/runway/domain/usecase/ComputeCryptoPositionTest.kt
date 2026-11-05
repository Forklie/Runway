package com.example.runway.domain.usecase

import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.model.CryptoEntryType
import com.example.runway.domain.model.CryptoEntryType.DEPOSIT
import com.example.runway.domain.model.CryptoEntryType.HOLDING_SNAPSHOT
import com.example.runway.domain.model.CryptoEntryType.WITHDRAWAL
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.lkr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeCryptoPositionTest {

    private val compute = ComputeCryptoPosition()

    @Test
    fun `net position uses the latest snapshot only (BR-61)`() {
        val entries = listOf(
            entry(DEPOSIT, lkr(150_000), "2026-01-10"),
            entry(DEPOSIT, lkr(50_000), "2026-03-02"),
            entry(HOLDING_SNAPSHOT, lkr(260_000), "2026-03-31"), // Q1 gain, older snapshot
            entry(WITHDRAWAL, lkr(50_000), "2026-05-20"),
            entry(HOLDING_SNAPSHOT, lkr(180_000), "2026-10-01"),
        )
        val p = compute(entries)

        assertEquals(lkr(200_000), p.depositsMinor)
        assertEquals(lkr(50_000), p.withdrawalsMinor)
        assertEquals(lkr(180_000), p.holdingsMinor)
        assertEquals(lkr(30_000), p.netMinor) // 50,000 + 180,000 − 200,000
    }

    @Test
    fun `holdings worth less than what went in is a loss`() {
        val entries = listOf(
            entry(DEPOSIT, lkr(100_000), "2026-06-01"),
            entry(HOLDING_SNAPSHOT, lkr(62_000), "2026-09-30"), // Q3 loss
        )
        assertEquals(lkr(-38_000), compute(entries).netMinor)
    }

    @Test
    fun `no snapshot yet means holdings are unknown`() {
        val p = compute(listOf(entry(DEPOSIT, lkr(10_000), "2026-06-01")))
        assertNull(p.holdingsMinor)
        assertEquals(lkr(-10_000), p.netMinor)
    }

    @Test
    fun `an empty ledger is zero, not a crash`() {
        assertEquals(0L, compute(emptyList()).netMinor)
    }

    private fun entry(type: CryptoEntryType, amount: Long, date: String) = CryptoEntry(
        id = "$type-$date",
        type = type,
        asset = "USDT",
        amountLkrMinor = amount,
        date = at(date),
        note = "",
    )
}
