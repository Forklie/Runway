package com.example.runway.domain.usecase

import com.example.runway.core.money.FxMath
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import javax.inject.Inject

/** What one subscription costs, in LKR minor units (BR-04: sums use LKR only). */
data class SubscriptionCost(
    val monthlyLkrMinor: Long,
    val yearlyLkrMinor: Long,
)

/**
 * BR-92: monthly amount, and x 12 per year. A YEARLY item is the other way round: its amount is the
 * yearly cost and the monthly cost is a twelfth, rounded to the nearest unit. Pure Kotlin, never throws.
 */
class ComputeSubscriptionCost @Inject constructor() {

    /**
     * @param fxRates LKR per 1 unit of a currency, as kept in the profile (for example {"USD": 305.0}).
     * @return null when the item is in a foreign currency with no known rate: we do not guess one.
     */
    operator fun invoke(item: RecurringItem, fxRates: Map<String, Double>): SubscriptionCost? {
        val rate = if (item.currency == CurrencyCode.LKR) 1.0 else fxRates[item.currency.name]
        if (rate == null || rate <= 0.0) return null

        val amountLkr = FxMath.toLkrMinor(item.amountMinor, rate)
        return when (item.frequency) {
            RecurringFrequency.MONTHLY -> SubscriptionCost(monthlyLkrMinor = amountLkr, yearlyLkrMinor = amountLkr * 12)
            RecurringFrequency.YEARLY -> SubscriptionCost(monthlyLkrMinor = (amountLkr + 6) / 12, yearlyLkrMinor = amountLkr)
        }
    }
}
