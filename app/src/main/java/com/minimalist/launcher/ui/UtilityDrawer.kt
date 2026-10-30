package com.minimalist.launcher.ui

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalist.launcher.LauncherViewModel
import com.minimalist.launcher.Slot

/**
 * Quick-launch panel. Two groups, so the seven choices read as "apps I reach for" and "the rest":
 * Camera / Music / Notes / Torch, then Phone settings / All apps, with launcher Setup as a quiet link.
 */
@Composable
fun UtilityDrawer(vm: LauncherViewModel, onSlot: (Slot) -> Unit) {
    val motion = vm.motionEnabled
    val noRipple = remember { MutableInteractionSource() }

    // 0 = hidden, 1 = fully open. The panel follows the finger while dragged (down here, up from Home via
    // vm.drawerPull) and animates to open/closed on release.
    var progress by remember { mutableFloatStateOf(if (vm.drawerOpen) 1f else 0f) }
    var dragging by remember { mutableStateOf(false) }
    val pulling = vm.drawerPull >= 0f
    LaunchedEffect(vm.drawerPull) { if (vm.drawerPull >= 0f) progress = vm.drawerPull }
    LaunchedEffect(vm.drawerOpen, pulling, dragging, motion) {
        if (pulling || dragging) return@LaunchedEffect
        val target = if (vm.drawerOpen) 1f else 0f
        if (motion) animate(progress, target, animationSpec = tween(220)) { v, _ -> progress = v } else progress = target
    }
    val scroll = rememberScrollState()
    val visible = progress > 0f || vm.drawerOpen || pulling

    if (visible) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000).copy(alpha = 0.667f * progress))
                    .then(
                        if (vm.drawerOpen) Modifier.clickable(interactionSource = noRipple, indication = null) { vm.drawerOpen = false }
                        else Modifier
                    )
            )
            val shape = CutCornerShape(topStart = 22.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { vm.drawerHeightPx = it.height.toFloat() }
                    .graphicsLayer { translationY = (1f - progress) * size.height }
                    .background(Pj.surface, shape)
                    .border(1.dp, Pj.edge, shape)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = {
                                dragging = false
                                if (progress < 0.7f) vm.drawerOpen = false
                            },
                            onDragCancel = { dragging = false },
                            onVerticalDrag = { change, dy ->
                                change.consume()
                                val h = vm.drawerHeightPx.coerceAtLeast(1f)
                                progress = (progress - dy / h).coerceIn(0f, 1f)
                            },
                        )
                    }
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(scroll, enabled = scroll.maxValue > 0)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).background(Pj.edge, RoundedCornerShape(2.dp)))
                }
                PjText("QUICK LAUNCH", size = PjType.caption, weight = FontWeight.SemiBold, spacing = 2.sp, color = LocalAccent.current)

                FitGroup {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DrawerTile("CAMERA", Glyph.CAMERA, Modifier.weight(1f)) { onSlot(Slot.CAMERA) }
                        DrawerTile("MUSIC", Glyph.MUSIC, Modifier.weight(1f)) { onSlot(Slot.MUSIC) }
                    }
                }
                FitGroup {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DrawerTile("NOTES", Glyph.NOTES, Modifier.weight(1f)) { onSlot(Slot.NOTES) }
                        TorchButton(Modifier.weight(1f))
                    }
                }
                FitGroup {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DrawerTile("SETTINGS", Glyph.SETTINGS, Modifier.weight(1f)) { onSlot(Slot.SETTINGS) }
                        DrawerTile("ALL APPS", Glyph.APPS, Modifier.weight(1f)) { vm.openApps() }
                    }
                }

                // Launcher configuration is a link, not a fifth button: it is used rarely.
                val setupInteraction = remember { MutableInteractionSource() }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).pjClick(setupInteraction) { vm.openSettings() },
                    contentAlignment = Alignment.Center,
                ) {
                    PjText("LAUNCHER SETUP", size = PjType.caption, weight = FontWeight.SemiBold, spacing = 1.5.sp, color = LocalAccent.current, align = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun TorchButton(modifier: Modifier) {
    val ctx = LocalContext.current
    val cm = remember { ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val torchId = remember {
        runCatching {
            cm.cameraIdList.firstOrNull {
                val c = cm.getCameraCharacteristics(it)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }
    // The torch callback is registered only while the drawer is on screen.
    DisposableEffect(torchId) {
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchId) on = enabled
            }
        }
        if (torchId != null) cm.registerTorchCallback(cb, null)
        onDispose { if (torchId != null) cm.unregisterTorchCallback(cb) }
    }
    DrawerTile(
        label = if (torchId == null) "NO TORCH" else if (on) "TORCH ON" else "TORCH",
        glyph = Glyph.TORCH,
        modifier = modifier,
        selected = on,
        stateDescription = if (torchId == null) "Unavailable" else if (on) "On" else "Off",
    ) {
        if (torchId != null) runCatching { cm.setTorchMode(torchId, !on) }
    }
}

/** Large vertical tile (icon over label) matching the Home tiles, sized for thumb reach on the small screen. */
@Composable
fun DrawerTile(
    label: String,
    glyph: Glyph,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    stateDescription: String? = null,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .heightIn(min = 104.dp)
            .machinedTile(pressed || selected, accent)
            .pjClick(interaction, onClick = onClick)
            .semantics { if (stateDescription != null) this.stateDescription = stateDescription },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(36.dp)) { drawGlyph(glyph, accent) }
            PjText(
                label, size = PjType.label, weight = FontWeight.SemiBold, spacing = 1.2.sp,
                color = if (selected) accent else Pj.text, align = TextAlign.Center, fit = true,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}
