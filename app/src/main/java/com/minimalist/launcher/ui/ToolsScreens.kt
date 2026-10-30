package com.minimalist.launcher.ui

import android.os.SystemClock
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.geometry.CornerRadius
import com.minimalist.launcher.VoiceMemos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import com.minimalist.launcher.formatDuration
import com.minimalist.launcher.openAppInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.minimalist.launcher.Calculator
import com.minimalist.launcher.LauncherViewModel
import com.minimalist.launcher.Screen
import com.minimalist.launcher.ToolKind
import com.minimalist.launcher.formatStopwatch
import com.minimalist.launcher.relativeTime
import kotlinx.coroutines.delay

@Composable
private fun ToolFrame(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ScreenHeader(title, onBack, trailing)
        content()
    }
}

/** The list behind the Tools tile. Everything here is part of the launcher: offline, no permissions. */
@Composable
fun ToolsScreen(vm: LauncherViewModel, onBack: () -> Unit) {
    ToolFrame("Tools", onBack) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FitGroup {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrawerTile("CALCULATOR", Glyph.CALC, Modifier.weight(1f)) { vm.openTool(ToolKind.CALCULATOR, Screen.Tools) }
                    DrawerTile("STOPWATCH", Glyph.STOPWATCH, Modifier.weight(1f)) { vm.openTool(ToolKind.STOPWATCH, Screen.Tools) }
                }
            }
            FitGroup {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrawerTile("NOTES", Glyph.NOTES, Modifier.weight(1f)) { vm.openTool(ToolKind.NOTES, Screen.Tools) }
                    Box(Modifier.weight(1f))
                }
            }
            PjText(
                "Built into the launcher. They work offline and keep their data on this phone only. " +
                    "Hold the Tools tile on Home to use another app instead.",
                size = PjType.caption, color = Pj.muted, modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun ToolScreen(vm: LauncherViewModel, kind: ToolKind, onBack: () -> Unit) {
    when (kind) {
        ToolKind.CALCULATOR -> CalculatorScreen(onBack)
        ToolKind.STOPWATCH -> StopwatchScreen(vm, onBack)
        ToolKind.NOTES -> NotesScreen(vm, onBack)
    }
}

// ---- Calculator

private val KEYS = listOf(
    listOf("AC", "±", "%", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "−"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "DEL", "="),
)

@Composable
private fun CalculatorScreen(onBack: () -> Unit) {
    val calc = remember { Calculator() }
    // Calculator is plain state, so keep a version counter to redraw after each key.
    var version by remember { mutableLongStateOf(0L) }
    fun press(k: String) {
        when (k) {
            "AC" -> calc.clear()
            "±" -> calc.negate()
            "%" -> calc.percent()
            "DEL" -> calc.backspace()
            "=" -> calc.equals()
            "÷", "×", "−", "+" -> calc.operator(k[0])
            "." -> calc.dot()
            else -> calc.digit(k[0])
        }
        version++
    }
    @Suppress("UNUSED_VARIABLE") val redraw = version // reading it is what redraws the keypad
    ToolFrame("Calculator", onBack) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.fillMaxWidth().weight(1.3f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
                PjText(calc.pending, size = PjType.label, color = Pj.muted, maxLines = 1, align = TextAlign.End, modifier = Modifier.fillMaxWidth())
                PjText(
                    calc.display, size = 56.sp, weight = FontWeight.Light, tabular = true, fit = true,
                    color = if (calc.isError) Pj.warn else Pj.text, align = TextAlign.End, modifier = Modifier.fillMaxWidth(),
                )
            }
            FitGroup {
                KEYS.forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            val op = k.length == 1 && k[0] in "÷×−+"
                            PjButton(
                                k, Modifier.weight(1f).fillMaxHeight(),
                                selected = op && calc.activeOp == k[0] || k == "=",
                                stateDescription = if (op && calc.activeOp == k[0]) "Pending" else null,
                                textSize = 28.sp,
                            ) { press(k) }
                        }
                    }
                }
            }
        }
    }
}

// ---- Stopwatch

@Composable
private fun StopwatchScreen(vm: LauncherViewModel, onBack: () -> Unit) {
    val sw = vm.stopwatch
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    // Ticks only while this screen is visible and the watch is running.
    LaunchedEffect(sw.running, lifecycle) {
        now = SystemClock.elapsedRealtime()
        if (!sw.running) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { now = SystemClock.elapsedRealtime(); delay(33) }
        }
    }
    val elapsed = sw.elapsed(now)
    ToolFrame("Stopwatch", onBack) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                PjText(formatStopwatch(elapsed), size = 60.sp, weight = FontWeight.Light, tabular = true, fit = true, align = TextAlign.Center)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val laps = sw.laps
                items(laps.indices.reversed().toList()) { i ->
                    val split = laps[i] - (if (i > 0) laps[i - 1] else 0L)
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        PjText("LAP ${i + 1}", size = PjType.label, color = Pj.muted, spacing = 1.sp, modifier = Modifier.widthIn(min = 80.dp))
                        PjText(formatStopwatch(split), size = PjType.body, tabular = true, modifier = Modifier.weight(1f))
                        PjText(formatStopwatch(laps[i]), size = PjType.body, color = Pj.muted, tabular = true)
                    }
                }
            }
            FitGroup {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton(if (sw.running) "STOP" else "START", Modifier.weight(1f), selected = sw.running) { if (sw.running) sw.stop() else sw.start() }
                    if (sw.running) BigButton("LAP", Modifier.weight(1f)) { sw.lap() }
                    else BigButton("RESET", Modifier.weight(1f)) { sw.reset() }
                }
            }
        }
    }
}

// ---- Notes

@Composable
private fun NotesScreen(vm: LauncherViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val voice = vm.voice
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var micDenied by remember { mutableStateOf(false) }

    // Recording never continues in the background: leaving the screen or the launcher saves it and stops.
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) voice.release() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); voice.release() }
    }
    val startRecording = { if (voice.startRecording()) micDenied = false }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micDenied = !granted
        if (granted) startRecording()
    }
    fun record() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
        else askMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    ToolFrame("Notes", onBack) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FitGroup {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PjButton("NOTES", Modifier.weight(1f), selected = tab == 0, glyph = Glyph.NOTES) { tab = 0 }
                    PjButton("VOICE", Modifier.weight(1f), selected = tab == 1, glyph = Glyph.MIC) { tab = 1 }
                }
            }
            if (tab == 0) TextNotes(vm) else VoiceMemoList(vm, vm.motionEnabled, micDenied, onRecord = ::record, onAppInfo = { openAppInfo(ctx, ctx.packageName) })
        }
    }
}

@Composable
private fun TextNotes(vm: LauncherViewModel) {
    val notes = vm.notes.items
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (notes.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                PjText("No notes yet. Tap NEW NOTE. Notes stay on this phone only.", color = Pj.muted, align = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notes, key = { it.id }) { n ->
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 72.dp).background(Pj.surface, PjShape).border(1.dp, Pj.edge, PjShape)
                            .pjClick(interaction) { vm.openNote(n.id) }.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PjText(n.title, weight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
                            PjText(relativeTime(now, n.updated), size = PjType.caption, color = Pj.muted)
                        }
                        if (n.preview.isNotEmpty()) PjText(n.preview, size = PjType.label, color = Pj.muted, maxLines = 2)
                    }
                }
            }
        }
        BigButton("NEW NOTE", Modifier.fillMaxWidth(), glyph = Glyph.NOTES) { vm.openNote(System.currentTimeMillis()) }
    }
}

/** A large, thumb-sized action for the bottom of a screen on a small phone. */
@Composable
private fun BigButton(label: String, modifier: Modifier = Modifier, glyph: Glyph? = null, selected: Boolean = false, onClick: () -> Unit) {
    val tone = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .heightIn(min = 80.dp)
            .background(tone.copy(alpha = if (selected) 0.28f else 0.12f), PjShape)
            .border(2.dp, if (selected) tone else Pj.edge, PjShape)
            .pjClick(interaction, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        if (glyph != null) Canvas(Modifier.size(28.dp)) { drawGlyph(glyph, tone) }
        PjText(label, size = 22.sp, weight = FontWeight.SemiBold, spacing = 2.sp, color = if (selected) tone else Pj.text, fit = true)
    }
}

@Composable
private fun VoiceMemoList(vm: LauncherViewModel, motion: Boolean, micDenied: Boolean, onRecord: () -> Unit, onAppInfo: () -> Unit) {
    val voice = vm.voice
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var confirm by remember { mutableLongStateOf(-1L) }
    // Clock for the recording timer and playback position: only while recording or playing, and visible.
    LaunchedEffect(voice.recording, voice.playing, lifecycle) {
        if (!voice.recording && !voice.playing) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { now = SystemClock.elapsedRealtime(); delay(200) }
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (voice.recording) {
            Column(
                Modifier.fillMaxWidth().background(Pj.surface, PjShape).border(1.dp, Pj.warn, PjShape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PjText("RECORDING", size = PjType.label, color = Pj.warn, weight = FontWeight.SemiBold, spacing = 1.5.sp, modifier = Modifier.weight(1f))
                    PjText(formatDuration(now - voice.startedAt), size = 34.sp, weight = FontWeight.Light, tabular = true)
                }
                Waveform(voice, calm = !motion, Modifier.fillMaxWidth().height(96.dp))
            }
        }
        if (micDenied) {
            Column(Modifier.fillMaxWidth().border(1.dp, Pj.edge, PjShape).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PjText("Microphone permission is needed to record. Nothing is recorded without it.", size = PjType.label)
                PjButton("OPEN APP INFO", Modifier.fillMaxWidth(), fit = false, onClick = onAppInfo)
            }
        }
        if (voice.items.isEmpty() && !voice.recording) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                PjText(
                    "No voice memos yet. Tap RECORD. Recordings stay on this phone, and it only records while this screen is open.",
                    color = Pj.muted, align = TextAlign.Center,
                )
            }
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(voice.items, key = { it.id }) { m ->
                val active = voice.playingId == m.id
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 76.dp).background(Pj.surface, PjShape).border(1.dp, if (active) LocalAccent.current else Pj.edge, PjShape)
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PjButton(
                        if (active && voice.playing) "PAUSE" else "PLAY", Modifier.widthIn(min = 80.dp),
                        selected = active && voice.playing,
                    ) { voice.togglePlay(m.id) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PjText(
                            java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(m.id)),
                            weight = FontWeight.SemiBold, maxLines = 1,
                        )
                        @Suppress("UNUSED_VARIABLE") val tick = now // re-read while playing so the position updates
                        PjText(
                            if (active) "${formatDuration(voice.position())} / ${formatDuration(m.durationMs)}" else formatDuration(m.durationMs),
                            size = PjType.label, color = Pj.muted, tabular = true,
                        )
                    }
                    if (confirm == m.id) {
                        PjButton("DELETE?", Modifier.widthIn(min = 80.dp).padding(end = 8.dp), selected = true) { voice.delete(m.id); confirm = -1L }
                    } else {
                        val di = remember { MutableInteractionSource() }
                        Box(
                            Modifier.size(56.dp).pjClick(di) { confirm = m.id }
                                .semantics { contentDescription = "Delete voice memo" },
                            contentAlignment = Alignment.Center,
                        ) { Canvas(Modifier.size(18.dp)) { drawGlyph(Glyph.CLOSE, Pj.muted) } }
                    }
                }
            }
        }
        RecordButton(recording = voice.recording, onClick = { if (voice.recording) voice.stopRecording() else onRecord() })
    }
}

/** The one big control on the Voice tab, at the bottom where a thumb reaches. RECORD becomes STOP while recording. */
@Composable
private fun RecordButton(recording: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val tone = if (recording) Pj.warn else accent
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .background(tone.copy(alpha = if (recording) 0.28f else 0.16f), PjShape)
            .border(2.dp, tone, PjShape)
            .pjClick(interaction, onClick = onClick)
            .semantics { stateDescription = if (recording) "Recording" else "Not recording" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        if (recording) {
            Canvas(Modifier.size(22.dp)) { drawRoundRect(tone, cornerRadius = CornerRadius(3.dp.toPx())) }
        } else {
            Canvas(Modifier.size(30.dp)) { drawGlyph(Glyph.MIC, tone) }
        }
        PjText(if (recording) "STOP" else "RECORD", size = 22.sp, weight = FontWeight.SemiBold, spacing = 2.sp, color = tone)
    }
}

/**
 * Live sound waves: the microphone's loudness scrolls across as mirrored, glowing bars, brightest at the newest edge.
 * Runs only while recording and visible. With reduced motion it updates slowly instead of at ~20 fps.
 */
@Composable
private fun Waveform(voice: VoiceMemos, calm: Boolean, modifier: Modifier) {
    val accent = LocalAccent.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val levels = remember { FloatArray(48) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(voice.recording, lifecycle) {
        levels.fill(0f)
        if (!voice.recording) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                // The square root lifts quiet sounds so ordinary speech moves the bars visibly.
                val l = kotlin.math.sqrt(voice.level())
                System.arraycopy(levels, 1, levels, 0, levels.size - 1)
                levels[levels.size - 1] = l
                frame++
                delay(if (calm) 250L else 50L)
            }
        }
    }
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE") val redraw = frame
        val n = levels.size
        val step = size.width / n
        val cy = size.height / 2f
        val minH = 3.dp.toPx()
        val maxH = size.height * 0.94f
        drawLine(accent.copy(alpha = 0.25f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1.dp.toPx())
        for (i in 0 until n) {
            val age = i / (n - 1f)                       // 0 = oldest, 1 = newest
            val h = minH + levels[i] * (maxH - minH)
            val x = (i + 0.5f) * step
            val a = 0.12f + 0.88f * age * age
            val core = androidx.compose.ui.graphics.lerp(accent, Color.White, 0.65f * age * age)
            drawLine(accent.copy(alpha = 0.22f * a), Offset(x, cy - h / 2), Offset(x, cy + h / 2), strokeWidth = step * 1.9f, cap = StrokeCap.Round, blendMode = BlendMode.Plus)
            drawLine(core.copy(alpha = a), Offset(x, cy - h / 2), Offset(x, cy + h / 2), strokeWidth = step * 0.5f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun NoteEditScreen(vm: LauncherViewModel, id: Long, onBack: () -> Unit) {
    val accent = LocalAccent.current
    var text by remember(id) { mutableStateOf(vm.notes.find(id)?.text.orEmpty()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(id) { if (text.isEmpty()) focus.requestFocus() }
    // Saved on every edit; a blank note is dropped by the store.
    ToolFrame("Note", onBack, trailing = {
        PjButton("DELETE", Modifier.widthIn(min = 96.dp)) { text = ""; vm.notes.delete(id); onBack() }
    }) {
        BasicTextField(
            value = text,
            onValueChange = { text = it; vm.notes.save(id, it) },
            modifier = Modifier.fillMaxSize().focusRequester(focus),
            textStyle = TextStyle(color = Pj.text, fontSize = PjType.body, fontFamily = PjFont, lineHeight = 26.sp),
            cursorBrush = SolidColor(accent),
        )
    }
}
