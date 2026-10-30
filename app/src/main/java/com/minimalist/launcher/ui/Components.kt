package com.minimalist.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalist.launcher.R

/**
 * Palette. Panels, borders and text tints are derived from [tint] (the active battery tone), so the whole UI
 * recolours together. [tint] is snapshot state: anything that reads these colours updates when it changes.
 */
object Pj {
    var tint by mutableStateOf(Color(0xFF22D3EE))
    val bg = Color(0xFF04070A)
    val surface: Color get() = lerp(bg, tint, 0.06f)

    /** Decorative lines only: unlit graduations, dividers. Not a component boundary. */
    val line: Color get() = lerp(bg, tint, 0.23f)

    /** The dial's empty track says how much charge is missing, so it is a little stronger than decoration. */
    val track: Color get() = lerp(bg, tint, 0.34f)

    /** Component edges and inactive indicators: solved per tone to stay at least 3:1 against [bg]. */
    val edge: Color
        get() {
            val t = tint
            if (t != edgeFor) {
                edgeColor = lerp(bg, t, edgeMix(t, bg, EDGE_CONTRAST))
                edgeFor = t
            }
            return edgeColor
        }
    private var edgeFor = Color.Unspecified
    private var edgeColor = Color.Unspecified
    private const val EDGE_CONTRAST = 3.2f

    val text: Color get() = lerp(Color.White, tint, 0.10f)
    val muted: Color get() = lerp(Color(0xFF9BA3A6), tint, 0.25f)
    val warn = Color(0xFFFFB454)
    val medium = Color(0xFFFFD84D)
    val critical = Color(0xFFFF4D5E)
}

/** Type roles. Nothing informational goes below [caption]. */
object PjType {
    val caption = 14.sp
    val label = 16.sp
    val body = 18.sp
    val title = 20.sp
}

/** Space Grotesk (SIL OFL, bundled as a variable font). */
@OptIn(ExperimentalTextApi::class)
val PjFont = FontFamily(
    listOf(
        FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold,
    ).map { w -> Font(R.font.space_grotesk, w, FontStyle.Normal, variationSettings = FontVariation.Settings(w, FontStyle.Normal)) }
)

/** Machined look: two opposite corners are chamfered, the others stay square. */
val PjShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
val PjPanel = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp)

/** Chamfered tile with a press glow and a hairline highlight along its top edge. */
fun Modifier.machinedTile(pressed: Boolean, accent: Color): Modifier = this
    .background(
        if (pressed) Brush.verticalGradient(listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.05f)))
        else SolidColor(Pj.surface),
        PjShape,
    )
    .border(1.dp, if (pressed) accent else Pj.edge, PjShape)
    .drawBehind {
        val y = 0.5.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.14f), Offset(10.dp.toPx(), y), Offset(size.width - 2.dp.toPx(), y), 1.dp.toPx())
    }

/** Labels in one group shrink together, so sibling buttons never end up at different sizes. */
val LocalFitGroup = staticCompositionLocalOf<MutableFloatState?> { null }

@Composable
fun FitGroup(content: @Composable () -> Unit) {
    val scale = remember { mutableFloatStateOf(1f) }
    CompositionLocalProvider(LocalFitGroup provides scale, content = content)
}

val LocalHaptic = staticCompositionLocalOf<() -> Unit> { {} }
val LocalAccent = compositionLocalOf { Color(0xFF22D3EE) }

@Composable
fun PjText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 15.sp,
    color: Color = Pj.text,
    weight: FontWeight = FontWeight.Normal,
    spacing: TextUnit = 0.sp,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    fit: Boolean = false,
    tabular: Boolean = false,
) {
    // fit: single line that shrinks (down to 55%) instead of breaking words when it does not fit.
    // Inside a FitGroup the smallest scale in the group wins, so siblings stay the same size.
    val group = LocalFitGroup.current
    var shrink by remember(text, size) { mutableFloatStateOf(1f) }
    val effective = if (fit && group != null) minOf(shrink, group.floatValue) else shrink
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = if (fit) 1 else maxLines,
        softWrap = !fit,
        overflow = if (fit) TextOverflow.Clip else TextOverflow.Ellipsis,
        onTextLayout = {
            if (fit && it.didOverflowWidth && effective > 0.55f) {
                shrink = effective * 0.92f
                if (group != null && shrink < group.floatValue) group.floatValue = shrink
            }
        },
        style = TextStyle(
            color = color,
            fontSize = if (fit) size * effective else size,
            fontWeight = weight,
            letterSpacing = spacing,
            textAlign = align,
            fontFamily = PjFont,
            fontFeatureSettings = if (tabular) "tnum" else null,
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pjClick(
    interaction: MutableInteractionSource,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val haptic = LocalHaptic.current
    return combinedClickable(
        interactionSource = interaction,
        indication = null,
        role = role,
        onLongClick = onLongClick?.let { lc -> { haptic(); lc() } },
        onClick = { haptic(); onClick() },
    )
}

/**
 * Fires [onHold] once the finger has stayed down for [holdMillis], longer than the system long-press. Only watches the
 * touch (never consumes it), so the button's own pressed highlight still shows while holding.
 */
@Composable
fun Modifier.pjHold(
    holdMillis: Long,
    onPress: () -> Unit = {},
    onRelease: (held: Boolean) -> Unit = {},
    onHold: () -> Unit,
): Modifier {
    val haptic = LocalHaptic.current
    val current by rememberUpdatedState(onHold)
    val pressed by rememberUpdatedState(onPress)
    val released by rememberUpdatedState(onRelease)
    return pointerInput(holdMillis) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            pressed()
            val held = try {
                withTimeout(holdMillis) { waitForUpOrCancellation(PointerEventPass.Initial) }
                false
            } catch (_: PointerEventTimeoutCancellationException) {
                true
            }
            if (held) {
                haptic(); current()
                released(true)
                waitForUpOrCancellation(PointerEventPass.Initial)
            } else released(false)
        }
    }
}

/** Outlined button with a 48 dp minimum height. [glyph] sits left of the label; [stateDescription] is read by TalkBack. */
@Composable
fun PjButton(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    fit: Boolean = true,
    glyph: Glyph? = null,
    stateDescription: String? = null,
    textSize: TextUnit = PjType.label,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = PjShape
    val ink = if (selected) accent else Pj.text
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .border(BorderStroke(1.dp, if (selected || pressed) accent else Pj.edge), shape)
            .background(if (selected) accent.copy(alpha = 0.22f) else if (pressed) accent.copy(alpha = 0.14f) else Color.Transparent, shape)
            .pjClick(interaction, onLongClick = onLongClick, onClick = onClick)
            .semantics { if (stateDescription != null) this.stateDescription = stateDescription }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (glyph != null) Canvas(Modifier.size(24.dp)) { drawGlyph(glyph, if (selected) accent else accent.copy(alpha = 0.9f)) }
            PjText(label, size = textSize, weight = FontWeight.SemiBold, spacing = 1.2.sp, color = ink, align = TextAlign.Center, fit = fit)
        }
    }
}

@Composable
fun PjSwitchRow(label: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    val haptic = LocalHaptic.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { haptic(); onChange(it) })
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f)) {
            androidx.compose.foundation.layout.Column {
                PjText(label, size = PjType.body)
                if (detail != null) PjText(detail, size = PjType.label, color = Pj.muted)
            }
        }
        Box(
            Modifier
                .size(width = 44.dp, height = 26.dp)
                .border(1.dp, if (checked) accent else Pj.edge, CircleShape)
                .background(if (checked) accent.copy(alpha = 0.25f) else Color.Transparent, CircleShape)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(18.dp).background(if (checked) accent else Pj.muted, CircleShape))
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PjButton("BACK", Modifier.widthIn(min = 80.dp), onClick = onBack)
        Box(Modifier.weight(1f)) { PjText(title.uppercase(), size = PjType.title, weight = FontWeight.SemiBold, spacing = 2.sp, color = LocalAccent.current, fit = true) }
        trailing?.invoke()
    }
}

/** Calls [onUp] / [onDown] once per vertical drag that travels past [thresholdPx]. */
fun Modifier.verticalSwipe(thresholdPx: Float, onUp: () -> Unit = {}, onDown: () -> Unit = {}): Modifier =
    pointerInput(thresholdPx) {
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if (total < -thresholdPx) onUp() else if (total > thresholdPx) onDown()
                total = 0f
            },
            onDragCancel = { total = 0f },
            onVerticalDrag = { _, dy -> total += dy },
        )
    }
