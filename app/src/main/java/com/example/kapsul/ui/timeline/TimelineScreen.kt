@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kapsul.data.Note
import com.example.kapsul.ui.KapsulViewModel
import com.example.kapsul.ui.components.plainPreview
import com.example.kapsul.ui.components.tagColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val TR = Locale.forLanguageTag("tr")

private data class Month(val key: Int, val label: String, val days: List<Day>, val count: Int, val topTags: List<String>)
private data class Day(val key: Int, val label: String, val notes: List<Note>)

/** Notes by month and day: "what was I busy with back then?" */
private fun group(notes: List<Note>): List<Month> {
    val cal = Calendar.getInstance()
    fun keyOf(t: Long, field: Int): Int {
        cal.timeInMillis = t
        return when (field) {
            Calendar.MONTH -> cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
            else -> cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        }
    }
    return notes.sortedByDescending { it.createdAt }
        .groupBy { keyOf(it.createdAt, Calendar.MONTH) }
        .map { (monthKey, monthNotes) ->
            val days = monthNotes.groupBy { keyOf(it.createdAt, Calendar.DAY_OF_YEAR) }.map { (dayKey, dayNotes) ->
                Day(dayKey, SimpleDateFormat("d MMMM, EEEE", TR).format(Date(dayNotes.first().createdAt)), dayNotes)
            }
            val tags = monthNotes.filterNot { it.isPrivate }.flatMap { it.tags }
                .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3).map { it.key }
            Month(
                monthKey,
                SimpleDateFormat("MMMM yyyy", TR).format(Date(monthNotes.first().createdAt))
                    .replaceFirstChar { it.titlecase(TR) },
                days,
                monthNotes.size,
                tags,
            )
        }
}

@Composable
fun TimelineScreen(vm: KapsulViewModel, onBack: () -> Unit, onOpenNote: (Long) -> Unit) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val months = remember(notes) { notes?.let(::group) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zaman çizelgesi") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
            )
        },
    ) { padding ->
        val list = months
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Henüz not yok; ilk notunla zaman çizelgen başlar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            ) {
                list.forEach { month ->
                    item(key = "m${month.key}") { MonthHeader(month) }
                    month.days.forEach { day ->
                        item(key = "d${day.key}") { DayRow(day, onOpenNote) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: Month) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondary, MaterialShapes.Cookie6Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Text("${month.count}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondary)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(month.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    buildString {
                        append("${month.count} not")
                        if (month.topTags.isNotEmpty()) append(" · " + month.topTags.joinToString(" ") { "#$it" })
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** One day on the vertical line: a dot, the date, and that day's notes. */
@Composable
private fun DayRow(day: Day, onOpenNote: (Long) -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val dot = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(Modifier.width(28.dp).fillMaxHeight()) {
            drawLine(line, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2.dp.toPx())
            drawCircle(dot, 6.dp.toPx(), Offset(size.width / 2, 22.dp.toPx()))
        }
        Column(Modifier.padding(start = 8.dp, bottom = 16.dp)) {
            Text(day.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                day.notes.forEach { TimelineNote(it) { onOpenNote(it.id) } }
            }
        }
    }
}

@Composable
private fun TimelineNote(note: Note, onClick: () -> Unit) {
    val colors = tagColors(note.tags.firstOrNull())
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SimpleDateFormat("HH:mm", TR).format(Date(note.createdAt)),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 8.dp),
                )
                when {
                    note.isSealed -> Icon(Icons.Outlined.HourglassTop, contentDescription = "Zaman kapsülü", modifier = Modifier.size(14.dp))
                    note.locked -> Icon(Icons.Outlined.Lock, contentDescription = "Kilitli", modifier = Modifier.size(14.dp))
                }
                if (note.reminderAt != null) {
                    Icon(Icons.Outlined.Alarm, contentDescription = "Hatırlatma var", modifier = Modifier.size(14.dp).padding(start = 2.dp))
                }
                Text(
                    note.title.ifBlank { "Başlıksız" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (!note.isPrivate) {
                val preview = plainPreview(note.body, note.title)
                if (preview.isNotBlank()) {
                    Text(preview, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
