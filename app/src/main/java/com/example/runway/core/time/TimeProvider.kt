package com.example.runway.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Inject this instead of calling LocalDate.now(). Tests pass Clock.fixed(...). */
@Singleton
class TimeProvider @Inject constructor(private val clock: Clock) {
    val zone: ZoneId get() = clock.zone
    fun now(): Instant = clock.instant()
    fun today(): LocalDate = LocalDate.now(clock)
    fun currentMonthKey(): String = MonthKey.of(today())
}