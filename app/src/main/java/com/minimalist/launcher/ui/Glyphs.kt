package com.minimalist.launcher.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.minimalist.launcher.Slot

/** One consistent stroke (1.7 dp, round caps and joins) on a 24-unit grid. Drawn in code, no bitmaps. */
enum class Glyph { CALL, MESSAGE, NAVIGATE, TOOLS, CAMERA, MUSIC, NOTES, SETTINGS, TORCH, APPS, BOLT, MORE, UP, BELL, CLOSE, CALC, STOPWATCH, MIC, USB, WIFI_DEBUG, DEV, CHEVRON }

fun Slot.glyph(): Glyph? = when (this) {
    Slot.CALL -> Glyph.CALL
    Slot.MESSAGE -> Glyph.MESSAGE
    Slot.NAVIGATE -> Glyph.NAVIGATE
    Slot.TOOLS -> Glyph.TOOLS
    Slot.CAMERA -> Glyph.CAMERA
    Slot.MUSIC -> Glyph.MUSIC
    Slot.NOTES -> Glyph.NOTES
    Slot.SETTINGS -> Glyph.SETTINGS
    else -> null
}

fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val s = size.minDimension / 24f
    val style = Stroke(1.7.dp.toPx() / s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    scale(s, s, Offset.Zero) {
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1, y1), Offset(x2, y2), style.width, StrokeCap.Round)
        fun ring(cx: Float, cy: Float, r: Float) = drawCircle(color, r, Offset(cx, cy), style = style)

        when (glyph) {
            Glyph.CALL -> drawPath(
                Path().apply {
                    moveTo(6f, 3f); lineTo(9f, 3f); lineTo(10.5f, 7.5f); lineTo(8.5f, 9f)
                    cubicTo(9.7f, 11.7f, 12.3f, 14.3f, 15f, 15.5f)
                    lineTo(16.5f, 13.5f); lineTo(21f, 15f); lineTo(21f, 18f)
                    cubicTo(21f, 19.7f, 19.7f, 21f, 18f, 21f)
                    cubicTo(10.8f, 21f, 3f, 13.2f, 3f, 6f)
                    cubicTo(3f, 4.3f, 4.3f, 3f, 6f, 3f)
                    close()
                },
                color, style = style,
            )
            Glyph.MESSAGE -> drawPath(
                Path().apply {
                    moveTo(3f, 4f); lineTo(21f, 4f); lineTo(21f, 16f); lineTo(11f, 16f)
                    lineTo(6f, 21f); lineTo(6f, 16f); lineTo(3f, 16f); close()
                },
                color, style = style,
            )
            Glyph.NAVIGATE -> drawPath(
                Path().apply { moveTo(12f, 3f); lineTo(19f, 20f); lineTo(12f, 16f); lineTo(5f, 20f); close() },
                color, style = style,
            )
            Glyph.TOOLS -> for (x in listOf(4f, 14f)) for (y in listOf(4f, 14f)) {
                drawRoundRect(color, Offset(x, y), Size(6f, 6f), CornerRadius(1.2f), style = style)
            }
            Glyph.CAMERA -> {
                drawRoundRect(color, Offset(3f, 7f), Size(18f, 13f), CornerRadius(2f), style = style)
                line(8f, 7f, 9.5f, 4f); line(9.5f, 4f, 14.5f, 4f); line(14.5f, 4f, 16f, 7f)
                ring(12f, 13.5f, 3.5f)
            }
            Glyph.MUSIC -> {
                line(9f, 17f, 9f, 5f); line(9f, 5f, 19f, 3f); line(19f, 3f, 19f, 15f)
                ring(6.5f, 17f, 2.5f); ring(16.5f, 15f, 2.5f)
            }
            Glyph.NOTES -> {
                drawRoundRect(color, Offset(5f, 3f), Size(14f, 18f), CornerRadius(1.5f), style = style)
                line(8.5f, 8f, 15.5f, 8f); line(8.5f, 12f, 15.5f, 12f); line(8.5f, 16f, 12.5f, 16f)
            }
            Glyph.SETTINGS -> {
                line(3f, 6f, 21f, 6f); line(3f, 12f, 21f, 12f); line(3f, 18f, 21f, 18f)
                // Knobs sit on the lines; the fill hides the line behind them.
                for ((cx, cy) in listOf(8f to 6f, 16f to 12f, 10f to 18f)) {
                    drawCircle(Pj.surface, 2.6f, Offset(cx, cy))
                    ring(cx, cy, 2.4f)
                }
            }
            Glyph.TORCH -> {
                drawPath(Path().apply { moveTo(8f, 3f); lineTo(16f, 3f); lineTo(15f, 9f); lineTo(9f, 9f); close() }, color, style = style)
                drawRoundRect(color, Offset(9f, 9f), Size(6f, 12f), CornerRadius(1.5f), style = style)
                line(12f, 12.5f, 12f, 15.5f)
            }
            Glyph.APPS -> for (x in listOf(5f, 12f, 19f)) for (y in listOf(5f, 12f, 19f)) {
                drawCircle(color, 1.5f, Offset(x, y))
            }
            Glyph.BOLT -> drawPath(
                Path().apply {
                    moveTo(13.5f, 2f); lineTo(5f, 13.5f); lineTo(11f, 13.5f); lineTo(10f, 22f)
                    lineTo(19f, 10f); lineTo(13f, 10f); close()
                },
                color, style = Fill,
            )
            Glyph.BELL -> {
                drawPath(
                    Path().apply {
                        moveTo(5f, 17f); lineTo(7f, 15f); lineTo(7f, 10f)
                        cubicTo(7f, 7.2f, 9.2f, 5f, 12f, 5f); cubicTo(14.8f, 5f, 17f, 7.2f, 17f, 10f)
                        lineTo(17f, 15f); lineTo(19f, 17f); close()
                    },
                    color, style = style,
                )
                line(12f, 3f, 12f, 5f); line(10f, 20f, 14f, 20f)
            }
            Glyph.USB -> {
                // Trident: stem with a ring at the foot, a square and a dot on branches.
                line(12f, 6f, 12f, 19f); ring(12f, 20f, 1.6f)
                line(12f, 14f, 7f, 10f); line(7f, 10f, 7f, 8f); ring(7f, 7f, 1.3f)
                line(12f, 11f, 17f, 8f); line(17f, 8f, 17f, 7f)
                drawRect(color, Offset(15.4f, 4.6f), Size(3.2f, 3.2f), style = style)
                line(12f, 6f, 10.3f, 8f); line(12f, 6f, 13.7f, 8f)
            }
            Glyph.WIFI_DEBUG -> {
                // A bug (debugging) with two signal arcs above it, so it does not read as plain Wi-Fi.
                for (r in listOf(4.5f, 8f)) {
                    drawArc(color, 235f, 70f, false, Offset(12f - r, 9f - r), Size(2 * r, 2 * r), style = style)
                }
                drawOval(color, Offset(8.8f, 11.5f), Size(6.4f, 9f), style = style)
                line(12f, 12f, 12f, 20f)
                line(8.8f, 14.5f, 5.5f, 13f); line(15.2f, 14.5f, 18.5f, 13f)
                line(8.8f, 18f, 5.5f, 19.5f); line(15.2f, 18f, 18.5f, 19.5f)
            }
            Glyph.CHEVRON -> { line(9f, 5f, 16f, 12f); line(16f, 12f, 9f, 19f) }
            Glyph.DEV -> {
                line(9f, 7f, 4f, 12f); line(4f, 12f, 9f, 17f)
                line(15f, 7f, 20f, 12f); line(20f, 12f, 15f, 17f)
                line(13.5f, 5f, 10.5f, 19f)
            }
            Glyph.MIC -> {
                drawRoundRect(color, Offset(9f, 3f), Size(6f, 11f), CornerRadius(3f), style = style)
                drawPath(Path().apply { moveTo(6f, 11f); cubicTo(6f, 15f, 9f, 17f, 12f, 17f); cubicTo(15f, 17f, 18f, 15f, 18f, 11f) }, color, style = style)
                line(12f, 17f, 12f, 21f); line(9f, 21f, 15f, 21f)
            }
            Glyph.CLOSE -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            Glyph.CALC -> {
                drawRoundRect(color, Offset(5f, 3f), Size(14f, 18f), CornerRadius(1.5f), style = style)
                line(8f, 8f, 16f, 8f)
                for (x in listOf(9f, 15f)) for (y in listOf(13f, 17f)) drawCircle(color, 1f, Offset(x, y))
            }
            Glyph.STOPWATCH -> {
                ring(12f, 13.5f, 7.5f); line(12f, 13.5f, 12f, 9.5f); line(10f, 3f, 14f, 3f); line(12f, 3f, 12f, 6f)
            }
            Glyph.UP -> {
                line(5f, 15f, 12f, 8f); line(12f, 8f, 19f, 15f)
            }
            Glyph.MORE -> for (y in listOf(5f, 12f, 19f)) drawCircle(color, 1.8f, Offset(12f, y))
        }
    }
}
