package com.minimalist.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.BatteryManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.compose.ui.graphics.toArgb

/**
 * Live wallpaper in the launcher's look. It draws one still frame and only redraws when it becomes visible or the
 * battery tone changes, so it costs nothing while idle. No animation loop, no network, no permissions.
 */
class PocketWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = PocketEngine()

    private inner class PocketEngine : Engine() {
        private var visible = false
        private var width = 0
        private var height = 0
        private val prefs = getSharedPreferences("launcher", Context.MODE_PRIVATE)
        private var percent = 100

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val before = tone()
                percent = levelOf(intent)
                if (tone() != before) draw()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                // Sticky broadcast: seeds the level without waiting for a change.
                val sticky = registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (sticky != null) percent = levelOf(sticky)
                draw()
            } else {
                try { unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.width = width; this.height = height
            draw()
        }

        override fun onDestroy() {
            if (visible) try { unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
        }

        private fun levelOf(i: Intent): Int {
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            return if (level < 0 || scale <= 0) 100 else (level * 100 / scale).coerceIn(0, 100)
        }

        private fun tone() = batteryTone(percent)

        private fun tint(): Int = when (tone()) {
            BatteryTone.CRITICAL -> 0xFFFF4D5E.toInt()
            BatteryTone.MEDIUM -> 0xFFFFD84D.toInt()
            BatteryTone.NORMAL ->
                (Accent.entries.firstOrNull { it.name == prefs.getString("accent", null) } ?: Accent.CYAN).color.toArgb()
        }

        private fun draw() {
            if (!visible || width == 0) return
            val holder = surfaceHolder
            val canvas = try { holder.lockCanvas() } catch (_: Exception) { null } ?: return
            try { paint(canvas) } finally { holder.unlockCanvasAndPost(canvas) }
        }

        private fun paint(c: Canvas) = DomePainter.paint(c, width.toFloat(), height.toFloat(), tint())
    }
}
