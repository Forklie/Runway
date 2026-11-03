package com.example.runway.domain.usecase

import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.RecurringKind
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeSubscriptionCostTest {

    private val compute = ComputeSubscriptionCost()

    private fun item(
        amount: Long,
        currency: CurrencyCode = CurrencyCode.LKR,
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    ) = RecurringItem(
        id = "s1", kind = RecurringKind.SUBSCRIPTION, name = "Streaming", amountMinor = amount,
        currency = currency, frequency = frequency, nextDueDate = Instant.EPOCH,
        categoryId = "subscriptions", accountId = "bank-main", active = true,
    )

    @Test
    fun `a monthly subscription costs its amount a month and 12 times that a year (BR-92)`() {
        // LKR 1,500 a month
        val cost = compute(item(150_000), emptyMap())!!
        assertEquals(150_000L, cost.monthlyLkrMinor)
        assertEquals(1_800_000L, cost.yearlyLkrMinor) // LKR 18,000
    }

    @Test
    fun `a yearly subscription costs a twelfth a month`() {
        // LKR 12,000 a year
        val cost = compute(item(1_200_000, frequency = RecurringFrequency.YEARLY), emptyMap())!!
        assertEquals(100_000L, cost.monthlyLkrMinor) // LKR 1,000
        assertEquals(1_200_000L, cost.yearlyLkrMinor)
    }

    @Test
    fun `a yearly amount that does not divide by 12 rounds to the nearest unit`() {
        // 1,000,005 / 12 = 83,333.75 -> 83,334
        assertEquals(83_334L, compute(item(1_000_005, frequency = RecurringFrequency.YEARLY), emptyMap())!!.monthlyLkrMinor)
    }

    @Test
    fun `a USD subscription is converted with the last known rate`() {
        // USD 10 at 305.00 = LKR 3,050 a month
        val cost = compute(item(1_000, currency = CurrencyCode.USD), mapOf("USD" to 305.0))!!
        assertEquals(305_000L, cost.monthlyLkrMinor)
        assertEquals(3_660_000L, cost.yearlyLkrMinor)
    }

    @Test
    fun `a USD subscription with no rate is not guessed`() {
        assertNull(compute(item(1_000, currency = CurrencyCode.USD), emptyMap()))
        assertNull(compute(item(1_000, currency = CurrencyCode.USD), mapOf("USD" to 0.0)))
    }

    @Test
    fun `an LKR subscription never needs a rate`() {
        assertEquals(150_000L, compute(item(150_000), mapOf("USD" to 305.0))!!.monthlyLkrMinor)
    }
}
