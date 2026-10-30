package com.minimalist.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.minimalist.launcher.PocketWallpaperService
import com.minimalist.launcher.openNotificationAccessSettings
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalist.launcher.Accent
import com.minimalist.launcher.ClockFormat
import com.minimalist.launcher.LauncherViewModel
import com.minimalist.launcher.Screen
import com.minimalist.launcher.Slot

@Composable
fun SettingsScreen(
    vm: LauncherViewModel,
    onBack: () -> Unit,
    onRequestHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
) {
    val prefs = vm.prefs
    val accent = LocalAccent.current
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ScreenHeader("Setup", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Home app
            Section("Home app", first = true)
            Group {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PjText(
                        if (vm.isDefaultHome) "Minimalist Launcher is your Home app." else "Minimalist Launcher is not your Home app yet.",
                        color = if (vm.isDefaultHome) accent else Pj.warn,
                    )
                    if (!vm.isDefaultHome) PjButton("SET AS HOME APP", Modifier.fillMaxWidth(), onClick = onRequestHome)
                    PjButton("CHANGE HOME APP", Modifier.fillMaxWidth(), fit = false, onClick = onOpenHomeSettings)
                }
            }
            Hint("To go back to your original launcher, choose it there. It is never removed or disabled.")

            // Shortcuts
            Section("Shortcuts")
            Group {
                val slots = Slot.entries.filter { !it.isFavorite }
                slots.forEachIndexed { i, slot ->
                    if (i > 0) Divider()
                    SlotRow(vm, slot)
                }
            }
            Hint("The torch toggle is built in. Long-press a home tile to change it quickly.")

            var favoritesOpen by rememberSaveable { mutableStateOf(false) }
            val favs = Slot.entries.filter { it.isFavorite }
            val favsSet = favs.count { prefs.assigned[it] != null }
            Group {
                NavRow("Favorites page", "$favsSet of ${favs.size} set", chevronOpen = favoritesOpen) { favoritesOpen = !favoritesOpen }
                if (favoritesOpen) favs.forEach { slot -> Divider(); SlotRow(vm, slot) }
            }
            Hint("Swipe left on the home screen. Icons are redrawn in the current theme colour.")

            // Look and feel
            Section("Appearance")
            Group {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PjText("Accent", size = PjType.label, color = Pj.muted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Accent.entries.forEach { a ->
                            val selected = a == prefs.accent
                            val interaction = remember { MutableInteractionSource() }
                            Box(
                                Modifier.weight(1f).heightIn(min = 56.dp)
                                    .pjClick(interaction, role = Role.RadioButton) { prefs.updateAccent(a) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(36.dp)
                                        .background(a.color, CircleShape)
                                        .border(if (selected) 3.dp else 0.dp, Pj.text, CircleShape)
                                )
                            }
                        }
                    }
                }
                Divider()
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PjText("Clock", size = PjType.label, color = Pj.muted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClockFormat.entries.forEach { f ->
                            PjButton(f.label.uppercase(), Modifier.weight(1f), selected = f == prefs.clockFormat) { prefs.updateClockFormat(f) }
                        }
                    }
                }
                Divider()
                WallpaperRow()
            }
            Hint("${prefs.accent.label}. Used above 40% battery. The whole UI turns yellow at 40% or below and red at 15% or below.")

            Section("Motion and feedback")
            Group {
                Column(Modifier.padding(horizontal = 14.dp)) {
                    PjSwitchRow(
                        "Reduce motion",
                        if (vm.systemReducesMotion) "System animations are off, so motion is already reduced." else "Skip intro and drawer animations.",
                        prefs.reduceMotion, prefs::updateReduceMotion,
                    )
                    Divider()
                    PjSwitchRow("Haptics", "Short tick on taps.", prefs.haptics, prefs::updateHaptics)
                    Divider()
                    PjSwitchRow(
                        "Showcase mode",
                        "Day-progress ring and seconds marker. Uses more battery.",
                        prefs.showcase, prefs::updateShowcase,
                    )
                }
            }

            // System
            Section("Notifications")
            val ctx = LocalContext.current
            Group {
                NavRow(
                    "Notification center",
                    if (vm.notificationAccess) "On" else "Off",
                    valueColor = if (vm.notificationAccess) accent else Pj.muted,
                ) { openNotificationAccessSettings(ctx) }
            }
            Hint(
                if (vm.notificationAccess) "Tap the bell on Home. Notifications are read on this phone, kept in memory only, and never saved or sent."
                else "Switch on notification access to list your notifications here. They are kept in memory only, never saved or sent. There is no internet permission."
            )

            Section("Gestures")
            Group {
                val gestures = listOf(
                    "Swipe up" to "Quick launch",
                    "Swipe left" to "Favorites page",
                    "Hold a tile" to "Change its app",
                    "Hold Claude" to "Voice chat",
                    "Hold an app" to "Hide or App info",
                )
                gestures.forEachIndexed { i, (g, what) ->
                    if (i > 0) Divider()
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PjText(g)
                        PjText(what, color = Pj.muted, size = PjType.label, align = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }
            }
            Hint("Swipes and holds work on Home. Holding an app works in the Apps list.")

            Section("Privacy")
            Hint(
                "Settings stay on this phone. No internet permission, accounts, analytics or ads. " +
                    "This launcher changes the home screen, app list and its own screens only. It does not replace the lock screen, " +
                    "the notification shade or system settings, and it does not stop Android's own telemetry."
            )
            Box(Modifier.size(16.dp))
        }
    }
}

/** Live wallpaper status and the system's own confirmation screen. The wallpaper never changes without it. */
@Composable
private fun WallpaperRow() {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(false) }
    // The picker is another screen: re-check when we come back.
    DisposableEffect(lifecycle) {
        val check = { active = WallpaperManager.getInstance(ctx).wallpaperInfo?.packageName == ctx.packageName }
        check()
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) check() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    if (active) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            PjText("Live wallpaper", size = PjType.label, color = Pj.muted)
            PjText("The Minimalist Launcher wallpaper is active.", color = LocalAccent.current)
        }
    } else {
        val ctx2 = ctx
        NavRow("Live wallpaper", "Set") {
            val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(ctx2, PocketWallpaperService::class.java))
            try { ctx2.startActivity(direct) } catch (_: ActivityNotFoundException) {
                try { ctx2.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) } catch (_: ActivityNotFoundException) {}
            }
        }
    }
}

@Composable
private fun SlotRow(vm: LauncherViewModel, slot: Slot) {
    NavRow(slot.label, vm.labelFor(slot).replace("System default", "Default")) { vm.pick(slot, Screen.Settings) }
}

/** A tappable row: label on the left, current value and a chevron on the right. */
@Composable
private fun NavRow(
    label: String,
    value: String,
    chevronOpen: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = Pj.muted,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .background(if (pressed) LocalAccent.current.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
            .pjClick(interaction, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PjText(label)
        PjText(
            value, color = valueColor, size = PjType.label, maxLines = 1, align = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Canvas(Modifier.size(18.dp).rotate(if (chevronOpen) 90f else 0f)) { drawGlyph(Glyph.CHEVRON, Pj.muted) }
    }
}

/** One bordered panel that groups related rows, so proximity and a frame do the grouping instead of spacing alone. */
@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, Pj.edge, PjPanel)) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pj.line))
}

@Composable
private fun Hint(text: String) {
    PjText(text, size = PjType.caption, color = Pj.muted, modifier = Modifier.padding(horizontal = 2.dp))
}

@Composable
private fun Section(title: String, first: Boolean = false) {
    PjText(
        title.uppercase(), size = PjType.caption, weight = FontWeight.SemiBold, spacing = 2.sp,
        color = LocalAccent.current, modifier = Modifier.padding(top = if (first) 4.dp else 18.dp),
    )
}
