package com.minimalist.launcher.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.minimalist.launcher.AppEntry
import com.minimalist.launcher.BatteryTone
import com.minimalist.launcher.batteryTone
import com.minimalist.launcher.LauncherViewModel
import com.minimalist.launcher.Screen
import com.minimalist.launcher.Slot

@Composable
fun LauncherApp(
    vm: LauncherViewModel,
    onLaunchSlot: (Slot) -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onRequestHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
) {
    val view = LocalView.current
    val prefs = vm.prefs
    val haptic: () -> Unit = remember(prefs.haptics, view) {
        {
            if (prefs.haptics) {
                view.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
                )
            }
        }
    }

    val battery by rememberBattery()
    val target = when (batteryTone(battery.percent)) {
        BatteryTone.CRITICAL -> Pj.critical
        BatteryTone.MEDIUM -> Pj.medium
        BatteryTone.NORMAL -> prefs.accent.color
    }
    // Fade between tones unless motion is off.
    val tone = if (vm.motionEnabled) animateColorAsState(target, tween(700), label = "tone").value else target
    SideEffect { Pj.tint = tone }

    CompositionLocalProvider(LocalHaptic provides haptic, LocalAccent provides tone) {
        // Always handled: on the root Home page Back does nothing, instead of letting Android play its
        // move-task-to-back animation and bring Home straight back.
        BackHandler { vm.back() }
        Box(Modifier.fillMaxSize().background(Pj.bg)) {
            when (val s = vm.screen) {
                Screen.Home -> {
                    HomePager(vm, battery, onLaunchSlot, onRequestHome)
                    UtilityDrawer(vm, onSlot = onLaunchSlot)
                    NotificationSheet(vm)
                }
                Screen.Apps -> AppListScreen(vm, pickSlot = null, onBack = vm::back, onOpen = onLaunchApp, onChoose = {})
                Screen.Settings -> SettingsScreen(vm, onBack = vm::back, onRequestHome = onRequestHome, onOpenHomeSettings = onOpenHomeSettings)
                Screen.Tools -> ToolsScreen(vm, onBack = vm::back)
                is Screen.Tool -> ToolScreen(vm, s.kind, onBack = vm::back)
                is Screen.NoteEdit -> NoteEditScreen(vm, s.id, onBack = vm::back)
                is Screen.Pick -> AppListScreen(
                    vm, pickSlot = s.slot, onBack = vm::back, onOpen = {},
                    onChoose = { app ->
                        prefs.assign(s.slot, app?.key)
                        vm.back()
                    },
                )
            }

            CreditsEgg(vm, tone)

            vm.missingSlot?.let { slot ->
                Dialog(onDismissRequest = { vm.missingSlot = null }) {
                    Column(
                        Modifier.padding(horizontal = 8.dp).fillMaxWidth().background(Pj.surface, PjPanel)
                            .border(1.dp, Pj.edge, PjPanel).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PjText("${slot.label} app unavailable", size = PjType.title, weight = FontWeight.SemiBold)
                        PjText(
                            "The app for this shortcut is not installed or cannot be opened. Choose another one.",
                            size = PjType.label, color = Pj.muted,
                        )
                        PjButton("CHOOSE APP", Modifier.fillMaxWidth()) {
                            vm.missingSlot = null
                            vm.pick(slot, Screen.Home)
                        }
                        if (slot.hasDefault) {
                            PjButton(if (slot.hasBuiltIn) "USE BUILT-IN" else "USE SYSTEM DEFAULT", Modifier.fillMaxWidth()) {
                                prefs.assign(slot, null); vm.missingSlot = null
                            }
                        }
                        PjButton("DISMISS", Modifier.fillMaxWidth()) { vm.missingSlot = null }
                    }
                }
            }
        }
    }
}
