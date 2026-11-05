package com.example.runway.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** monthKey is "yyyy-MM", e.g. "2026-10". */
object MonthKey {
    fun of(date: LocalDate): String = YearMonth.from(date).toString()
    fun of(instant: Instant, zone: ZoneId): String = of(instant.atZone(zone).toLocalDate())

    fun parse(key: String): YearMonth? = runCatching { YearMonth.parse(key) }.getOrNull()
    fun previous(key: String): String? = parse(key)?.minusMonths(1)?.toString()
    fun next(key: String): String? = parse(key)?.plusMonths(1)?.toString()
    fun startOf(key: String, zone: ZoneId): Instant? =
        parse(key)?.atDay(1)?.atStartOfDay(zone)?.toInstant()
}