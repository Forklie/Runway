package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountMovement
import com.example.runway.domain.model.MovementChannel
import com.example.runway.domain.model.MovementDirection
import com.example.runway.domain.model.TransactionType
import com.example.runway.testutil.Fixtures.tx
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountActivityTest {

    private val colombo = ZoneId.of("Asia/Colombo")

    private fun move(
        account: String,
        direction: MovementDirection,
        minor: Long,
        at: String,
        channel: MovementChannel = MovementChannel.CARD,
    ) = AccountMovement(account, direction, channel, minor, Instant.parse(at))

    @Test
    fun `totals, net and channels per account, busiest first`() {
        val result = summarizeAccountActivity(
            listOf(
                move("1111", MovementDirection.IN, 1_000, "2026-11-01T04:00:00Z", MovementChannel.OTHER),
                move("2222", MovementDirection.OUT, 300, "2026-11-01T04:00:00Z"),
                move("2222", MovementDirection.OUT, 200, "2026-11-02T04:00:00Z", MovementChannel.ATM),
                move("2222", MovementDirection.IN, 100, "2026-11-02T05:00:00Z", MovementChannel.OTHER),
            ),
            colombo,
        )
        assertEquals(listOf("2222", "1111"), result.map { it.accountSuffix })
        val busy = result.first()
        assertEquals(100L, busy.inMinor)
        assertEquals(500L, busy.outMinor)
        assertEquals(-400L, busy.netMinor)
        assertEquals(3, busy.count)
        assertEquals(2, busy.activeDays)
        assertEquals(mapOf(MovementChannel.CARD to 1, MovementChannel.ATM to 1, MovementChannel.OTHER to 1), busy.countByChannel)
    }

    @Test
    fun `active days use the local date, not UTC`() {
        // 19:00 UTC on the 1st is 00:30 on the 2nd in Colombo, the same day as the second message
        val result = summarizeAccountActivity(
            listOf(
                move("1111", MovementDirection.OUT, 100, "2026-11-01T19:00:00Z"),
                move("1111", MovementDirection.OUT, 100, "2026-11-02T04:00:00Z"),
            ),
            colombo,
        )
        assertEquals(1, result.single().activeDays)
    }

    @Test
    fun `no movements means no accounts`() {
        assertTrue(summarizeAccountActivity(emptyList(), colombo).isEmpty())
    }

    @Test
    fun `a debit with an expense of that amount logged that day is not offered`() {
        val coffee = move("1111", MovementDirection.OUT, 50_000, "2026-11-02T04:00:00Z")
        val lunch = move("1111", MovementDirection.OUT, 150_000, "2026-11-02T06:00:00Z")
        val salary = move("1111", MovementDirection.IN, 50_000, "2026-11-02T07:00:00Z")
        val sameAmountOtherDay = move("1111", MovementDirection.OUT, 50_000, "2026-11-03T04:00:00Z")
        val logged = listOf(tx(TransactionType.EXPENSE, 50_000, Instant.parse("2026-11-02T10:00:00Z")))
        assertEquals(
            listOf(sameAmountOtherDay, lunch), // newest first, credits never offered
            unloggedDebits(listOf(coffee, lunch, salary, sameAmountOtherDay), logged, colombo),
        )
    }
}
