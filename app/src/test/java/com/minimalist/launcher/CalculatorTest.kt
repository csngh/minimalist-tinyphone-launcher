package com.minimalist.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorTest {
    private fun Calculator.keys(s: String) = s.forEach {
        when (it) {
            '.' -> dot()
            '=' -> equals()
            '+', '−', '×', '÷' -> operator(it)
            else -> digit(it)
        }
    }

    private fun calc(s: String) = Calculator().apply { keys(s) }.display

    @Test fun addsAndChainsLeftToRight() {
        assertEquals("5", calc("2+3="))
        assertEquals("20", calc("2+3×4="))
    }

    @Test fun decimalsAreExact() {
        assertEquals("0.3", calc("0.1+0.2="))
        assertEquals("0.5", calc("1÷2="))
    }

    @Test fun divisionByZeroIsAnErrorThatClearsOnNextDigit() {
        val c = Calculator().apply { keys("5÷0=") }
        assertTrue(c.isError)
        c.digit('7')
        assertEquals("7", c.display)
    }

    @Test fun editingKeys() {
        val c = Calculator().apply { keys("123") }
        c.backspace(); assertEquals("12", c.display)
        c.negate(); assertEquals("-12", c.display)
        c.percent(); assertEquals("-0.12", c.display)
        c.clear(); assertEquals("0", c.display)
    }

    @Test fun inputIsLimitedAndDotIsSingle() {
        assertEquals("123456789012", calc("1234567890123456"))
        assertEquals("1.5", calc("1.5."))
        assertEquals("0.5", calc(".5"))
    }

    @Test fun resultsReuseAsFirstOperand() {
        assertEquals("12", calc("3×4="))
        assertEquals("15", calc("3×4=+3="))
        assertEquals("1000000", calc("1000×1000="))
    }

    @Test fun stopwatchFormat() {
        assertEquals("00:00.00", formatStopwatch(0))
        assertEquals("01:02.34", formatStopwatch(62_340))
        assertEquals("1:00:00.00", formatStopwatch(3_600_000))
    }

    @Test fun voiceMemoDuration() {
        assertEquals("0:07", formatDuration(7_400))
        assertEquals("12:34", formatDuration(754_000))
    }

    @Test fun relativeTimeBuckets() {
        val now = 10_000_000_000L
        assertEquals("now", relativeTime(now, now - 20_000, java.util.Locale.US))
        assertEquals("5 min", relativeTime(now, now - 5 * 60_000, java.util.Locale.US))
        assertEquals("3 h", relativeTime(now, now - 3 * 3_600_000L, java.util.Locale.US))
    }
}
