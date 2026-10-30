package com.minimalist.launcher.ui

import android.content.ComponentName
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalist.launcher.IconMasks
import com.minimalist.launcher.LauncherViewModel
import com.minimalist.launcher.Screen
import com.minimalist.launcher.Slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

private val FAVORITES = listOf(Slot.FAV1, Slot.FAV2, Slot.FAV3, Slot.FAV4, Slot.FAV5, Slot.FAV6)

/** Two home pages: the main dial, and a page of six favorite apps. Swipe left/right, or tap the dots. */
@Composable
fun HomePager(
    vm: LauncherViewModel,
    battery: BatteryInfo,
    onSlot: (Slot) -> Unit,
    onRequestHome: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = vm.homePage) { 2 }
    // The ViewModel owns the page so Home/Back can reset it and it survives recreation.
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect {
            vm.homePage = it
            if (it == 1) vm.prefs.markHintSeen()
        }
    }
    LaunchedEffect(vm.homePage) {
        if (pager.currentPage != vm.homePage) {
            if (vm.motionEnabled) pager.animateScrollToPage(vm.homePage) else pager.scrollToPage(vm.homePage)
        }
    }
    val fallbackPx = with(LocalDensity.current) { 460.dp.toPx() }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        val p = vm.drawerPull
                        if (p > 0.2f) vm.drawerOpen = true
                        vm.drawerPull = -1f
                    },
                    onDragCancel = { vm.drawerPull = -1f },
                    onVerticalDrag = { _, dy ->
                        if (vm.eggHolding) return@detectVerticalDragGestures
                        total += dy
                        if (total < 0f) {
                            vm.notificationsOpen = false
                            val h = if (vm.drawerHeightPx > 0f) vm.drawerHeightPx else fallbackPx
                            vm.drawerPull = (-total / h).coerceIn(0f, 1f)
                        }
                    },
                )
            },
    ) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), userScrollEnabled = !vm.eggHolding) { page ->
            if (page == 0) HomeScreen(vm, battery, onSlot, onRequestHome)
            else FavoritesPage(vm, onSlot)
        }
        PageDots(current = vm.homePage, onSelect = { vm.homePage = it })
    }
}

@Composable
private fun PageDots(current: Int, onSelect: (Int) -> Unit) {
    val accent = LocalAccent.current
    Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(2) { i ->
            val interaction = remember { MutableInteractionSource() }
            val on = i == current
            Box(
                Modifier
                    .width(64.dp).height(52.dp)
                    .semantics { contentDescription = "Page ${i + 1} of 2"; selected = on }
                    .pjClick(interaction, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = if (on) 20.dp else 8.dp, height = 8.dp)
                        .background(if (on) accent else Pj.edge, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun FavoritesPage(vm: LauncherViewModel, onSlot: (Slot) -> Unit) {
    val accent = LocalAccent.current
    // Same font-scale cap as the main page so tiles keep their shape.
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, min(base.fontScale, 1.5f))) {
        Column(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                PjText("FAVORITES", size = PjType.label, weight = FontWeight.SemiBold, spacing = 2.sp, color = accent, modifier = Modifier.weight(1f))
                PjText("Hold a tile to change", size = PjType.caption, color = Pj.muted, maxLines = 1)
            }
            for (row in 0 until 3) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FavTile(vm, FAVORITES[row * 2], onSlot)
                    FavTile(vm, FAVORITES[row * 2 + 1], onSlot)
                }
            }
        }
    }
}

@Composable
private fun RowScope.FavTile(vm: LauncherViewModel, slot: Slot, onSlot: (Slot) -> Unit) {
    val accent = LocalAccent.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val key = vm.prefs.assigned[slot]
    Box(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .heightIn(min = 56.dp)
            .machinedTile(pressed, accent)
            .pjClick(interaction, onLongClick = { vm.pick(slot, Screen.Home) }) { onSlot(slot) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (key == null) {
                Canvas(Modifier.size(36.dp)) {
                    val w = 2.dp.toPx()
                    drawLine(accent.copy(alpha = 0.7f), Offset(size.width / 2, 4.dp.toPx()), Offset(size.width / 2, size.height - 4.dp.toPx()), w, StrokeCap.Round)
                    drawLine(accent.copy(alpha = 0.7f), Offset(4.dp.toPx(), size.height / 2), Offset(size.width - 4.dp.toPx(), size.height / 2), w, StrokeCap.Round)
                }
                PjText("ADD", size = PjType.label, weight = FontWeight.SemiBold, spacing = 1.5.sp, color = Pj.muted)
            } else {
                ThemedIcon(key, 52.dp, accent)
                PjText(
                    vm.apps.firstOrNull { it.key == key }?.label ?: "Missing app",
                    size = PjType.label, weight = FontWeight.Medium, align = TextAlign.Center, maxLines = 2,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/** An app's icon redrawn as a single-colour glyph in [tint]. Generated on first use, cached, recoloured for free. */
@Composable
fun ThemedIcon(component: String, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val mask = remember(component, px) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(component, px) {
        mask.value = withContext(Dispatchers.Default) {
            ComponentName.unflattenFromString(component)?.let { IconMasks.load(ctx, it, px) }?.asImageBitmap()
        }
    }
    Box(modifier.size(size)) {
        mask.value?.let { Image(it, null, Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint, BlendMode.SrcIn)) }
    }
}
