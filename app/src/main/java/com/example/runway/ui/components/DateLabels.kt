package com.example.runway.ui.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Design system dates: "Jun 2027", "30 Nov", "Tuesday 3 Nov", "Tue 3 Nov". English, to match the strings.
private val monthYear = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
private val monthYearSpoken = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dayMonthSpoken = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEEE d MMM", Locale.ENGLISH)
private val shortWeekdayDayMonth = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val time24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val time12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

fun LocalDate.monthYearLabel(): String = format(monthYear)
fun LocalDate.monthYearSpoken(): String = format(monthYearSpoken)
fun LocalDate.dayMonthLabel(): String = format(dayMonth)
fun LocalDate.dayMonthSpoken(): String = format(dayMonthSpoken)
fun LocalDate.weekdayLabel(): String = format(weekdayDayMonth)
fun LocalDate.shortDayLabel(): String = format(shortWeekdayDayMonth)

/** "8:42 pm" or "20:42", following the phone's 12 or 24-hour setting. */
@Composable
fun Instant.timeLabel(zone: ZoneId): String {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return atZone(zone).format(if (is24Hour) time24 else time12).lowercase(Locale.ENGLISH)
}
