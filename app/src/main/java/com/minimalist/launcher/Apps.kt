package com.minimalist.launcher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.widget.Toast
import android.net.Uri
import android.provider.Settings

data class AppEntry(val label: String, val component: ComponentName) {
    val key: String get() = component.flattenToString()
    val packageName: String get() = component.packageName
}

/** Every app with a launcher icon, sorted by name. This app itself is excluded. */
fun loadApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(query, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            AppEntry(
                it.loadLabel(pm).toString(),
                ComponentName(it.activityInfo.packageName, it.activityInfo.name),
            )
        }
        .distinctBy { it.key }
        .sortedBy { it.label.lowercase() }
}

enum class LaunchResult { OK, NEEDS_SETUP, MISSING }

fun launchComponent(context: Context, flattened: String): Boolean {
    val cn = ComponentName.unflattenFromString(flattened) ?: return false
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(cn)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/** Opens the user's chosen app for [slot], or the system default when none is chosen. */
fun launchSlot(context: Context, slot: Slot, assigned: String?): LaunchResult {
    if (assigned != null) {
        return if (launchComponent(context, assigned)) LaunchResult.OK else LaunchResult.MISSING
    }
    val intent = slot.defaultIntent() ?: return LaunchResult.NEEDS_SETUP
    return try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        LaunchResult.OK
    } catch (_: ActivityNotFoundException) {
        LaunchResult.MISSING
    }
}

fun openAppInfo(context: Context, packageName: String) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
    }
}

private const val CLAUDE_PACKAGE = "com.anthropic.claude"
private const val CLAUDE_VOICE_SHORTCUT = "quick_action_voice"

/**
 * Starts Claude's own "Start voice" app shortcut, the same one a long-press on its icon shows. Only the Home app may
 * start another app's shortcut. If that shortcut is missing, falls back to just opening Claude.
 */
fun openClaudeVoice(context: Context) {
    val la = context.getSystemService(LauncherApps::class.java)
    val started = try {
        val query = LauncherApps.ShortcutQuery()
            .setPackage(CLAUDE_PACKAGE)
            .setShortcutIds(listOf(CLAUDE_VOICE_SHORTCUT))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        val shortcut = la?.getShortcuts(query, Process.myUserHandle())?.firstOrNull { it.isEnabled }
        if (la != null && shortcut != null) { la.startShortcut(shortcut, null, null); true } else false
    } catch (_: Exception) {
        false
    }
    if (started) return
    val open = context.packageManager.getLaunchIntentForPackage(CLAUDE_PACKAGE)
    if (open != null) {
        context.startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } else {
        Toast.makeText(context, "Claude is not installed", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens Developer options. With [settingKey] ("enable_adb", "toggle_adb_wireless") Settings scrolls to that switch and
 * highlights it; the sub-screens themselves are not exported, so this is as direct as an app can get.
 */
fun openDeveloperOptions(context: Context, settingKey: String? = null) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (settingKey != null) intent.putExtra(":settings:fragment_args_key", settingKey)
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Developer options are not available", Toast.LENGTH_SHORT).show()
    }
}

fun developerOptionsOn(context: Context): Boolean =
    Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

/** Whether USB debugging / Wireless debugging are on. Reading these needs no permission. */
fun usbDebuggingOn(context: Context): Boolean =
    Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

fun wirelessDebuggingOn(context: Context): Boolean =
    Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
