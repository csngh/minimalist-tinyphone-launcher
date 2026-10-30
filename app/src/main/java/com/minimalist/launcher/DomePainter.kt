package com.minimalist.launcher

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.sin

/**
 * The wallpaper's dome, drawn once here so the live wallpaper and the unlock transition's overlay (which redraws it in
 * Compose) can be kept in step.
 */
object DomePainter {
    private fun mix(color: Int, alpha: Float) = ColorUtils.setAlphaComponent(color, (alpha * 255).toInt().coerceIn(0, 255))

    fun paint(c: Canvas, w: Float, h: Float, tint: Int) {
        c.drawColor(0xFF04070A.toInt())

        // The lock-screen clock and notifications live in the upper half, so the dial rises from the bottom edge
        // as a wide dome and leaves the top of the screen clean.
        val cx = w / 2f
        val cy = h * 1.18f
        val r = h * 0.40f

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 1.55f, intArrayOf(mix(tint, 0.30f), mix(tint, 0.09f), 0), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r * 1.55f, glow)

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        ring.strokeWidth = r * 0.012f
        ring.color = mix(tint, 0.6f)
        c.drawCircle(cx, cy, r, ring)

        // A fainter outer rim, and graduations on the inside of the dome like the real dial.
        ring.strokeWidth = r * 0.005f
        ring.color = mix(tint, 0.16f)
        c.drawCircle(cx, cy, r * 1.10f, ring)

        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        for (i in 0..80) {
            val deg = 180.0 + 180.0 * i / 80.0              // only the half that rises above the bottom edge
            val a = Math.toRadians(deg)
            val major = i % 10 == 0
            val r0 = r * 0.955f
            val r1 = r0 - r * (if (major) 0.07f else 0.035f)
            tick.strokeWidth = r * (if (major) 0.011f else 0.006f)
            tick.color = mix(tint, if (major) 0.45f else 0.22f)
            c.drawLine(cx + (r0 * cos(a)).toFloat(), cy + (r0 * sin(a)).toFloat(), cx + (r1 * cos(a)).toFloat(), cy + (r1 * sin(a)).toFloat(), tick)
        }
    }

}
