package com.minimalist.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.util.LruCache

/**
 * Turns any app icon into a white-on-transparent mask, so it can be tinted with the current theme colour.
 *
 * - Adaptive icon with a monochrome layer (Android 13+): that layer's alpha is the mask.
 * - Adaptive icon without one: the foreground shape, with details shaded by brightness (never below 55% visible).
 * - Artwork with an opaque square background (legacy icons, some adaptive foregrounds): contrast against the icon's
 *   average brightness, so the square does not turn into a solid block (small differences count as background).
 */
object IconMasks {
    private val cache = LruCache<String, Bitmap>(160)
    private const val FAINT = 70

    fun load(context: Context, component: ComponentName, sizePx: Int): Bitmap? {
        val key = "${component.flattenToString()}@$sizePx"
        cache.get(key)?.let { return it }
        val drawable = try {
            context.packageManager.getActivityIcon(component).mutate()
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }

        val src = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(src)
        var useLuma = true
        if (drawable is AdaptiveIconDrawable) {
            val mono = if (Build.VERSION.SDK_INT >= 33) drawable.monochrome else null
            val layer = mono ?: drawable.foreground
            // Adaptive layers are 108 dp with the visible 72 dp centred, so overscan by a quarter each side.
            val pad = (sizePx * 0.25f).toInt()
            layer.setBounds(-pad, -pad, sizePx + pad, sizePx + pad)
            layer.draw(canvas)
            useLuma = mono == null
        } else {
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
        }

        val px = IntArray(sizePx * sizePx)
        src.getPixels(px, 0, sizePx, 0, 0, sizePx, sizePx)
        // Opaque just inside two opposite corners means the artwork carries its own square background (legacy icons, or an
        // adaptive foreground that paints one). A plain glyph leaves those spots transparent; the monochrome layer never fills them.
        fun opaqueAt(fx: Float, fy: Float) = (px[(fy * (sizePx - 1)).toInt() * sizePx + (fx * (sizePx - 1)).toInt()] ushr 24) > 200
        val fullBleed = useLuma && opaqueAt(0.15f, 0.15f) && opaqueAt(0.85f, 0.85f)
        var mean = 0f
        if (fullBleed) {
            var n = 0
            for (p in px) if ((p ushr 24) > 128) { mean += luma(p); n++ }
            if (n > 0) mean /= n
        }
        for (i in px.indices) {
            val a = px[i] ushr 24
            val out = when {
                a == 0 -> 0
                fullBleed -> (a * ((kotlin.math.abs(luma(px[i]) - mean) - 0.12f) * 3.2f).coerceIn(0f, 1f)).toInt()
                useLuma -> (a * (0.55f + 0.45f * luma(px[i]))).toInt()
                else -> a
            }
            // Faint alpha is a baked-in backdrop, not artwork: drop it, and stretch the rest back to full strength.
            val shaped = if (out < FAINT) 0 else ((out - FAINT) * 255 / (255 - FAINT))
            px[i] = (shaped shl 24) or 0x00FFFFFF
        }
        src.setPixels(px, 0, sizePx, 0, 0, sizePx, sizePx)
        cache.put(key, src)
        return src
    }

    private fun luma(p: Int): Float {
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    }
}
