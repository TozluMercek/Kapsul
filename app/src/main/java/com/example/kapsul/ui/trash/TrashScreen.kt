@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kapsul.data.Note
import com.example.kapsul.data.NoteRepository
import com.example.kapsul.ui.KapsulViewModel
import com.example.kapsul.ui.components.plainPreview
import kotlin.math.ceil

/** Deleted notes wait here for 30 days before they are gone for good. */
@Composable
fun TrashScreen(vm: KapsulViewModel, onBack: () -> Unit) {
    val notes by vm.trash.collectAsStateWithLifecycle()
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Çöp kutusu")
                        Text(
                            "Notlar ${NoteRepository.TRASH_DAYS} gün sonra kalıcı olarak silinir",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
                actions = {
                    if (notes.isNotEmpty()) TextButton(onClick = { confirmEmpty = true }) { Text("Boşalt") }
                },
            )
        },
    ) { padding ->
        if (notes.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(120.dp).background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie7Sided.toShape()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("Çöp kutusu boş", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sildiğin notlar burada ${NoteRepository.TRASH_DAYS} gün boyunca saklanır ve geri yüklenebilir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    TrashedNote(
                        note = note,
                        onRestore = { vm.restore(note) },
                        onDelete = { confirmDelete = note },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
            title = { Text("Çöp kutusu boşaltılsın mı?") },
            text = { Text("${notes.size} not ve ekleri kalıcı olarak silinir. Bu geri alınamaz.") },
            confirmButton = { TextButton(onClick = { vm.emptyTrash(); confirmEmpty = false }) { Text("Boşalt") } },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Vazgeç") } },
        )
    }
    confirmDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
            title = { Text("Kalıcı olarak silinsin mi?") },
            text = { Text("“${note.title.ifBlank { "Başlıksız" }}” ve ekleri geri alınamaz şekilde silinir.") },
            confirmButton = { TextButton(onClick = { vm.deleteForever(note); confirmDelete = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun TrashedNote(note: Note, onRestore: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val deletedAt = note.deletedAt ?: System.currentTimeMillis()
    val daysLeft = ceil((deletedAt + NoteRepository.TRASH_DAYS * 86_400_000.0 - System.currentTimeMillis()) / 86_400_000.0)
        .toInt().coerceAtLeast(0)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isPrivate) Icon(Icons.Outlined.Lock, contentDescription = "Kilitli", modifier = Modifier.size(16.dp).padding(end = 4.dp))
                Text(note.title.ifBlank { "Başlıksız" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            if (!note.isPrivate) {
                val preview = plainPreview(note.body, note.title)
                if (preview.isNotBlank()) {
                    Text(preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (daysLeft <= 1) "Yarın silinecek" else "$daysLeft gün sonra silinecek",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (daysLeft <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDelete) { Text("Kalıcı sil") }
                FilledTonalButton(onClick = onRestore) {
                    Icon(Icons.Outlined.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Geri yükle")
                }
            }
        }
    }
}
