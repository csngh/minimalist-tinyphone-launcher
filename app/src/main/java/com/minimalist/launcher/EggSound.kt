package com.minimalist.launcher

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sound for the clock easter egg, synthesised on the fly (no audio files, no permission). A motor whine and friction
 * hiss follow [level], the ring's speed, so the pitch rises as it spins up and falls as it winds down; sparks add
 * crackle. [boom] marks the circle opening out, with a riser that climbs until the credits appear, and [finale] is
 * the big chord, impact and crash that lands with them.
 *
 * It plays as a sonification sound, so it follows the system's sound volume and is silent in silent or vibrate mode.
 * It never takes audio focus, so music keeps playing underneath.
 */
class EggSound {
    private val sampleRate = 22_050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    /** Ring speed, 0 to 1. Written from the UI thread, read by the audio thread. */
    @Volatile var level = 0f
    @Volatile private var motorOn = false
    @Volatile private var boomAt = -1L
    @Volatile private var finaleAt = -1L
    @Volatile private var samplesWritten = 0L

    fun begin() { motorOn = true; ensureRunning() }
    fun end() { motorOn = false }
    fun boom() { boomAt = samplesWritten; ensureRunning() }
    fun finale() { finaleAt = samplesWritten; ensureRunning() }
    fun release() { motorOn = false; scope.cancel() }

    private fun ensureRunning() {
        if (job?.isActive == true) return
        job = scope.launch { play() }
    }

    private fun play() {
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBytes <= 0) return
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBytes, 2048))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }
        try {
            track.setVolume(0.85f)
            track.play()
            val rnd = Random(System.nanoTime())
            val buf = ShortArray(512)
            var amp = 0f
            var freq = 60f
            var phase = 0.0
            var prevNoise = 0f
            var low = 0f
            var click = 0f
            var quiet = 0
            var risePhase = 0.0
            var crashPrev = 0f
            while (true) {
                for (i in buf.indices) {
                    val sp = level.coerceIn(0f, 1f)
                    val targetAmp = if (motorOn) 0.07f + 0.26f * sp else 0f
                    amp += (targetAmp - amp) * 0.002f
                    freq += ((60f + 480f * Math.pow(sp.toDouble(), 1.3).toFloat()) - freq) * 0.0015f
                    phase += 2.0 * PI * freq / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                    val saw = (phase / PI - 1.0).toFloat()
                    val motor = 0.6f * sin(phase).toFloat() + 0.25f * sin(2.0 * phase + 0.3).toFloat() + 0.15f * saw

                    val noise = rnd.nextFloat() * 2f - 1f
                    val hiss = (noise - prevNoise * 0.9f) * 0.5f
                    prevNoise = noise
                    var x = amp * motor + amp * sp * sp * 0.8f * hiss

                    if (motorOn && rnd.nextFloat() < 0.0004f + 0.003f * sp * sp * sp) click = (rnd.nextFloat() * 2f - 1f) * 0.5f
                    x += click * sp
                    click *= 0.85f

                    val now = samplesWritten + i
                    if (boomAt >= 0) {
                        val t = (now - boomAt).toFloat() / sampleRate
                        if (t < 2.5f) {
                            val sweep = 40f + 110f * exp(-t * 3f)
                            x += sin(2.0 * PI * sweep * t).toFloat() * exp(-t * 2.2f) * 0.55f
                            low += (noise - low) * (0.02f + 0.25f * (t / 1.6f).coerceAtMost(1f))   // noise brightens as it opens out
                            val swell = if (t < 1.6f) sin(PI * t / 1.6).toFloat().let { it * it } else 0f
                            x += low * swell * 1.6f
                            // Riser: a tone climbing and brightening until the credits land at 1.5 s, then cut.
                            if (t < 1.5f) {
                                val u = t / 1.5f
                                risePhase += 2.0 * PI * (180f + 1900f * u * u) / sampleRate
                                val g = u * u * 0.30f
                                x += (sin(risePhase) + 0.5 * sin(2.0 * risePhase) + 0.25 * sin(3.0 * risePhase)).toFloat() * g
                            }
                        } else boomAt = -1
                    }
                    if (finaleAt >= 0) {
                        val t = (now - finaleAt).toFloat() / sampleRate
                        if (t < 5f) {
                            // Impact: a deep thump.
                            x += sin(2.0 * PI * (38f + 40f * exp(-t * 5f)) * t).toFloat() * exp(-t * 2.4f) * 0.9f
                            // Crash: bright noise that rings out.
                            val crash = (noise - crashPrev * 0.95f)
                            crashPrev = noise
                            x += crash * exp(-t * 1.5f) * 0.55f
                            // A wide, open D major chord, swelling in over 50 ms and fading slowly.
                            val attack = (t / 0.05f).coerceAtMost(1f)
                            val env = attack * exp(-t * 0.75f) * 0.075f
                            for (f in CHORD) {
                                val w = 2.0 * PI * f * t
                                x += (sin(w) + 0.4 * sin(2.0 * w) + 0.2 * sin(3.0 * w)).toFloat() * env
                                x += sin(2.0 * PI * f * 1.004 * t).toFloat() * env * 0.5f   // detuned copy: shimmer
                            }
                            // Sparkle: a quick run up the scale on top.
                            for ((k, f) in SPARKLE.withIndex()) {
                                val ts = 0.12f + k * 0.085f
                                if (t >= ts) x += sin(2.0 * PI * f * (t - ts)).toFloat() * exp(-(t - ts) * 8f) * 0.13f
                            }
                        } else finaleAt = -1
                    }
                    x = x / (1f + abs(x))                       // soft limit, so peaks never clip
                    buf[i] = (x * 30_000f).toInt().toShort()
                }
                samplesWritten += buf.size
                if (track.write(buf, 0, buf.size) < 0) break
                // Stop once everything has faded and nothing is queued.
                if (!motorOn && amp < 0.002f && boomAt < 0 && finaleAt < 0) { if (++quiet > 20) break } else quiet = 0
            }
            track.stop()
        } catch (_: Exception) {
        } finally {
            track.release()
        }
    }
}

private val CHORD = doubleArrayOf(73.42, 146.83, 220.0, 293.66, 369.99, 440.0, 587.33, 739.99, 880.0)
private val SPARKLE = doubleArrayOf(1174.66, 1479.98, 1760.0, 2093.0, 2349.32, 2959.96, 3520.0, 4698.64)
