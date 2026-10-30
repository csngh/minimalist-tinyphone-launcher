package com.minimalist.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.minimalist.launcher.LauncherViewModel
import com.minimalist.launcher.openClaudeVoice
import com.minimalist.launcher.openDeveloperOptions
import com.minimalist.launcher.developerOptionsOn
import com.minimalist.launcher.usbDebuggingOn
import com.minimalist.launcher.wirelessDebuggingOn
import com.minimalist.launcher.Screen
import com.minimalist.launcher.NotificationStore
import com.minimalist.launcher.Slot
import com.minimalist.launcher.formatClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class BatteryInfo(val percent: Int, val charging: Boolean)

private fun parseBattery(i: Intent): BatteryInfo {
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val pct = if (level < 0 || scale <= 0) 0 else (level * 100 / scale).coerceIn(0, 100)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo(pct, charging)
}

/** Listens for battery changes only while the activity is started. */
@Composable
fun rememberBattery(): State<BatteryInfo> {
    val ctx = LocalContext.current
    // Seeded from the sticky broadcast (no receiver registered) so the first frame already has the real level.
    val state = remember {
        mutableStateOf(
            ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let(::parseBattery)
                ?: BatteryInfo(100, false)
        )
    }
    LifecycleStartEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { state.value = parseBattery(i) }
        }
        ContextCompat.registerReceiver(
            ctx, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )?.let { state.value = parseBattery(it) }
        onStopOrDispose { ctx.unregisterReceiver(receiver) }
    }
    return state
}

/** Wall-clock millis, refreshed on the minute while resumed. No timer runs off-screen. */
@Composable
private fun rememberNow(): State<Long> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val n = System.currentTimeMillis()
                state.longValue = n
                delay(60_000 - n % 60_000 + 20)
            }
        }
    }
    return state
}

/** Frame clock for opt-in animations: ticks every [intervalMs] while resumed and [active]; idle otherwise. */
@Composable
private fun rememberTicker(active: Boolean, intervalMs: Long): State<Long> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycle, active, intervalMs) {
        if (!active) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                state.longValue = System.currentTimeMillis()
                delay(intervalMs)
            }
        }
    }
    return state
}

private const val BEAMS = 4 // beams around a full turn of the gradient; about two are visible on the arc

/**
 * Repeating sweep gradient: each cycle fades in slowly to a bright head and drops off quickly, so a beam reads as a
 * soft streak with a long tail. Transparent stops share the beam's RGB so there is no dark fringe.
 */
private fun beamBrush(color: Color, center: Offset): Brush {
    val clear = color.copy(alpha = 0f)
    val stops = buildList {
        add(0f to clear)
        for (k in 0 until BEAMS) {
            val a = k / BEAMS.toFloat()
            val w = 1f / BEAMS
            add(a + 0.30f * w to clear)
            add(a + 0.93f * w to color)
            add(a + 1.00f * w - 0.0001f to clear)
        }
        add(1f to clear)
    }
    return Brush.sweepGradient(*stops.toTypedArray(), center = center)
}

private const val TICKS = 20 // 21 marks across the 270 degree dial: one per 5%

@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    battery: BatteryInfo,
    onSlot: (Slot) -> Unit,
    onRequestHome: () -> Unit,
) {
    val prefs = vm.prefs
    val accent = LocalAccent.current
    val motion = vm.motionEnabled
    val now by rememberNow()
    val tick by rememberTicker(prefs.showcase, if (motion) 66L else 1000L)
    // Charging animation: only while actually charging below 100%, with motion on. ~30 fps, only while visible.
    val chargeAnim = battery.charging && battery.percent < 100 && motion
    val chargeTick by rememberTicker(chargeAnim, 33L)
    // Fully charged and still plugged in: a heavy glow and ripples instead. Same rules: motion on, visible only.
    val fullAnim = battery.charging && battery.percent >= 100 && motion
    val fullTick by rememberTicker(fullAnim, 33L)
    val ctx = LocalContext.current
    val clock = formatClock(now, ZoneId.systemDefault(), prefs.clockFormat, DateFormat.is24HourFormat(ctx), Locale.getDefault())

    // The boot-in plays on first launch and after a long gap, only with motion on.
    val intro = remember(vm.introToken) { motion && vm.introToken > vm.introSeen }

    // Cap font scaling on this one screen so the clock ring and tiles keep their shape.
    // Lists and settings use the full system font scale.
    val base = LocalDensity.current
    val capped = Density(base.density, min(base.fontScale, 1.5f))

    CompositionLocalProvider(LocalDensity provides capped) {
        val arcColor = accent // already battery-derived: red when critical, yellow when medium

        Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BoxWithConstraints(Modifier.weight(1.15f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val d = minOf(maxWidth, maxHeight)
                val fs = capped.fontScale
                val sweep = remember(vm.introToken) { Animatable(if (intro) 0f else 1f) }
                LaunchedEffect(vm.introToken) {
                    if (intro) sweep.animateTo(1f, tween(520))
                }
                val cPx = with(LocalDensity.current) { (d / 2).toPx() }
                val beamBrush = remember(arcColor, cPx) { beamBrush(arcColor, Offset(cPx, cPx)) }
                val coreBrush = remember(arcColor, cPx) {
                    beamBrush(androidx.compose.ui.graphics.lerp(arcColor, Color.White, 0.8f), Offset(cPx, cPx))
                }
                val conf = LocalConfiguration.current
                val rippleReach = (maxOf(conf.screenWidthDp, conf.screenHeightDp) * 0.7f).dp
                val dayFraction = ((now + java.util.TimeZone.getDefault().getOffset(now)) % 86_400_000L) / 86_400_000f

                val dialBounds = remember { mutableStateOf(Rect.Zero) }
                Box(
                    Modifier
                        .size(d)
                        .onGloballyPositioned { dialBounds.value = it.boundsInRoot() }
                        // Easter egg: hold the clock dial. Watches the touch without consuming it. A touch that moves
                        // before it has settled is a swipe and never starts the egg. Once it has started, Home's own
                        // swipes are switched off (see eggHolding) so the dial cannot be dragged away from the animation.
                        // Movement is measured in window coordinates, because the dial itself moves while the page is swiped.
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                fun root(p: androidx.compose.ui.geometry.Offset) = p + dialBounds.value.topLeft
                                val start = root(down.position)
                                val slop = viewConfiguration.touchSlop
                                val abandoned = withTimeoutOrNull(EGG_SETTLE_MS) {
                                    while (true) {
                                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                                        val c = ev.changes.firstOrNull { it.id == down.id }
                                        if (c == null || !c.pressed || (root(c.position) - start).getDistance() > slop) return@withTimeoutOrNull true
                                    }
                                    @Suppress("UNREACHABLE_CODE") false
                                }
                                if (abandoned == true) return@awaitEachGesture
                                val b = dialBounds.value
                                vm.setEggDial(b.center, b.width / 2f)
                                vm.eggHolding = true
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                                    val c = ev.changes.firstOrNull { it.id == down.id }
                                    if (c == null || !c.pressed || (root(c.position) - start).getDistance() > slop * 2) break
                                }
                                vm.eggHolding = false
                            }
                        }
                        .clearAndSetSemantics { contentDescription = "Battery ${battery.percent} percent${if (battery.charging) ", charging" else ""}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 4.dp.toPx()
                        val inset = stroke / 2 + 2.dp.toPx()
                        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                        val c = Offset(size.width / 2, size.height / 2)
                        val radius = arcSize.width / 2
                        val fill = battery.percent / 100f * sweep.value

                        // Charging state. All phases come from wall-clock time, so nothing accumulates.
                        val charging = chargeAnim && sweep.value >= 1f && fill > 0f
                        val t = if (charging) chargeTick else 0L
                        val breathe = if (charging) ((sin(2.0 * PI * (t % 2400L) / 2400.0) + 1.0) / 2.0).toFloat() else 0f
                        val pulse = if (charging) ((sin(2.0 * PI * (t % 1200L) / 1200.0) + 1.0) / 2.0).toFloat() else 0f
                        val bright = androidx.compose.ui.graphics.lerp(arcColor, Color.White, 0.7f)

                        // Fully charged: the whole dial glows on and off, and ripples spread from the centre.
                        if (fullAnim && sweep.value >= 1f) {
                            val ft = fullTick
                            val wave = ((sin(2.0 * PI * (ft % 2600L) / 2600.0 - PI / 2) + 1.0) / 2.0).toFloat()
                            val glowOn = wave * wave * (3f - 2f * wave) // eased, so it is properly dark between flashes
                            val coreR = size.width * 0.5f
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(arcColor.copy(alpha = 0.62f * glowOn), arcColor.copy(alpha = 0.24f * glowOn), Color.Transparent),
                                    c, coreR,
                                ),
                                coreR, c, blendMode = BlendMode.Plus,
                            )
                            // Ripples are born on the outer ring and travel outward across the whole screen, fading
                            // as they go. The arc closes into a full circle as it grows.
                            for (k in 0 until 3) {
                                val rp = ((ft + k * 1300L) % 3900L) / 3900f
                                val fade = (1f - rp) * (1f - rp * 0.6f)
                                val e = rippleReach.toPx() * rp
                                val rs = Size(arcSize.width + 2 * e, arcSize.height + 2 * e)
                                val rt = Offset(inset - e, inset - e)
                                val start = 135f - 45f * rp
                                val sweepDeg = 270f + 90f * rp
                                drawArc(arcColor, start, sweepDeg, false, rt, rs, alpha = 0.22f * fade, style = Stroke(8.dp.toPx() * (1f - 0.5f * rp), cap = StrokeCap.Round), blendMode = BlendMode.Plus)
                                drawArc(arcColor, start, sweepDeg, false, rt, rs, alpha = 0.8f * fade, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round), blendMode = BlendMode.Plus)
                            }
                            // The charged ring flares with the glow.
                            val tl = Offset(inset, inset)
                            drawArc(arcColor, 135f, 270f, false, tl, arcSize, alpha = 0.45f * glowOn, style = Stroke(stroke * 3.4f, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
                        }

                        // Inner light: the dial reads as lit from within, and breathes while charging.
                        drawCircle(
                            Brush.radialGradient(listOf(accent.copy(alpha = 0.10f + 0.10f * breathe), Color.Transparent), c, size.width / 2),
                            size.width / 2, c,
                        )

                        drawArc(Pj.track, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(arcColor, 135f, 270f * fill, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))

                        if (charging) {
                            // Energy beams: soft-edged bands of light flow along the charged arc. They are one sweep
                            // gradient (bright head, long fading tail) rotated around the dial, drawn in layers of
                            // decreasing width with additive blending for a neon bloom. The arc geometry is counter-
                            // rotated so only the light moves.
                            val shift = (t % 1600L) / 1600f * (360f / BEAMS)
                            rotate(shift, pivot = c) {
                                val start = 135f - shift
                                val sw = 270f * fill
                                val tl = Offset(inset, inset)
                                drawArc(beamBrush, start, sw, false, tl, arcSize, alpha = 0.14f, style = Stroke(stroke * 2.8f, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
                                drawArc(beamBrush, start, sw, false, tl, arcSize, alpha = 0.30f, style = Stroke(stroke * 1.8f, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
                                drawArc(beamBrush, start, sw, false, tl, arcSize, alpha = 0.65f, style = Stroke(stroke * 1.15f, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
                                drawArc(coreBrush, start, sw, false, tl, arcSize, alpha = 1f, style = Stroke(stroke * 0.5f, cap = StrokeCap.Round))
                            }
                        }

                        // Graduations just inside the arc: one per 5%, longer every 25%. Lit up to the charge level.
                        // While charging, a wave of light runs across the unlit ones toward 100%.
                        val wave = fill + (1f - fill) * ((t % 2200L) / 2200f)
                        for (i in 0..TICKS) {
                            val f = i / TICKS.toFloat()
                            val a = (135.0 + 270.0 * f) * PI / 180.0
                            val major = i % 5 == 0
                            val r0 = radius - stroke / 2 - 4.dp.toPx()
                            val r1 = r0 - (if (major) 8.dp else 4.dp).toPx()
                            val lit = f <= fill + 0.001f && fill > 0f
                            val glow = if (charging && !lit) {
                                val x = (f - wave) / 0.09f
                                kotlin.math.exp(-(x * x)).toFloat()
                            } else 0f
                            drawLine(
                                if (lit) arcColor.copy(alpha = 0.8f) else androidx.compose.ui.graphics.lerp(Pj.line, bright, glow),
                                Offset(c.x + (r0 * cos(a)).toFloat(), c.y + (r0 * sin(a)).toFloat()),
                                Offset(c.x + (r1 * cos(a)).toFloat(), c.y + (r1 * sin(a)).toFloat()),
                                if (major || glow > 0.4f) 1.5.dp.toPx() else 1.dp.toPx(),
                            )
                        }

                        // Bright leading edge, like a needle tip. While charging it radiates two sonar rings.
                        if (fill > 0f) {
                            val a = (135.0 + 270.0 * fill) * PI / 180.0
                            val p = Offset(c.x + (radius * cos(a)).toFloat(), c.y + (radius * sin(a)).toFloat())
                            if (charging) {
                                for (k in 0..1) {
                                    val rp = ((t + k * 800L) % 1600L) / 1600f
                                    drawCircle(
                                        arcColor.copy(alpha = 0.6f * (1f - rp)), (5f + 26f * rp).dp.toPx(), p,
                                        style = Stroke(1.5.dp.toPx()),
                                    )
                                }
                            }
                            drawCircle(arcColor.copy(alpha = 0.2f + 0.2f * pulse), (8f + 3f * pulse).dp.toPx(), p)
                            drawCircle(Color.White.copy(alpha = 0.9f), 2.dp.toPx(), p)
                        }

                        if (prefs.showcase) {
                            // Real data only: day-progress ring and a seconds marker.
                            val inner = inset + 24.dp.toPx()
                            val innerSize = Size(size.width - inner * 2, size.height - inner * 2)
                            drawArc(Pj.line, 0f, 360f, false, Offset(inner, inner), innerSize, style = Stroke(1.dp.toPx()))
                            drawArc(accent.copy(alpha = 0.6f), -90f, 360f * dayFraction, false, Offset(inner, inner), innerSize, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                            val t = tick // read in draw scope: redraws without recomposing
                            val secs = (t % 60_000L) / 1000f
                            val ang = (secs * 6f - 90f) * (PI / 180.0)
                            val r = innerSize.width / 2
                            drawCircle(accent, 2.5.dp.toPx(), Offset(c.x + (r * cos(ang)).toFloat(), c.y + (r * sin(ang)).toFloat()))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            PjText(
                                clock.time,
                                size = (d.value * 0.25f / fs).sp,
                                weight = FontWeight.Light,
                                maxLines = 1,
                                tabular = true,
                            )
                            if (clock.meridiem != null) {
                                PjText(
                                    clock.meridiem, size = PjType.caption, color = Pj.muted, spacing = 1.sp,
                                    modifier = Modifier.padding(start = 3.dp, bottom = 6.dp),
                                )
                            }
                        }
                        PjText(
                            clock.date, size = PjType.label, color = Pj.muted, spacing = 2.sp,
                            align = TextAlign.Center, maxLines = 1,
                        )
                    }
                    // Above the time, so the clock itself never moves when an icon appears or goes away.
                    DevChips(vm, Modifier.align(Alignment.Center).offset(y = -(d * 0.27f)))
                    Row(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PjText(
                            "${battery.percent}%",
                            size = PjType.label, weight = FontWeight.SemiBold, color = arcColor, spacing = 1.sp, tabular = true,
                        )
                        if (battery.charging) Canvas(Modifier.size(width = 12.dp, height = 16.dp)) { drawGlyph(Glyph.BOLT, arcColor) }
                    }
                }

                // Notification count, only once the user has switched notification access on. Swiping down opens the
                // same screen from anywhere on Home.
                if (vm.notificationAccess && NotificationStore.connected) {
                    NotificationChip(NotificationStore.count, Modifier.align(Alignment.BottomEnd)) { vm.openNotifications() }
                }
            }

            // One decision for the whole grid, so the four tiles always agree on glyphs.
            BoxWithConstraints(Modifier.weight(1f)) {
                val tileHeight = (maxHeight - 8.dp) / 2
                val glyphs = tileHeight >= 80.dp
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FitGroup {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tile(vm, Slot.CALL, 0, intro, glyphs, onSlot)
                            Tile(vm, Slot.MESSAGE, 1, intro, glyphs, onSlot)
                        }
                    }
                    FitGroup {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tile(vm, Slot.NAVIGATE, 2, intro, glyphs, onSlot)
                            // Hold for 1.2 s to start: a stray tap or short press should not open a voice chat.
                            // The fill charges while held, and a plain tap says what to do instead.
                            var holding by remember { mutableStateOf(false) }
                            var hint by remember { mutableStateOf(false) }
                            val charge = remember { Animatable(0f) }
                            LaunchedEffect(holding) {
                                if (holding) charge.animateTo(1f, tween(1200, easing = LinearEasing))
                                else charge.animateTo(0f, tween(150))
                            }
                            LaunchedEffect(hint) { if (hint) { delay(1600); hint = false } }
                            ActionTile(
                                vm, if (hint) "Hold to talk" else "Claude", Glyph.MIC, 3, intro, glyphs,
                                Modifier.pjHold(
                                    1200,
                                    onPress = { holding = true },
                                    onRelease = { held -> holding = false; if (!held) hint = true },
                                ) { openClaudeVoice(ctx) },
                                onLongClick = null,
                                fill = { charge.value },
                            ) {}
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PjButton(
                    "TOOLS", Modifier.weight(1f), glyph = Glyph.TOOLS,
                    onLongClick = { vm.pick(Slot.TOOLS, Screen.Home) },
                ) { onSlot(Slot.TOOLS) }
                PjButton("QUICK", Modifier.weight(1f), glyph = Glyph.UP) { vm.notificationsOpen = false; vm.drawerOpen = true }
            }
        }

        // Notices float over the screen instead of taking space from it, so the dial and tiles never resize
        // when one appears or goes away.
        AnimatedVisibility(
            visible = !vm.isDefaultHome || !prefs.hintSeen,
            modifier = Modifier.align(Alignment.TopCenter).padding(start = 16.dp, end = 16.dp, top = 8.dp),
            enter = if (motion) fadeIn() + slideInVertically { -it / 2 } else EnterTransition.None,
            exit = if (motion) fadeOut() else ExitTransition.None,
        ) {
            if (!vm.isDefaultHome) SetupBanner(onRequestHome) else HintBanner(onDismiss = prefs::markHintSeen)
        }
        }
    }
    // Declared last so the effects above still see the intro as pending.
    LaunchedEffect(vm.introToken) { vm.introSeen = vm.introToken }
}

/**
 * USB debugging, Wireless debugging and Developer options, each shown only while it is on. Each opens Developer options
 * scrolled to its own switch (Android has no separate screen for them).
 */
@Composable
private fun DevChips(vm: LauncherViewModel, modifier: Modifier) {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    val usb = remember(vm.resumeTick) { usbDebuggingOn(ctx) }
    val wifi = remember(vm.resumeTick) { wirelessDebuggingOn(ctx) }
    val dev = remember(vm.resumeTick) { developerOptionsOn(ctx) }
    Row(modifier) {
        for ((on, glyph, label, key) in listOf(
            DevChip(usb, Glyph.USB, "USB debugging", "enable_adb"),
            DevChip(wifi, Glyph.WIFI_DEBUG, "Wireless debugging", "toggle_adb_wireless"),
            DevChip(dev, Glyph.DEV, "Developer options", null),
        )) {
            if (!on) continue
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(48.dp)
                    .pjClick(interaction) { openDeveloperOptions(ctx, key) }
                    .semantics { contentDescription = "$label, on. Open its setting" },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(24.dp)) { drawGlyph(glyph, accent) }
            }
        }
    }
}

private data class DevChip(val on: Boolean, val glyph: Glyph, val label: String, val settingKey: String?)

@Composable
private fun NotificationChip(count: Int, modifier: Modifier, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val tint = if (count > 0) accent else Pj.muted
    Row(
        modifier
            .heightIn(min = 56.dp)
            .pjClick(interaction, onClick = onClick)
            .semantics { contentDescription = if (count == 0) "Notifications, none" else "Notifications, $count" }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.size(24.dp)) { drawGlyph(Glyph.BELL, tint) }
        if (count > 0) PjText(count.toString(), size = PjType.label, weight = FontWeight.SemiBold, color = tint, tabular = true)
    }
}

@Composable
private fun SetupBanner(onRequestHome: () -> Unit) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(Pj.bg.copy(alpha = 0.96f), PjShape)
            .border(1.dp, accent, PjShape)
            .pjClick(interaction, onClick = onRequestHome)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        PjText("SET AS HOME APP", size = PjType.caption, weight = FontWeight.SemiBold, spacing = 1.5.sp, color = accent)
    }
}

/** Shown once, after the launcher becomes the Home app: the two gestures nothing else teaches. */
@Composable
private fun HintBanner(onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Pj.bg.copy(alpha = 0.96f), PjShape).border(1.dp, Pj.edge, PjShape).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PjText(
            "Swipe left: favorites. Hold a tile: change its app. Hold Claude: talk.",
            size = PjType.caption, modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        PjButton("GOT IT", Modifier.padding(vertical = 4.dp), onClick = onDismiss)
    }
}

@Composable
private fun RowScope.Tile(
    vm: LauncherViewModel, slot: Slot, index: Int, intro: Boolean,
    showGlyph: Boolean, onSlot: (Slot) -> Unit,
) = ActionTile(
    vm, slot.label, slot.glyph(), index, intro, showGlyph, Modifier,
    onLongClick = { vm.pick(slot, Screen.Home) },
) { onSlot(slot) }

/** A Home grid tile with its own action; [extra] can add gestures (the Claude tile uses a long hold). */
@Composable
private fun RowScope.ActionTile(
    vm: LauncherViewModel, label: String, glyph: Glyph?, index: Int, intro: Boolean,
    showGlyph: Boolean, extra: Modifier, onLongClick: (() -> Unit)?, fill: () -> Float = { 0f }, onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val appear = remember(vm.introToken) { Animatable(if (intro) 0f else 1f) }
    LaunchedEffect(vm.introToken) {
        if (intro) {
            delay(120L + index * 55L)
            appear.animateTo(1f, tween(250))
        }
    }
    Box(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .heightIn(min = 56.dp)
            .graphicsLayer { translationY = (1f - appear.value) * 12.dp.toPx() }
            .alpha(appear.value)
            .machinedTile(pressed, accent)
            .drawBehind {
                val f = fill()
                if (f > 0f) drawRect(accent.copy(alpha = 0.30f), Offset(1f, size.height * (1f - f)), Size(size.width - 2f, size.height * f - 1f))
            }
            .then(extra)
            .pjClick(interaction, onLongClick = onLongClick, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showGlyph) glyph?.let { g -> Canvas(Modifier.size(32.dp)) { drawGlyph(g, accent) } }
            PjText(
                label.uppercase(), size = PjType.label, weight = FontWeight.SemiBold, spacing = 1.2.sp,
                align = TextAlign.Center, fit = true, modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

/** How long a touch on the dial must stay put before it counts as the start of the easter-egg hold. */
private const val EGG_SETTLE_MS = 250L
