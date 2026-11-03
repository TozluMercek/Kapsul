@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.example.kapsul.ui.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimeInput
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kapsul.data.Note
import com.example.kapsul.data.ReminderMode
import com.example.kapsul.data.ReminderRepeat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val TR = Locale.forLanguageTag("tr")

/** "Bugün 20:00", "Yarın 09:00", "5 Kas Çar 14:30" (+ repeat). */
fun reminderLabel(at: Long, repeat: ReminderRepeat = ReminderRepeat.NONE): String {
    val cal = Calendar.getInstance().apply { timeInMillis = at }
    val today = Calendar.getInstance()
    val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    val time = SimpleDateFormat("HH:mm", TR).format(Date(at))
    val day = when {
        sameDay(cal, today) -> "Bugün"
        sameDay(cal, tomorrow) -> "Yarın"
        else -> SimpleDateFormat("d MMM EEE", TR).format(Date(at))
    }
    val suffix = when (repeat) {
        ReminderRepeat.NONE -> ""
        ReminderRepeat.DAILY -> " · her gün"
        ReminderRepeat.WEEKLY -> " · her hafta"
    }
    return "$day $time$suffix"
}

private fun defaultTime(): Long = Calendar.getInstance().apply {
    add(Calendar.HOUR_OF_DAY, 1)
    set(Calendar.MINUTE, (get(Calendar.MINUTE) / 5) * 5)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun at(dayOffset: Int, hour: Int): Long = Calendar.getInstance().apply {
    add(Calendar.DAY_OF_YEAR, dayOffset)
    set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Pick when and how to be reminded about a note. */
@Composable
fun ReminderSheet(
    note: Note,
    onSave: (at: Long, mode: ReminderMode, repeat: ReminderRepeat) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var at by rememberSaveable { mutableLongStateOf(note.reminderAt ?: defaultTime()) }
    var mode by rememberSaveable { mutableStateOf(ReminderMode.from(note.reminderMode)) }
    var repeat by rememberSaveable { mutableStateOf(ReminderRepeat.from(note.reminderRepeat)) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    val inPast = at <= System.currentTimeMillis() && repeat == ReminderRepeat.NONE
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Saved either way; without the permission Android just won't show it.
        onSave(at, mode, repeat)
    }

    fun save() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else onSave(at, mode, repeat)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Hatırlatma", style = MaterialTheme.typography.headlineSmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                SuggestionChip(onClick = { at = defaultTime() }, label = { Text("1 saat sonra") })
                SuggestionChip(
                    onClick = { at = if (now < 20) at(0, 20) else at(1, 20) },
                    label = { Text(if (now < 20) "Bu akşam 20:00" else "Yarın akşam 20:00") },
                )
                SuggestionChip(onClick = { at = at(1, 9) }, label = { Text("Yarın 09:00") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(SimpleDateFormat("d MMM EEE", TR).format(Date(at)))
                }
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(SimpleDateFormat("HH:mm", TR).format(Date(at)))
                }
            }

            Text("Nasıl hatırlatayım?", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                ToggleButton(
                    checked = mode == ReminderMode.NOTIFICATION,
                    onCheckedChange = { mode = ReminderMode.NOTIFICATION },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bildirim")
                }
                ToggleButton(
                    checked = mode == ReminderMode.ALARM,
                    onCheckedChange = { mode = ReminderMode.ALARM },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Alarm")
                }
            }
            Text(
                if (mode == ReminderMode.ALARM) "Kapatana kadar alarm sesiyle çalar; kilit ekranında tam ekran açılır."
                else "Zamanı gelince sesli bir bildirim gönderir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Tekrar", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ReminderRepeat.NONE to "Yok", ReminderRepeat.DAILY to "Her gün", ReminderRepeat.WEEKLY to "Her hafta")
                    .forEach { (value, label) ->
                        FilterChip(selected = repeat == value, onClick = { repeat = value }, label = { Text(label) })
                    }
            }

            if (inPast) {
                Text("Geçmiş bir zaman seçtin.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.reminderAt != null) {
                    TextButton(onClick = onRemove) { Text("Hatırlatmayı kaldır", color = MaterialTheme.colorScheme.error) }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = ::save,
                    enabled = !inPast,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.height(52.dp),
                ) { Text("Kaydet", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }

    if (pickDate) {
        // DatePicker works in UTC midnight; keep the chosen local time of day.
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val local = Calendar.getInstance().apply { timeInMillis = at }
        utc.clear()
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        val state = rememberDatePickerState(initialSelectedDateMillis = utc.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val p = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = picked }
                        at = Calendar.getInstance().apply {
                            timeInMillis = at
                            set(p.get(Calendar.YEAR), p.get(Calendar.MONTH), p.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis
                    }
                    pickDate = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Vazgeç") } },
        ) { DatePicker(state) }
    }

    if (pickTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = at }
        val state = rememberTimePickerState(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24Hour = true)
        var typing by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Saat seç", modifier = Modifier.weight(1f))
                    // Dial or keyboard entry, as in the Material time picker spec.
                    IconButton(onClick = { typing = !typing }) {
                        Icon(
                            if (typing) Icons.Outlined.Schedule else Icons.Outlined.Keyboard,
                            contentDescription = if (typing) "Kadranla seç" else "Klavyeyle yaz",
                        )
                    }
                }
            },
            text = { if (typing) TimeInput(state) else TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    at = Calendar.getInstance().apply {
                        timeInMillis = at
                        set(Calendar.HOUR_OF_DAY, state.hour); set(Calendar.MINUTE, state.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    pickTime = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Vazgeç") } },
        )
    }
}

/** Missing permissions that would make a reminder late or quiet; empty when all is well. */
fun missingReminderPermissions(context: Context, canScheduleExact: Boolean, mode: ReminderMode): List<Pair<String, Intent>> {
    val missing = mutableListOf<Pair<String, Intent>>()
    val pkg = Uri.parse("package:${context.packageName}")
    if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        missing += "Tam zamanında çalması için “Alarmlar ve hatırlatıcılar” izni" to
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
    }
    if (mode == ReminderMode.ALARM && Build.VERSION.SDK_INT >= 34 &&
        !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    ) {
        missing += "Kilit ekranında tam ekran alarm için izin" to
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
    }
    return missing
}

/** Explains and links to the system settings for each missing permission. */
@Composable
fun ReminderPermissionsDialog(missing: List<Pair<String, Intent>>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Alarm, contentDescription = null) },
        title = { Text("Hatırlatma kaydedildi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tam istediğin gibi çalışması için şu izinleri vermen gerekiyor:")
                missing.forEach { (label, intent) ->
                    TextButton(onClick = { runCatching { context.startActivity(intent) } }) { Text(label) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tamam") } },
    )
}
