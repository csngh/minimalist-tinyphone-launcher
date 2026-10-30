package com.minimalist.launcher

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Memo(val id: Long, val durationMs: Long)

/**
 * Voice memos, recorded with the microphone only while the Voice tab is open and the screen is on, and kept as AAC
 * files in this app's private storage. Not backed up, not shared, not transcribed (that would need the internet).
 */
class VoiceMemos(private val context: Context) {
    private val sp = context.getSharedPreferences("voice", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "memos").apply { mkdirs() }

    var items by mutableStateOf(load())
        private set
    var recording by mutableStateOf(false)
        private set
    var startedAt by mutableLongStateOf(0L)
        private set
    /** The memo being played or paused, or -1. */
    var playingId by mutableLongStateOf(-1L)
        private set
    var playing by mutableStateOf(false)
        private set

    private var recorder: MediaRecorder? = null
    private var recordingId = 0L
    private var player: MediaPlayer? = null

    private fun file(id: Long) = File(dir, "$id.m4a")

    /** Returns false if the microphone could not be opened (another app is using it, for instance). */
    fun startRecording(): Boolean {
        if (recording) return true
        stopPlayback()
        val id = System.currentTimeMillis()
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file(id).absolutePath)
            r.prepare()
            r.start()
            recorder = r; recordingId = id; startedAt = SystemClock.elapsedRealtime(); recording = true
            true
        } catch (_: Exception) {
            r.release(); file(id).delete()
            false
        }
    }

    /** Stops and saves. A recording under a second is discarded as an accidental tap. */
    fun stopRecording() {
        val r = recorder ?: return
        val ms = SystemClock.elapsedRealtime() - startedAt
        recorder = null; recording = false
        val ok = try { r.stop(); true } catch (_: RuntimeException) { false }
        r.release()
        if (ok && ms >= 1000) {
            items = listOf(Memo(recordingId, ms)) + items
            persist()
        } else {
            file(recordingId).delete()
        }
    }

    /** How loud it has been since the last call, 0..1. Drives the live waveform; nothing is stored. */
    fun level(): Float = try { (recorder?.maxAmplitude ?: 0) / 32767f } catch (_: Exception) { 0f }

    fun togglePlay(id: Long) {
        if (playingId == id) {
            player?.let { p -> if (playing) { p.pause(); playing = false } else { p.start(); playing = true } }
            return
        }
        stopPlayback()
        val p = MediaPlayer()
        try {
            p.setDataSource(file(id).absolutePath)
            p.prepare()
            p.setOnCompletionListener { stopPlayback() }
            p.start()
            player = p; playingId = id; playing = true
        } catch (_: Exception) {
            p.release()
        }
    }

    fun position(): Long = try { player?.currentPosition?.toLong() ?: 0L } catch (_: IllegalStateException) { 0L }

    fun stopPlayback() {
        player?.let { try { it.stop() } catch (_: IllegalStateException) {}; it.release() }
        player = null; playingId = -1L; playing = false
    }

    fun delete(id: Long) {
        if (playingId == id) stopPlayback()
        file(id).delete()
        items = items.filter { it.id != id }
        persist()
    }

    /** Ends any recording (saving it) and any playback. Called when the screen is left or goes to the background. */
    fun release() { stopRecording(); stopPlayback() }

    private fun persist() {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("ms", it.durationMs)) }
        sp.edit().putString("memos", arr.toString()).apply()
    }

    private fun load(): List<Memo> = try {
        val arr = JSONArray(sp.getString("memos", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it).let { o -> Memo(o.getLong("id"), o.getLong("ms")) } }
            .filter { file(it.id).exists() }
            .sortedByDescending { it.id }
    } catch (_: Exception) {
        emptyList()
    }
}

/** 0:07 or 12:34: how long a memo is. */
fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
