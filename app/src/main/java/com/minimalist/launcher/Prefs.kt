package com.minimalist.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** A configurable shortcut. [defaultIntent] is used when the user has not picked an app. */
enum class Slot(val key: String, val label: String) {
    CALL("call", "Call"),
    MESSAGE("message", "Message"),
    NAVIGATE("navigate", "Navigate"),
    TOOLS("tools", "Tools"),
    CAMERA("camera", "Camera"),
    MUSIC("music", "Music"),
    NOTES("notes", "Notes"),
    SETTINGS("settings", "Phone settings"),
    FAV1("fav1", "Favorite 1"),
    FAV2("fav2", "Favorite 2"),
    FAV3("fav3", "Favorite 3"),
    FAV4("fav4", "Favorite 4"),
    FAV5("fav5", "Favorite 5"),
    FAV6("fav6", "Favorite 6");

    /** Opens the dialer with no number: never places a call. */
    fun defaultIntent(): Intent? = when (this) {
        CALL -> Intent(Intent.ACTION_DIAL)
        MESSAGE -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
        NAVIGATE -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
        CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        MUSIC -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
        SETTINGS -> Intent(Settings.ACTION_SETTINGS)
        TOOLS, NOTES, FAV1, FAV2, FAV3, FAV4, FAV5, FAV6 -> null
    }

    val isFavorite: Boolean get() = name.startsWith("FAV")

    /** Tools and Notes open a screen inside the launcher until the user picks another app for them. */
    val hasBuiltIn: Boolean get() = this == TOOLS || this == NOTES

    val hasDefault: Boolean get() = !isFavorite
}

enum class ClockFormat(val label: String) { SYSTEM("Auto"), H12("12 h"), H24("24 h") }

enum class Accent(val label: String, val color: Color) {
    CYAN("Cyan", Color(0xFF22D3EE)),
    ICE("Ice", Color(0xFFA5ECFF)),
    TEAL("Teal", Color(0xFF2DD4BF)),
    AZURE("Azure", Color(0xFF38A8F8)),
    AMBER("Amber", Color(0xFFFFB454)),
}

/** All configuration lives in this app's private SharedPreferences. Nothing leaves the device. */
class LauncherPrefs(context: Context) {
    private val sp = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var accent by mutableStateOf(
        Accent.entries.firstOrNull { it.name == sp.getString("accent", null) } ?: Accent.CYAN
    )
        private set
    var clockFormat by mutableStateOf(
        ClockFormat.entries.firstOrNull { it.name == sp.getString("clock", null) } ?: ClockFormat.SYSTEM
    )
        private set
    var haptics by mutableStateOf(sp.getBoolean("haptics", true))
        private set
    var reduceMotion by mutableStateOf(sp.getBoolean("reduce_motion", false))
        private set
    var showcase by mutableStateOf(sp.getBoolean("showcase", false))
        private set
    /** The one-time "swipe left / hold a tile" hint on Home. */
    var hintSeen by mutableStateOf(sp.getBoolean("hint_seen", false))
        private set
    var hidden by mutableStateOf(sp.getStringSet("hidden", emptySet())?.toSet() ?: emptySet())
        private set

    /** Slot -> flattened ComponentName. A missing key means "system default" (or unset). */
    val assigned = mutableStateMapOf<Slot, String>().apply {
        Slot.entries.forEach { s -> sp.getString("slot_${s.key}", null)?.let { put(s, it) } }
    }

    fun updateAccent(v: Accent) { accent = v; sp.edit().putString("accent", v.name).apply() }
    fun updateClockFormat(v: ClockFormat) { clockFormat = v; sp.edit().putString("clock", v.name).apply() }
    fun updateHaptics(v: Boolean) { haptics = v; sp.edit().putBoolean("haptics", v).apply() }
    fun updateReduceMotion(v: Boolean) { reduceMotion = v; sp.edit().putBoolean("reduce_motion", v).apply() }
    fun markHintSeen() { hintSeen = true; sp.edit().putBoolean("hint_seen", true).apply() }
    fun updateShowcase(v: Boolean) { showcase = v; sp.edit().putBoolean("showcase", v).apply() }

    fun assign(slot: Slot, component: String?) {
        if (component == null) assigned.remove(slot) else assigned[slot] = component
        sp.edit().apply {
            if (component == null) remove("slot_${slot.key}") else putString("slot_${slot.key}", component)
        }.apply()
    }

    /** Hiding only removes an app from the default list. The app stays installed and enabled. */
    fun setHidden(component: String, hide: Boolean) {
        hidden = if (hide) hidden + component else hidden - component
        sp.edit().putStringSet("hidden", hidden).apply()
    }
}
