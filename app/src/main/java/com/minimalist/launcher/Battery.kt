package com.minimalist.launcher

/** The whole UI takes its colour from the battery level: red when critical, yellow when medium, otherwise the user's accent. */
enum class BatteryTone { NORMAL, MEDIUM, CRITICAL }

const val CRITICAL_AT_PERCENT = 15
const val MEDIUM_AT_PERCENT = 40

fun batteryTone(percent: Int): BatteryTone = when {
    percent <= CRITICAL_AT_PERCENT -> BatteryTone.CRITICAL
    percent <= MEDIUM_AT_PERCENT -> BatteryTone.MEDIUM
    else -> BatteryTone.NORMAL
}
