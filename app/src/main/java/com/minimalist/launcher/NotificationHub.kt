package com.minimalist.launcher

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat

/** One notification, reduced to what the notification center shows. Held in memory only. */
class Note(
    val key: String,
    val pkg: String,
    val app: String,
    val title: String,
    val text: String,
    val whenMs: Long,
    val intent: PendingIntent?,
    val autoCancel: Boolean,
    val clearable: Boolean,
    val ongoing: Boolean,
)

/**
 * The live list of notifications. It is filled by [NotificationHub] only while the user has granted notification
 * access, lives in memory, and is never written to disk or sent anywhere.
 */
object NotificationStore {
    val items = mutableStateListOf<Note>()
    var connected by mutableStateOf(false)
    internal var hub: NotificationHub? = null

    /** What the badge counts: things the user can act on, not media players and system chores. */
    val count: Int get() = items.count { !it.ongoing }

    fun dismiss(note: Note) { if (note.clearable) hub?.cancelNotification(note.key) }

    fun clearAll() { items.filter { it.clearable }.forEach { hub?.cancelNotification(it.key) } }

    /** Fires the notification's own tap action, as the system shade would. */
    fun open(context: Context, note: Note) {
        val pi = note.intent ?: return
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                val opts = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                pi.send(context, 0, null, null, null, null, opts.toBundle())
            } else {
                pi.send()
            }
            if (note.autoCancel) dismiss(note)
        } catch (_: PendingIntent.CanceledException) {
        }
    }
}

fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Opens the system screen where the user grants or revokes notification access. Nothing is granted by this app. */
fun openNotificationAccessSettings(context: Context) {
    val own = ComponentName(context, NotificationHub::class.java)
    val attempts = buildList {
        if (Build.VERSION.SDK_INT >= 30) {
            add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, own.flattenToString()))
        }
        add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    for (i in attempts) {
        try {
            context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
}

class NotificationHub : NotificationListenerService() {
    override fun onListenerConnected() {
        NotificationStore.hub = this
        NotificationStore.connected = true
        reload()
    }

    override fun onListenerDisconnected() {
        NotificationStore.hub = null
        NotificationStore.connected = false
        NotificationStore.items.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val note = toNote(sbn)
        val items = NotificationStore.items
        val at = items.indexOfFirst { it.key == sbn.key }
        when {
            note == null && at >= 0 -> items.removeAt(at)
            note == null -> return
            at >= 0 -> items[at] = note
            else -> items.add(note)
        }
        sort()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationStore.items.removeAll { it.key == sbn.key }
    }

    private fun reload() {
        val list = try { activeNotifications } catch (_: SecurityException) { null }.orEmpty().mapNotNull(::toNote)
        NotificationStore.items.clear()
        NotificationStore.items.addAll(list)
        sort()
    }

    /** Things to act on first, newest first; ongoing items (players, USB, chores) after them. */
    private fun sort() {
        val sorted = NotificationStore.items.sortedWith(compareBy<Note> { it.ongoing }.thenByDescending { it.whenMs })
        if (sorted != NotificationStore.items.toList()) {
            NotificationStore.items.clear()
            NotificationStore.items.addAll(sorted)
        }
    }

    private fun toNote(sbn: StatusBarNotification): Note? {
        if (sbn.packageName == packageName) return null
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val extras = n.extras
        val secret = n.visibility == Notification.VISIBILITY_SECRET
        val title = if (secret) "" else extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = if (secret) "" else (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) {
            sbn.packageName.substringAfterLast('.')
        }
        return Note(
            key = sbn.key,
            pkg = sbn.packageName,
            app = label,
            title = title,
            text = text,
            whenMs = if (n.`when` > 0) n.`when` else sbn.postTime,
            intent = n.contentIntent,
            autoCancel = n.flags and Notification.FLAG_AUTO_CANCEL != 0,
            clearable = sbn.isClearable,
            ongoing = sbn.isOngoing,
        )
    }
}
