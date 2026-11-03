package com.example.kapsul.ui.attachments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Create or edit a sticky note: a short text on a colored square. */
@Composable
fun StickyNoteDialog(
    initialText: String,
    initialColor: Int,
    onConfirm: (text: String, color: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var color by rememberSaveable { mutableIntStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yapışkan not") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Kısa bir hatırlatma…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    textStyle = MaterialTheme.typography.titleMedium,
                    shape = MaterialTheme.shapes.small,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(color),
                        unfocusedContainerColor = Color(color),
                        focusedTextColor = Color(0xFF2B2B2B),
                        unfocusedTextColor = Color(0xFF2B2B2B),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF2B2B2B),
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StickyColors.forEach { c ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(Color(c), CircleShape)
                                .border(
                                    if (c == color) 3.dp else 1.dp,
                                    if (c == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape,
                                )
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c == color) Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF2B2B2B), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim(), color) }) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

/** Choose table size; the table is inserted as Markdown and rendered as a grid. */
@Composable
fun TableDialog(onConfirm: (rows: Int, columns: Int) -> Unit, onDismiss: () -> Unit) {
    var rows by rememberSaveable { mutableIntStateOf(3) }
    var columns by rememberSaveable { mutableIntStateOf(3) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tablo ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper("Satır", rows, 1..12) { rows = it }
                Stepper("Sütun", columns, 1..6) { columns = it }
                Spacer(Modifier.height(4.dp))
                // Live preview of the grid shape.
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(rows + 1) { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(columns) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .background(
                                            if (r == 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            MaterialTheme.shapes.extraSmall,
                                        )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(rows, columns) }) { Text("Ekle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
            Icon(Icons.Filled.Remove, contentDescription = "$label azalt")
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.width(40.dp).padding(horizontal = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
            Icon(Icons.Filled.Add, contentDescription = "$label artır")
        }
    }
}

/** Markdown for an empty table with a header row. */
fun markdownTable(rows: Int, columns: Int): String = buildString {
    append((1..columns).joinToString(" | ", "| ", " |") { "Sütun $it" }).append('\n')
    append((1..columns).joinToString(" | ", "| ", " |") { "---" }).append('\n')
    repeat(rows) { append((1..columns).joinToString(" | ", "| ", " |") { "  " }).append('\n') }
}
