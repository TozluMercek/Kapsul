package com.example.kapsul.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val TR = Locale.forLanguageTag("tr")

/** "şimdi", "12 dk önce", "bugün 14:05", "dün", "4 gün önce", "12 Eki" … */
fun relativeDate(millis: Long, now: Long = System.currentTimeMillis()): String {
    val diffMin = (now - millis) / 60_000
    if (diffMin < 1) return "şimdi"
    if (diffMin < 60) return "$diffMin dk önce"

    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val days = daysBetween(then, today)
    return when {
        days == 0 -> "bugün " + SimpleDateFormat("HH:mm", TR).format(Date(millis))
        days == 1 -> "dün"
        days < 7 -> "$days gün önce"
        then.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            SimpleDateFormat("d MMM", TR).format(Date(millis))
        else -> SimpleDateFormat("d MMM yyyy", TR).format(Date(millis))
    }
}

private fun daysBetween(a: Calendar, b: Calendar): Int {
    fun Calendar.dayStart() = (clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return ((b.dayStart() - a.dayStart()) / 86_400_000L).toInt()
}
