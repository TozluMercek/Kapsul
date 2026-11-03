package com.example.kapsul

import com.example.kapsul.data.ReminderRepeat
import com.example.kapsul.reminders.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSchedulerTest {
    private val hour = 3_600_000L
    private val day = 24 * hour

    @Test
    fun oneOffReminderHasNoNextOccurrence() {
        assertNull(ReminderScheduler.nextOccurrence(1_000L, ReminderRepeat.NONE, now = 2_000L))
    }

    @Test
    fun dailyReminderMovesToTheNextDay() {
        val at = 10 * day + 9 * hour
        assertEquals(at + day, ReminderScheduler.nextOccurrence(at, ReminderRepeat.DAILY, now = at))
    }

    @Test
    fun missedDailyRemindersSkipToTheFuture() {
        val at = 10 * day + 9 * hour
        val now = at + 3 * day + hour
        assertEquals(at + 4 * day, ReminderScheduler.nextOccurrence(at, ReminderRepeat.DAILY, now = now))
    }

    @Test
    fun weeklyReminderMovesOneWeek() {
        val at = 10 * day
        assertEquals(at + 7 * day, ReminderScheduler.nextOccurrence(at, ReminderRepeat.WEEKLY, now = at + hour))
    }
}
