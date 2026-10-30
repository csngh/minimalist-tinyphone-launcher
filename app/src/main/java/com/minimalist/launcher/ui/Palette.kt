package com.minimalist.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** WCAG contrast ratio between two opaque colours. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/** Smallest mix of [tint] into [bg] whose result reaches [target]:1 against [bg]. */
fun edgeMix(tint: Color, bg: Color, target: Float): Float {
    var lo = 0f
    var hi = 1f
    repeat(14) {
        val mid = (lo + hi) / 2
        if (contrastRatio(lerp(bg, tint, mid), bg) >= target) hi = mid else lo = mid
    }
    return hi
}
