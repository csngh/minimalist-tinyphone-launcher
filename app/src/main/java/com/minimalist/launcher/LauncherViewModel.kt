package com.minimalist.launcher

import android.app.Application
import android.app.role.RoleManager
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data object Apps : Screen
    data object Settings : Screen
    /** The built-in tools list. */
    data object Tools : Screen
    /** A built-in tool; [back] is where it was opened from. */
    data class Tool(val kind: ToolKind, val back: Screen) : Screen
    data class NoteEdit(val id: Long, val back: Screen) : Screen
    /** Choosing an app for [slot]; [back] is where to return afterwards. */
    data class Pick(val slot: Slot, val back: Screen) : Screen
}

enum class ToolKind { CALCULATOR, STOPWATCH, NOTES }

private const val AWAY_FOR_INTRO_MS = 10 * 60 * 1000L

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = LauncherPrefs(app)
    val notes = NotesStore(app)
    val voice = VoiceMemos(app)
    val stopwatch = StopwatchState(app)

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set
    var drawerOpen by mutableStateOf(false)
    /** 0..1 while a drag up from Home is pulling the Quick launch panel in; -1 when not pulling. */
    var drawerPull by mutableFloatStateOf(-1f)
    var drawerHeightPx = 0f
    /** Bumped on every resume so state read straight from system settings is re-read. */
    var resumeTick by mutableIntStateOf(0)
        private set
    /** The notification sheet, laid over Home exactly like the quick-launch drawer. */
    var notificationsOpen by mutableStateOf(false)
    /** 0 = main home page, 1 = favorites page. */
    var homePage by mutableIntStateOf(0)
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var missingSlot by mutableStateOf<Slot?>(null)
    var isDefaultHome by mutableStateOf(false)
        private set
    /** Whether the user has switched on notification access for this app in system settings. */
    var notificationAccess by mutableStateOf(false)
        private set
    /** Bumped when Home should play its short boot-in again (first launch, or back after a long gap). */
    var introToken by mutableIntStateOf(1)
        private set
    var introSeen = 0
    /** Easter egg: lightning out of the clock dial, then a credits page. */
    var eggActive by mutableStateOf(false)
    /** Finger is down on the dial, counting towards the 3-second hold. */
    var eggHolding by mutableStateOf(false)
    var eggOrigin by mutableStateOf(Offset.Zero)
    var eggRadius by mutableFloatStateOf(0f)
    var eggToken by mutableIntStateOf(0)
        private set
    fun setEggDial(origin: Offset, radius: Float) { eggOrigin = origin; eggRadius = radius }
    fun fireEgg() { eggToken++; eggActive = true }

    private var stoppedAt = 0L

    private var systemAnimationsOff by mutableStateOf(false)

    /** Motion is on unless the user or the system (animator scale 0) turned it off. */
    val motionEnabled: Boolean get() = !prefs.reduceMotion && !systemAnimationsOff
    val systemReducesMotion: Boolean get() = systemAnimationsOff

    override fun onCleared() { voice.release() }

    fun onStopped() { stoppedAt = SystemClock.elapsedRealtime() }

    fun onResume() {
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > AWAY_FOR_INTRO_MS) introToken++
        resumeTick++
        refreshRole()
        notificationAccess = hasNotificationAccess(getApplication())
        refreshApps()
        systemAnimationsOff = Settings.Global.getFloat(
            getApplication<Application>().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }

    fun refreshRole() {
        val rm = getApplication<Application>().getSystemService(RoleManager::class.java)
        isDefaultHome = rm?.isRoleHeld(RoleManager.ROLE_HOME) == true
    }

    private fun refreshApps() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.Default) { loadApps(getApplication()) }
            apps = list
        }
    }

    fun labelFor(slot: Slot): String {
        val key = prefs.assigned[slot]
        if (key == null) return if (slot.hasBuiltIn) "Built-in" else if (slot.hasDefault) "System default" else "Not set"
        val found = apps.firstOrNull { it.key == key }
        return found?.label ?: if (apps.isEmpty()) "…" else "Missing app"
    }

    fun goHome() { screen = Screen.Home; drawerOpen = false; notificationsOpen = false; homePage = 0 }
    fun openApps() { drawerOpen = false; notificationsOpen = false; screen = Screen.Apps }
    fun openSettings() { drawerOpen = false; notificationsOpen = false; screen = Screen.Settings }
    fun openNotifications() { drawerOpen = false; notificationsOpen = true }

    /** The built-in screen behind a Tools or Notes shortcut that has no app assigned. */
    fun openBuiltIn(slot: Slot) {
        drawerOpen = false
        notificationsOpen = false
        screen = if (slot == Slot.NOTES) Screen.Tool(ToolKind.NOTES, Screen.Home) else Screen.Tools
    }
    fun openTool(kind: ToolKind, back: Screen) { screen = Screen.Tool(kind, back) }
    fun openNote(id: Long) { screen = Screen.NoteEdit(id, screen) }
    fun pick(slot: Slot, back: Screen) { drawerOpen = false; notificationsOpen = false; screen = Screen.Pick(slot, back) }

    fun back() {
        when {
            eggActive -> eggActive = false
            notificationsOpen -> notificationsOpen = false
            drawerOpen -> drawerOpen = false
            screen is Screen.Pick -> screen = (screen as Screen.Pick).back
            screen is Screen.Tool -> screen = (screen as Screen.Tool).back
            screen is Screen.NoteEdit -> screen = (screen as Screen.NoteEdit).back
            screen != Screen.Home -> screen = Screen.Home
            homePage != 0 -> homePage = 0
        }
    }
}
