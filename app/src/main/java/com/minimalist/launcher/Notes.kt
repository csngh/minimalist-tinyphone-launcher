package com.minimalist.launcher

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

class NoteItem(val id: Long, val text: String, val updated: Long) {
    val title: String get() = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val preview: String get() = text.lineSequence().filter { it.isNotBlank() }.drop(1).joinToString(" ") { it.trim() }
}

/** Notes live in this app's private storage only. Not backed up, not synced, not shared. */
class NotesStore(context: Context) {
    private val sp = context.getSharedPreferences("notes", Context.MODE_PRIVATE)

    var items by mutableStateOf(load())
        private set

    fun find(id: Long): NoteItem? = items.firstOrNull { it.id == id }

    /** Saves [text] for [id]; a blank note is removed instead of stored. */
    fun save(id: Long, text: String) {
        val rest = items.filter { it.id != id }
        items = if (text.isBlank()) rest else (listOf(NoteItem(id, text, System.currentTimeMillis())) + rest)
        persist()
    }

    fun delete(id: Long) { items = items.filter { it.id != id }; persist() }

    private fun persist() {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text).put("updated", it.updated)) }
        sp.edit().putString("notes", arr.toString()).apply()
    }

    private fun load(): List<NoteItem> = try {
        val arr = JSONArray(sp.getString("notes", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it).let { o -> NoteItem(o.getLong("id"), o.getString("text"), o.getLong("updated")) } }
            .sortedByDescending { it.updated }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Stopwatch state that survives the launcher being closed: it stores when it started on the system's elapsed clock,
 * not a running counter. After a reboot that clock restarts, so a stale start is treated as stopped.
 */
class StopwatchState(context: Context) {
    private val sp = context.getSharedPreferences("stopwatch", Context.MODE_PRIVATE)

    private var base by mutableStateOf(sp.getLong("base", 0L))
    private var startedAt by mutableStateOf(sp.getLong("started", -1L).takeIf { it in 0..SystemClock.elapsedRealtime() } ?: -1L)
    var laps by mutableStateOf(sp.getString("laps", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() })
        private set

    val running: Boolean get() = startedAt >= 0

    fun elapsed(now: Long = SystemClock.elapsedRealtime()): Long = base + if (running) now - startedAt else 0L

    fun start() { if (!running) { startedAt = SystemClock.elapsedRealtime(); save() } }

    fun stop() { if (running) { base = elapsed(); startedAt = -1L; save() } }

    fun lap() { if (running) { laps = laps + elapsed(); save() } }

    fun reset() { base = 0L; startedAt = -1L; laps = emptyList(); save() }

    private fun save() {
        sp.edit().putLong("base", base).putLong("started", startedAt).putString("laps", laps.joinToString(",")).apply()
    }
}

/** 1:02.34 or 1:02:03.45: how a stopwatch reads. */
fun formatStopwatch(ms: Long): String {
    val cs = (ms / 10) % 100
    val s = (ms / 1000) % 60
    val m = (ms / 60_000) % 60
    val h = ms / 3_600_000
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
}
