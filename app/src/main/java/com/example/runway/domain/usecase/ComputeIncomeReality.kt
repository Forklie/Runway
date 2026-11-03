package com.example.runway.domain.usecase

import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.Reliability
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** One source over the counted months. [source] is null for income logged without a source. */
data class SourceReality(
    val source: IncomeSource?,
    val averageMinor: Long,
    val minMinor: Long,
    val maxMinor: Long,
)

data class IncomeReality(
    val monthsCounted: Int,             // 0 to 3: "based on N months" when under 3 (BR-44)
    val sources: List<SourceReality>,   // reliable first, then the largest average
    val totalAverageMinor: Long,
    val ownEstimateMinor: Long?,        // his own monthly guess, null when he never gave one
) {
    /** Positive when his estimate is above what actually arrived. */
    val estimateGapMinor: Long? get() = ownEstimateMinor?.let { it - totalAverageMinor }
}

/**
 * "What he actually earns" (BR-44): average, min and max per source over the last 3 COMPLETE
 * calendar months (the current month is excluded, BR-97). Only months with income logged count,
 * so a new user sees "based on 1 month" instead of averages dragged down by empty months.
 */
class ComputeIncomeReality @Inject constructor() {

    operator fun invoke(
        sources: List<IncomeSource>,
        transactions: List<Transaction>,
        today: LocalDate,
        zone: ZoneId,
    ): IncomeReality {
        val window = completeMonths(today)
        val income = transactions.filter { it.type == TransactionType.INCOME && !it.archived }
            .groupBy { YearMonth.from(it.date.atZone(zone)) }
            .filterKeys { it in window }
        val counted = window.filter { income[it].orEmpty().isNotEmpty() }
        val ownEstimate = sources.mapNotNull { it.ownMonthlyEstimateMinor }.takeIf { it.isNotEmpty() }?.sum()
        if (counted.isEmpty()) return IncomeReality(0, emptyList(), 0L, ownEstimate)

        val known: Set<String?> = sources.map { it.id }.toSet()
        fun totalsFor(sourceId: String?): List<Long> = counted.map { month ->
            income[month].orEmpty()
                .filter { if (sourceId == null) it.incomeSourceId !in known else it.incomeSourceId == sourceId }
                .sumOf { it.amountLkrMinor }
        }

        val perSource = sources.map { reality(it, totalsFor(it.id)) } +
            listOfNotNull(totalsFor(null).takeIf { totals -> totals.any { it > 0 } }?.let { reality(null, it) })

        return IncomeReality(
            monthsCounted = counted.size,
            sources = perSource.sortedWith(
                compareBy<SourceReality> { it.source?.reliability != Reliability.RELIABLE }
                    .thenByDescending { it.averageMinor },
            ),
            totalAverageMinor = counted.sumOf { month -> income[month].orEmpty().sumOf { it.amountLkrMinor } } /
                counted.size,
            ownEstimateMinor = ownEstimate,
        )
    }

    /** Where the income query starts: the first day of the oldest month in the window. */
    fun windowStart(today: LocalDate, zone: ZoneId): Instant =
        completeMonths(today).first().atDay(1).atStartOfDay(zone).toInstant()

    private fun completeMonths(today: LocalDate): List<YearMonth> {
        val current = YearMonth.from(today)
        return (MONTHS downTo 1).map { current.minusMonths(it.toLong()) }
    }

    private fun reality(source: IncomeSource?, monthlyTotals: List<Long>) = SourceReality(
        source = source,
        averageMinor = monthlyTotals.sum() / monthlyTotals.size,
        minMinor = monthlyTotals.min(),
        maxMinor = monthlyTotals.max(),
    )

    private companion object {
        const val MONTHS = 3
    }
}
