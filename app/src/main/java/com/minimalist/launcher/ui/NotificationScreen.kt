package com.minimalist.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalist.launcher.LauncherViewModel
import com.minimalist.launcher.Note
import com.minimalist.launcher.NotificationStore
import com.minimalist.launcher.openAppInfo
import com.minimalist.launcher.openNotificationAccessSettings
import com.minimalist.launcher.relativeTime

/**
 * The notification center: what Android is currently showing in its shade, drawn in the launcher's own style.
 * A sheet that slides down from the top over Home, opened only by tapping the bell. Tap the dimmed area or press
 * Back to close it: no gestures, no back button.
 */
@Composable
fun NotificationSheet(vm: LauncherViewModel) {
    val motion = vm.motionEnabled
    val noRipple = remember { MutableInteractionSource() }

    // Close the sheet when the last clearable notification goes (CLEAR, or swiping away the final one).
    LaunchedEffect(Unit) {
        var hadClearable = false
        snapshotFlow { NotificationStore.items.any { it.clearable } }.collect { has ->
            if (hadClearable && !has) vm.notificationsOpen = false
            hadClearable = has
        }
    }

    AnimatedVisibility(
        visible = vm.notificationsOpen,
        enter = if (motion) fadeIn() else EnterTransition.None,
        exit = if (motion) fadeOut() else ExitTransition.None,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xAA000000))
                .clickable(interactionSource = noRipple, indication = null) { vm.notificationsOpen = false }
        )
    }
    AnimatedVisibility(
        visible = vm.notificationsOpen,
        modifier = Modifier.fillMaxSize(),
        enter = if (motion) slideInVertically { -it } else EnterTransition.None,
        exit = if (motion) slideOutVertically { -it } else ExitTransition.None,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val shape = CutCornerShape(bottomEnd = 22.dp)
            val ctx = LocalContext.current
            val items = NotificationStore.items
            val connected = NotificationStore.connected
            val canClear = vm.notificationAccess && items.any { it.clearable }
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.86f)
                    .background(Pj.surface, shape)
                    .border(1.dp, Pj.edge, shape)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PjText(
                        "NOTIFICATIONS", size = PjType.caption, weight = FontWeight.SemiBold, spacing = 2.sp,
                        color = LocalAccent.current, modifier = Modifier.weight(1f),
                    )
                    if (canClear) PjButton("CLEAR", Modifier.widthIn(min = 88.dp)) { NotificationStore.clearAll() }
                }
                when {
                    !vm.notificationAccess -> Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        AccessCard(
                            onEnable = { openNotificationAccessSettings(ctx) },
                            onAppInfo = { openAppInfo(ctx, ctx.packageName) },
                        )
                    }
                    !connected -> Message("Waiting for Android to connect. This takes a few seconds after access is turned on.")
                    items.isEmpty() -> Message("No notifications.")
                    else -> {
                        val now = System.currentTimeMillis()
                        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(items, key = { it.key }) { note -> NoteRow(note, now) { NotificationStore.open(ctx, note) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(24.dp), contentAlignment = Alignment.Center) {
        PjText(text, color = Pj.muted, align = TextAlign.Center)
    }
}

@Composable
private fun AccessCard(onEnable: () -> Unit, onAppInfo: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Pj.surface, PjPanel).border(1.dp, Pj.edge, PjPanel).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PjText("SHOW NOTIFICATIONS HERE", size = PjType.label, weight = FontWeight.SemiBold, spacing = 1.5.sp, color = LocalAccent.current)
        PjText(
            "Android asks you to switch on notification access for Minimalist Launcher. It then reads your notifications to " +
                "list them in this screen.",
            size = PjType.label,
        )
        PjText(
            "They stay on this phone and in memory only. Nothing is saved, and there is no internet permission, so nothing " +
                "can be sent anywhere. Turn it off any time in the same system screen.",
            size = PjType.caption, color = Pj.muted,
        )
        PjButton("OPEN ACCESS SETTINGS", Modifier.fillMaxWidth(), fit = false, onClick = onEnable)
        PjText(
            "If the switch is greyed out as a restricted setting, Android is protecting an app installed outside the Play " +
                "Store. Open App info, tap the three dots at the top right, choose Allow restricted settings, then try again.",
            size = PjType.caption, color = Pj.muted,
        )
        PjButton("OPEN APP INFO", Modifier.fillMaxWidth(), fit = false, onClick = onAppInfo)
    }
}

@Composable
private fun NoteRow(note: Note, now: Long, onOpen: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val accent = LocalAccent.current
    val ctx = LocalContext.current
    val icon = remember(note.pkg) { ctx.packageManager.getLaunchIntentForPackage(note.pkg)?.component?.flattenToString() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(if (note.ongoing) Color.Transparent else Pj.surface, PjShape)
            .border(1.dp, if (note.ongoing) Pj.line else Pj.edge, PjShape)
            .pjClick(interaction, onClick = onOpen)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) ThemedIcon(icon, 28.dp, accent) else Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(8.dp)) { drawCircle(accent) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PjText(note.app.uppercase(), size = PjType.caption, color = accent, spacing = 1.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                PjText(relativeTime(now, note.whenMs), size = PjType.caption, color = Pj.muted, maxLines = 1)
            }
            if (note.title.isNotEmpty()) PjText(note.title, weight = FontWeight.SemiBold, maxLines = 1)
            if (note.text.isNotEmpty()) PjText(note.text, size = PjType.label, color = Pj.muted, maxLines = 2)
        }
        if (note.clearable) {
            val di = remember { MutableInteractionSource() }
            Box(
                Modifier.size(56.dp).pjClick(di) { NotificationStore.dismiss(note) }
                    .semantics { contentDescription = "Dismiss ${note.app} notification" },
                contentAlignment = Alignment.Center,
            ) { Canvas(Modifier.size(18.dp)) { drawGlyph(Glyph.CLOSE, Pj.muted) } }
        } else Box(Modifier.size(12.dp))
    }
}
