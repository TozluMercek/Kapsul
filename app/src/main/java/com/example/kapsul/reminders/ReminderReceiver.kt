package com.example.kapsul.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.kapsul.KapsulApplication
import com.example.kapsul.capsule.TimeCapsules
import com.example.kapsul.data.ReminderMode
import com.example.kapsul.data.ReminderRepeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires reminders, and handles the notification's "Ertele" / "Tamam" buttons. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as KapsulApplication
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
        if (noteId < 0) return
        val mode = ReminderMode.from(intent.getStringExtra(EXTRA_MODE))

        when (intent.action) {
            ACTION_SNOOZE -> {
                ReminderNotifications.dismiss(context, noteId)
                app.reminders.snooze(noteId, mode)
            }
            ACTION_DISMISS -> ReminderNotifications.dismiss(context, noteId)
            ACTION_CAPSULE_OPEN -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val note = app.repository.get(noteId) ?: return@launch
                        TimeCapsules.openIfDue(app.repository, note)?.let { ReminderNotifications.showCapsuleOpened(context, it) }
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_FIRE -> {
                val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val note = app.repository.get(noteId) ?: return@launch
                        ReminderNotifications.show(context, note, if (snooze) mode else ReminderMode.from(note.reminderMode))
                        if (!snooze) {
                            // Move a repeating reminder forward; a one-off one is done.
                            val next = note.reminderAt?.let {
                                ReminderScheduler.nextOccurrence(it, ReminderRepeat.from(note.reminderRepeat))
                            }
                            val updated = if (next != null) note.copy(reminderAt = next)
                            else note.copy(reminderAt = null, reminderMode = null, reminderRepeat = null)
                            app.repository.save(updated)
                            if (next != null) app.reminders.schedule(updated)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.example.kapsul.reminder.FIRE"
        const val ACTION_SNOOZE = "com.example.kapsul.reminder.SNOOZE"
        const val ACTION_DISMISS = "com.example.kapsul.reminder.DISMISS"
        const val ACTION_CAPSULE_OPEN = "com.example.kapsul.capsule.OPEN"
        const val EXTRA_NOTE_ID = "noteId"
        const val EXTRA_SNOOZE = "snooze"
        const val EXTRA_MODE = "mode"
    }
}

/** Alarms don't survive a reboot; put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as KapsulApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                app.repository.withReminders().forEach { note ->
                    val at = note.reminderAt ?: return@forEach
                    val next = when {
                        at > now -> at
                        else -> ReminderScheduler.nextOccurrence(at, ReminderRepeat.from(note.reminderRepeat))
                            ?: (now + 5_000) // missed one-off reminder: show it right away
                    }
                    val updated = note.copy(reminderAt = next)
                    if (next != at) app.repository.save(updated)
                    app.reminders.schedule(updated)
                }
                app.repository.sealedCapsules().forEach { note ->
                    val opened = TimeCapsules.openIfDue(app.repository, note, now)
                    if (opened != null) ReminderNotifications.showCapsuleOpened(context, opened)
                    else note.capsuleUntil?.let { app.reminders.scheduleCapsule(note.id, it) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
