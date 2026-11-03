package com.example.kapsul.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.kapsul.MainActivity
import com.example.kapsul.R
import com.example.kapsul.data.Note
import com.example.kapsul.data.ReminderMode

/** Notification channels and the reminder / alarm notifications themselves. */
object ReminderNotifications {
    private const val CHANNEL_REMINDERS = "reminders"
    private const val CHANNEL_ALARMS = "alarms"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Hatırlatmalar", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notlar için bildirim hatırlatmaları"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARMS, "Alarmlar", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Kapatılana kadar çalan not alarmları"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            }
        )
    }

    fun notificationId(noteId: Long) = noteId.toInt()

    fun show(context: Context, note: Note, mode: ReminderMode) {
        val id = notificationId(note.id)
        val open = PendingIntent.getActivity(
            context, id, MainActivity.openNoteIntent(context, note.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = note.title.ifBlank { "Kapsül hatırlatması" }
        val text = note.body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && it != note.title }
            ?: "Notunu açmak için dokun"

        val builder = NotificationCompat.Builder(context, if (mode == ReminderMode.ALARM) CHANNEL_ALARMS else CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Ertele (10 dk)", action(context, note.id, ReminderReceiver.ACTION_SNOOZE, mode))
            .addAction(0, "Tamam", action(context, note.id, ReminderReceiver.ACTION_DISMISS, mode))

        if (mode == ReminderMode.ALARM) {
            val fullScreen = PendingIntent.getActivity(
                context, id, AlarmActivity.intent(context, note.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setFullScreenIntent(fullScreen, true)
        } else {
            builder
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        }

        val notification = builder.build()
        // Alarm sound repeats until the user reacts.
        if (mode == ReminderMode.ALARM) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        runCatching { context.getSystemService(NotificationManager::class.java).notify(id, notification) }
    }

    fun dismiss(context: Context, noteId: Long) =
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(noteId))

    private fun action(context: Context, noteId: Long, action: String, mode: ReminderMode): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            notificationId(noteId) * 4 + if (action == ReminderReceiver.ACTION_SNOOZE) 2 else 3,
            Intent(context, ReminderReceiver::class.java)
                .setAction(action)
                .putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
                .putExtra(ReminderReceiver.EXTRA_MODE, mode.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
