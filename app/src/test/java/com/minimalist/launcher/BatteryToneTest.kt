package com.minimalist.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryToneTest {
    @Test fun criticalAtOrBelowFifteen() {
        assertEquals(BatteryTone.CRITICAL, batteryTone(0))
        assertEquals(BatteryTone.CRITICAL, batteryTone(15))
    }

    @Test fun mediumBetweenSixteenAndForty() {
        assertEquals(BatteryTone.MEDIUM, batteryTone(16))
        assertEquals(BatteryTone.MEDIUM, batteryTone(40))
    }

    @Test fun normalAboveForty() {
        assertEquals(BatteryTone.NORMAL, batteryTone(41))
        assertEquals(BatteryTone.NORMAL, batteryTone(100))
    }
}
