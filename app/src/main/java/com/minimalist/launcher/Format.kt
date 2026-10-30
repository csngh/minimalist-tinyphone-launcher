package com.minimalist.launcher

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClockText(val time: String, val meridiem: String?, val date: String)

fun formatClock(epochMs: Long, zone: ZoneId, format: ClockFormat, systemIs24: Boolean, locale: Locale): ClockText {
    val t = Instant.ofEpochMilli(epochMs).atZone(zone)
    val use24 = when (format) {
        ClockFormat.SYSTEM -> systemIs24
        ClockFormat.H24 -> true
        ClockFormat.H12 -> false
    }
    val time = t.format(DateTimeFormatter.ofPattern(if (use24) "HH:mm" else "h:mm", locale))
    val meridiem = if (use24) null else t.format(DateTimeFormatter.ofPattern("a", locale)).uppercase(locale)
    val date = t.format(DateTimeFormatter.ofPattern("EEE d MMM", locale)).uppercase(locale)
    return ClockText(time, meridiem, date)
}

/** "now", "5 min", "3 h", then a short date: how old a notification is. */
fun relativeTime(nowMs: Long, thenMs: Long, locale: Locale = Locale.getDefault()): String {
    val mins = ((nowMs - thenMs) / 60_000L).coerceAtLeast(0)
    return when {
        mins < 1 -> "now"
        mins < 60 -> "$mins min"
        mins < 24 * 60 -> "${mins / 60} h"
        else -> java.text.SimpleDateFormat("d MMM", locale).format(java.util.Date(thenMs))
    }
}
