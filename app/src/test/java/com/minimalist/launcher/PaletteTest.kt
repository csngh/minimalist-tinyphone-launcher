package com.minimalist.launcher

import androidx.compose.ui.graphics.Color
import com.minimalist.launcher.ui.Pj
import com.minimalist.launcher.ui.contrastRatio
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    private val tones = mapOf(
        "cyan" to Color(0xFF22D3EE),
        "ice" to Color(0xFFA5ECFF),
        "teal" to Color(0xFF2DD4BF),
        "azure" to Color(0xFF38A8F8),
        "amber" to Color(0xFFFFB454),
        "yellow" to Pj.medium,
        "red" to Pj.critical,
    )

    @After fun reset() { Pj.tint = Color(0xFF22D3EE) }

    @Test fun componentEdgesAndInactiveDotsReachThreeToOneInEveryTone() {
        for ((name, t) in tones) {
            Pj.tint = t
            val ratio = contrastRatio(Pj.edge, Pj.bg)
            assertTrue("$name edge $ratio", ratio >= 3.0f)
        }
    }

    @Test fun textStaysLegibleInEveryTone() {
        for ((name, t) in tones) {
            Pj.tint = t
            assertTrue("$name text", contrastRatio(Pj.text, Pj.bg) >= 7f)
            assertTrue("$name muted", contrastRatio(Pj.muted, Pj.bg) >= 4.5f)
            assertTrue("$name muted on tile", contrastRatio(Pj.muted, Pj.surface) >= 4.5f)
        }
    }
}
