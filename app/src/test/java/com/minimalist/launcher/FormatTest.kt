package com.minimalist.launcher

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {
    private val utc = ZoneId.of("UTC")
    private val ms = ZonedDateTime.of(2026, 10, 27, 17, 5, 0, 0, utc).toInstant().toEpochMilli()

    @Test fun twentyFourHour() {
        val c = formatClock(ms, utc, ClockFormat.H24, false, Locale.US)
        assertEquals("17:05", c.time)
        assertNull(c.meridiem)
        assertEquals("TUE 27 OCT", c.date)
    }

    @Test fun twelveHour() {
        val c = formatClock(ms, utc, ClockFormat.H12, true, Locale.US)
        assertEquals("5:05", c.time)
        assertEquals("PM", c.meridiem)
    }

    @Test fun systemFollowsDeviceSetting() {
        assertEquals("17:05", formatClock(ms, utc, ClockFormat.SYSTEM, true, Locale.US).time)
        assertEquals("5:05", formatClock(ms, utc, ClockFormat.SYSTEM, false, Locale.US).time)
    }

    @Test fun honoursTimeZone() {
        val c = formatClock(ms, ZoneId.of("Asia/Kolkata"), ClockFormat.H24, true, Locale.US)
        assertEquals("22:35", c.time)
    }
}
