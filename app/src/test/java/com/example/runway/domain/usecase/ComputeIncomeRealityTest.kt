package com.example.runway.domain.usecase

import com.example.runway.domain.model.IncomeSourceKind.ADSENSE
import com.example.runway.domain.model.IncomeSourceKind.FREELANCE
import com.example.runway.domain.model.IncomeSourceKind.SALARY
import com.example.runway.domain.model.Reliability.RELIABLE
import com.example.runway.domain.model.Reliability.VARIABLE
import com.example.runway.domain.model.TransactionType.INCOME
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.colombo
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.source
import com.example.runway.testutil.Fixtures.tx
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeIncomeRealityTest {

    private val compute = ComputeIncomeReality()
    private val today = LocalDate.of(2026, 11, 3)
    private val sources = listOf(
        source("salary", SALARY, RELIABLE, expected = lkr(120_000), ownEstimate = lkr(140_000)),
        source("freelance", FREELANCE, VARIABLE, ownEstimate = lkr(50_000)),
        source("adsense", ADSENSE, VARIABLE, ownEstimate = lkr(20_000)),
    )

    private val threeMonths = listOf(
        tx(INCOME, lkr(120_000), at("2026-08-25"), source = "salary"),
        tx(INCOME, lkr(130_000), at("2026-09-25"), source = "salary"),
        tx(INCOME, lkr(140_000), at("2026-10-25"), source = "salary"),
        tx(INCOME, lkr(90_000), at("2026-09-12"), source = "freelance"),
        tx(INCOME, lkr(3_500), at("2026-08-08"), source = "adsense"),
        tx(INCOME, lkr(19_000), at("2026-09-08"), source = "adsense"),
        tx(INCOME, lkr(12_000), at("2026-10-08"), source = "adsense"),
        tx(INCOME, lkr(125_000), at("2026-11-01"), source = "salary"), // current month: excluded
        tx(INCOME, lkr(999_000), at("2026-07-25"), source = "salary"), // before the window
    )

    @Test
    fun `average, min and max per source over the last 3 complete months (BR-44, FR-15)`() {
        val r = compute(sources, threeMonths, today, colombo)
        val bySource = r.sources.associateBy { it.source?.id }

        assertEquals(3, r.monthsCounted)
        assertEquals(SourceReality(sources[0], lkr(130_000), lkr(120_000), lkr(140_000)), bySource["salary"])
        assertEquals(SourceReality(sources[1], lkr(30_000), 0L, lkr(90_000)), bySource["freelance"])
        assertEquals(lkr(11_500), bySource["adsense"]?.averageMinor)
        assertEquals(lkr(171_500), r.totalAverageMinor)
    }

    @Test
    fun `his own estimate is compared with what actually arrived (BR-44)`() {
        val r = compute(sources, threeMonths, today, colombo)
        assertEquals(lkr(210_000), r.ownEstimateMinor)
        assertEquals(lkr(38_500), r.estimateGapMinor)
    }

    @Test
    fun `salary is listed first, then the largest average`() {
        val r = compute(sources, threeMonths, today, colombo)
        assertEquals(listOf("salary", "freelance", "adsense"), r.sources.map { it.source?.id })
    }

    @Test
    fun `with under 3 months of income it says how many months it used (AT-18)`() {
        val oneMonth = listOf(tx(INCOME, lkr(130_000), at("2026-10-25"), source = "salary"))
        val r = compute(sources, oneMonth, today, colombo)

        assertEquals(1, r.monthsCounted)
        assertEquals(lkr(130_000), r.totalAverageMinor)
    }

    @Test
    fun `no complete month of income yet means nothing to average`() {
        val r = compute(sources, emptyList(), today, colombo)
        assertEquals(0, r.monthsCounted)
        assertEquals(emptyList<SourceReality>(), r.sources)
    }

    @Test
    fun `income logged without a source gets its own row`() {
        val gifts = listOf(tx(INCOME, lkr(6_000), at("2026-10-02"), source = null))
        val other = compute(sources, gifts, today, colombo).sources.single { it.source == null }
        assertEquals(lkr(6_000), other.averageMinor)
    }

    @Test
    fun `the query window starts on the first day of the oldest complete month`() {
        assertEquals(LocalDate.of(2026, 8, 1).atStartOfDay(colombo).toInstant(), compute.windowStart(today, colombo))
    }
}
