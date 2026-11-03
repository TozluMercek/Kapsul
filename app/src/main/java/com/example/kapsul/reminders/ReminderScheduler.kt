package com.example.kapsul.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.kapsul.MainActivity
import com.example.kapsul.data.Note
import com.example.kapsul.data.ReminderMode
import com.example.kapsul.data.ReminderRepeat
import java.util.Calendar

/** Puts note reminders on the system [AlarmManager]. */
class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    /** Without this (Android 12+) reminders can arrive a few minutes late. */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun schedule(note: Note) {
        val at = note.reminderAt ?: return cancel(note.id)
        set(note.id, at, ReminderMode.from(note.reminderMode), snooze = false)
    }

    /** One-off reminder [minutes] from now that leaves the note's own schedule alone. */
    fun snooze(noteId: Long, mode: ReminderMode, minutes: Int = 10) =
        set(noteId, System.currentTimeMillis() + minutes * 60_000L, mode, snooze = true)

    fun cancel(noteId: Long) {
        alarms.cancel(firePending(noteId, snooze = false, ReminderMode.NOTIFICATION))
        alarms.cancel(firePending(noteId, snooze = true, ReminderMode.NOTIFICATION))
    }

    /** Opens a time capsule at [at]; wakes the device so the "opened" notification is on time. */
    fun scheduleCapsule(noteId: Long, at: Long) {
        val operation = capsulePending(noteId)
        try {
            if (canScheduleExact()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    fun cancelCapsule(noteId: Long) = alarms.cancel(capsulePending(noteId))

    private fun capsulePending(noteId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        noteId.toInt() xor CAPSULE_REQUEST_BIT,
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_CAPSULE_OPEN)
            .putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun set(noteId: Long, at: Long, mode: ReminderMode, snooze: Boolean) {
        val operation = firePending(noteId, snooze, mode)
        try {
            when {
                // Alarms show in the status bar like a clock alarm and are always exact.
                mode == ReminderMode.ALARM && canScheduleExact() -> {
                    val show = PendingIntent.getActivity(
                        context, noteId.toInt(),
                        MainActivity.openNoteIntent(context, noteId),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    alarms.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), operation)
                }
                canScheduleExact() -> alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
                else -> alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            }
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun firePending(noteId: Long, snooze: Boolean, mode: ReminderMode): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
            .putExtra(ReminderReceiver.EXTRA_SNOOZE, snooze)
            .putExtra(ReminderReceiver.EXTRA_MODE, mode.name)
        return PendingIntent.getBroadcast(
            context,
            requestCode(noteId, snooze),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Keeps capsule request codes apart from reminder ones (which are noteId * 2 [+ 1]). */
        private const val CAPSULE_REQUEST_BIT = 0x40000000

        fun requestCode(noteId: Long, snooze: Boolean) = noteId.toInt() * 2 + if (snooze) 1 else 0

        /** The next occurrence strictly after now for a repeating reminder, or null. */
        fun nextOccurrence(at: Long, repeat: ReminderRepeat, now: Long = System.currentTimeMillis()): Long? {
            val field = when (repeat) {
                ReminderRepeat.NONE -> return null
                ReminderRepeat.DAILY -> Calendar.DAY_OF_YEAR
                ReminderRepeat.WEEKLY -> Calendar.WEEK_OF_YEAR
            }
            val cal = Calendar.getInstance().apply { timeInMillis = at }
            do cal.add(field, 1) while (cal.timeInMillis <= now)
            return cal.timeInMillis
        }
    }
}
