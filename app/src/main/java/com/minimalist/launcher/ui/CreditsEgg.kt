package com.minimalist.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalist.launcher.EggSound
import com.minimalist.launcher.LauncherViewModel
import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private const val HOLD_MS = 3000          // how long the dial must be held
private const val GROW_MS = 1500          // the circle then opens out over this
private const val PAGE_MS = 10_000L
private const val TURNS = 6f              // revolutions during the hold; the speed ramps up from standing still
private const val SPARKS = 90
private const val SETTLE_MS = 0L          // the dial gesture already waited for the touch to settle

private val spinEase = FastOutSlowInEasing
private const val OMEGA = TURNS * 2f * PI.toFloat() * 2f / (HOLD_MS / 1000f)   // rad/s at full speed
/**
 * The big vibration when the ring hands over to the glow and the credits: one hard thump, a burst of full-strength
 * pulses while the circle opens out, then a fading tail as the page settles. About 1.7 s, matching the opening-out.
 */
private fun violentBuzz(context: Context) {
    val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
    if (!vibrator.hasVibrator()) return
    val times = ArrayList<Long>()
    val amps = ArrayList<Int>()
    fun add(ms: Long, amp: Int) { times += ms; amps += amp }
    add(0, 0)
    add(250, 255)                                 // the thump
    repeat(16) { add(25, 0); add(45, 255) }       // rapid full-strength pulses
    for (a in intArrayOf(210, 150, 100, 60)) { add(15, 0); add(60, a) }   // tail
    try {
        val effect = VibrationEffect.createWaveform(times.toLongArray(), amps.toIntArray(), -1)
        // Same usage as the tick haptics, so it follows the system touch-feedback setting and is not overridden by them.
        if (Build.VERSION.SDK_INT >= 33) vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        else vibrator.vibrate(effect)
    } catch (_: Exception) {
    }
}

private fun tickGapMs(speed: Float) = 340f - 290f * speed                     // quicker ticks as it speeds up

/**
 * Easter egg. Holding the clock dial for 3 seconds makes its ring start turning, slowly at first and faster and
 * faster, with haptic ticks that quicken in step and friction sparks once it is quick. Letting go early does not
 * stop it dead: the ring, the glow and the ticks all wind down to a pause, then it fades away.
 * At 3 seconds the circle opens out over the whole screen into a credits page that stays for 10 seconds.
 * Tap or Back closes the page early. With motion off there is no ring, only the haptics and the page.
 */
@Composable
fun CreditsEgg(vm: LauncherViewModel, tint: Color) {
    val grow = remember { Animatable(0f) }     // 0..1 over the opening-out
    var page by remember { mutableStateOf(false) }
    // The spinning ring is integrated frame by frame, so letting go can wind the same motion down.
    var active by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(0f) }        // 0..1 of full speed
    var angle by remember { mutableFloatStateOf(0f) }
    var clock by remember { mutableFloatStateOf(0f) }        // seconds, drives sparks and shock rings
    var cutoff by remember { mutableFloatStateOf(Float.MAX_VALUE) }   // no new sparks after this clock time
    var ringAlpha by remember { mutableFloatStateOf(1f) }
    val view = LocalView.current
    val context = LocalContext.current
    val sound = remember { EggSound() }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    val motion = vm.motionEnabled

    fun tick() {
        if (vm.prefs.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    LaunchedEffect(vm.eggHolding) {
        if (vm.eggActive) return@LaunchedEffect
        if (vm.eggHolding) {
            delay(SETTLE_MS)
            // Pressing again during a wind-down carries on from the current speed instead of jumping.
            if (speed <= 0f) { clock = 0f; angle = 0f }
            cutoff = Float.MAX_VALUE; ringAlpha = 1f; active = true
            sound.begin()
            var elapsed = speed * HOLD_MS / 1000f
            var last = withFrameNanos { it }
            var sinceTick = 0f
            while (elapsed < HOLD_MS / 1000f) {
                val now = withFrameNanos { it }
                val dt = (now - last) / 1e9f
                last = now
                elapsed += dt
                speed = (elapsed / (HOLD_MS / 1000f)).coerceAtMost(1f)
                sound.level = speed
                angle += speed * OMEGA * dt
                clock += dt
                sinceTick += dt * 1000f
                if (sinceTick >= tickGapMs(speed)) { tick(); sinceTick = 0f }
            }
            // Only the long pattern here: a separate tap at the same moment would cancel it.
            if (vm.prefs.haptics) violentBuzz(context)
            sound.boom()
            vm.fireEgg()
        } else if (active && speed > 0f) {
            // Released early: friction slows the ring to a stop. The wind-down is longer the faster it was going.
            val start = speed
            val seconds = 0.5f + 1.8f * start
            cutoff = clock
            var last = withFrameNanos { it }
            var sinceTick = 0f
            while (speed > 0f) {
                val now = withFrameNanos { it }
                val dt = (now - last) / 1e9f
                last = now
                speed = (speed - start / seconds * dt).coerceAtLeast(0f)
                sound.level = speed
                angle += speed * OMEGA * dt
                clock += dt
                sinceTick += dt * 1000f
                if (speed > 0.04f && sinceTick >= tickGapMs(speed)) { tick(); sinceTick = 0f }
            }
            sound.end()
            delay(350)                                   // a moment at rest
            val fadeStart = withFrameNanos { it }
            while (ringAlpha > 0f) {
                val now = withFrameNanos { it }
                ringAlpha = (1f - (now - fadeStart) / 1e9f / 0.3f).coerceAtLeast(0f)
            }
            active = false
        }
    }
    LaunchedEffect(vm.eggToken, vm.eggActive) {
        if (!vm.eggActive) {
            page = false; grow.snapTo(0f)
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.cancel()   // closed early: stop buzzing
            active = false; speed = 0f; ringAlpha = 1f; cutoff = Float.MAX_VALUE
            sound.end()
            return@LaunchedEffect
        }
        page = false
        grow.snapTo(0f)
        if (motion) grow.animateTo(1f, tween(GROW_MS, easing = LinearEasing))
        page = true
        sound.end()
        sound.finale()
        delay(PAGE_MS)
        vm.eggActive = false
    }

    val noRipple = remember { MutableInteractionSource() }
    // With motion on the opened-out circle already covers the screen, so the page background is there at once.
    val pageBg by animateFloatAsState(if (page) 1f else 0f, tween(if (motion) 0 else 350), label = "eggBg")
    val pageText by animateFloatAsState(if (page) 1f else 0f, tween(if (motion) 450 else 350), label = "eggText")
    val showRing = motion && active && (!page || pageBg < 1f)
    if (!showRing && !vm.eggActive) return

    val growSeconds = grow.value * GROW_MS / 1000f
    Box(
        if (vm.eggActive) Modifier.fillMaxSize().clickable(interactionSource = noRipple, indication = null) { vm.eggActive = false }
        else Modifier.fillMaxSize(),    // while only holding, touches must still reach the dial
    ) {
        // Keep the full-screen circle until the page background is fully opaque, so no frame shows Home in between.
        if (showRing) Canvas(Modifier.fillMaxSize()) {
            drawSpin(
                vm.eggOrigin, vm.eggRadius, clock + growSeconds, grow.value, speed, angle + OMEGA * growSeconds,
                ringAlpha, cutoff, vm.eggToken, tint,
            )
        }
        Box(Modifier.fillMaxSize().background(Pj.bg.copy(alpha = pageBg)))
        Column(
            Modifier.align(Alignment.Center).graphicsLayer { alpha = pageText },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PjText("DEVELOPED BY", size = PjType.caption, weight = FontWeight.SemiBold, spacing = 3.sp, color = Pj.muted, align = TextAlign.Center)
            PjText("Chetan Singh", size = 34.sp, weight = FontWeight.Light, spacing = 1.sp, color = tint, align = TextAlign.Center)
        }
    }
}

private fun DrawScope.drawSpin(
    origin: Offset, dialRadius: Float, seconds: Float, grow: Float, speed: Float, angle: Float,
    ringAlpha: Float, sparkCutoff: Float, token: Int, tint: Color,
) {
    val maxR = hypot(size.width, size.height)
    val e = spinEase.transform(grow)
    val r = dialRadius + (maxR - dialRadius) * e
    val fadeIn = (speed / 0.1f).coerceIn(0f, 1f)
    val fadeOut = if (grow > 0.75f) (1f - (grow - 0.75f) / 0.25f).coerceIn(0f, 1f) else 1f
    val a = fadeIn * fadeOut * ringAlpha

    // The circle that opens out; it is solid, so the Home screen is wiped away from the middle.
    if (grow > 0f) drawCircle(Pj.bg, r, origin)

    // Centre glow: calm at first, then flickering harder and brighter as the ring speeds up, with shock rings
    // pulsing out of it faster and faster. It stays at full violence while the circle opens out, then fades with it.
    val heat = (speed * speed).coerceIn(0f, 1f)
    if (heat > 0f) {
        val flick = 0.55f + 0.45f * Random(token * 7919 + (seconds * 50f).toInt()).nextFloat()
        val glowR = dialRadius * (0.45f + 0.75f * heat * flick) + (r - dialRadius)
        val shake = Offset((flick - 0.7f) * 8f * heat, (0.7f - flick) * 6f * heat).times(1.dp.toPx())
        val c = origin + shake
        val hot = lerp(tint, Color.White, 0.75f)
        drawCircle(
            Brush.radialGradient(
                listOf(hot.copy(alpha = (0.15f + 0.75f * heat * flick) * fadeOut), tint.copy(alpha = 0.45f * heat * flick * fadeOut), Color.Transparent),
                c, glowR,
            ),
            glowR, c, blendMode = BlendMode.Plus,
        )
        repeat(3) { k ->
            val phase = (seconds * (1.2f + 4.5f * heat) + k / 3f) % 1f
            drawCircle(
                tint.copy(alpha = (1f - phase) * 0.7f * heat * fadeOut), dialRadius * (0.2f + 0.95f * phase) + (r - dialRadius), c,
                style = Stroke((1.5f + 2.5f * heat).dp.toPx()),
            )
        }
    }

    // Spinning ring: rim, ticks and one bright marker so the rotation is visible.
    drawCircle(tint.copy(alpha = 0.85f * a), r, origin, style = Stroke(3.dp.toPx()))
    val ticks = 48
    for (i in 0 until ticks) {
        val ang = angle + i * 2f * PI.toFloat() / ticks
        val major = i % 6 == 0
        val inner = r - (if (major) 14.dp.toPx() else 7.dp.toPx())
        drawLine(
            tint.copy(alpha = (if (major) 0.9f else 0.5f) * a),
            origin + Offset(cos(ang), sin(ang)) * inner, origin + Offset(cos(ang), sin(ang)) * (r - 3.dp.toPx()),
            strokeWidth = (if (major) 2.5f else 1.5f).dp.toPx(), cap = StrokeCap.Round,
        )
    }
    val dot = origin + Offset(cos(angle), sin(angle)) * (r - 3.dp.toPx())
    drawCircle(lerp(tint, Color.White, 0.85f).copy(alpha = a), 5.dp.toPx(), dot)

    // Friction sparks, thrown off the rim at one fixed contact point along the way the ring is moving. They start
    // once the ring is turning quickly, and more are born the faster it goes.
    val contact = (PI / 2.0 + 0.5).toFloat()          // lower right of the dial
    val tangent = Offset(-sin(contact), cos(contact))
    val start = origin + Offset(cos(contact), sin(contact)) * dialRadius
    val spark = lerp(Color.White, tint, 0.2f)
    val rnd = Random(token * 131)
    repeat(SPARKS) {
        val born = 1.0f + 3.0f * kotlin.math.sqrt(rnd.nextFloat())
        val life = 0.28f + 0.22f * rnd.nextFloat()
        val spread = (rnd.nextFloat() - 0.5f) * 0.7f
        val speed = 380.dp.toPx() + 420.dp.toPx() * rnd.nextFloat()
        val lenJitter = rnd.nextFloat()
        val age = seconds - born
        if (age in 0f..life && born <= sparkCutoff && fadeOut > 0f) {
            val ca = cos(spread); val sa = sin(spread)
            val dir = Offset(tangent.x * ca - tangent.y * sa, tangent.x * sa + tangent.y * ca)
            val g = 1100.dp.toPx()
            val p = start + dir * (speed * age) + Offset(0f, 0.5f * g * age * age)
            val v = dir * speed + Offset(0f, g * age)
            val len = (6.dp.toPx() + 10.dp.toPx() * lenJitter) / hypot(v.x, v.y).coerceAtLeast(1f)
            val k = 1f - age / life
            drawLine(spark.copy(alpha = k), p, p - v * len, strokeWidth = 1.8.dp.toPx() * (0.5f + k * 0.5f), cap = StrokeCap.Round)
        }
    }
}
