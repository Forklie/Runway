package com.example.runway.data.remote.mapper

import com.google.firebase.Timestamp
import java.time.Instant

/** Firestore stores dates as Timestamp. Built from seconds and nanos so it works on every SDK version. */
fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)

/** A missing date never crashes a list: it falls back to [default]. */
fun Timestamp?.toInstantOr(default: Instant = Instant.EPOCH): Instant =
    this?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) } ?: default

/** Enums are stored as their name. An unknown or missing value maps to a safe default. */
inline fun <reified E : Enum<E>> String?.toEnumOr(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
