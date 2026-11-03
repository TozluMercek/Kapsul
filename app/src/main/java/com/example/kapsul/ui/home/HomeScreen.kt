@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kapsul.ui.KapsulViewModel
import com.example.kapsul.ui.components.NoteCard

@Composable
fun HomeScreen(
    vm: KapsulViewModel,
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onQuickCapture: (voice: Boolean) -> Unit,
    onAsk: () -> Unit,
    onSettings: () -> Unit,
) {
    val notes by vm.visibleNotes.collectAsStateWithLifecycle()
    val allNotes by vm.notes.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selectedTag by vm.selectedTag.collectAsStateWithLifecycle()
    val attachments by vm.attachmentsByNote.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(fabExpanded) { fabExpanded = false }

    // Long-press starts selection; while selecting, taps add/remove notes.
    var selection by remember { mutableStateOf(emptySet<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selecting = selection.isNotEmpty()
    BackHandler(selecting) { selection = emptySet() }
    fun toggle(id: Long) {
        selection = if (id in selection) selection - id else selection + id
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Kapsül") },
                subtitle = {
                    val count = allNotes?.size ?: 0
                    Text("$count anı")
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Ayarlar")
                    }
                    FilledTonalIconButton(
                        onClick = onAsk,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Hafızana sor")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!selecting) {
                CaptureFabMenu(
                    expanded = fabExpanded,
                    onExpandedChange = { fabExpanded = it },
                    onQuickNote = { fabExpanded = false; onQuickCapture(false) },
                    onVoiceNote = { fabExpanded = false; onQuickCapture(true) },
                    onNewNote = { fabExpanded = false; onNewNote() },
                    onAsk = { fabExpanded = false; onAsk() },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 120.dp,
                ),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = StaggeredGridItemSpan.FullLine, key = "search") {
                    SearchField(query, vm::setQuery)
                }
                if (tags.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "tags") {
                        TagFilterRow(tags, selectedTag, vm::selectTag)
                    }
                }
                val current = notes
                when {
                    current == null -> item(span = StaggeredGridItemSpan.FullLine, key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator(Modifier.size(64.dp))
                        }
                    }
                    current.isEmpty() -> item(span = StaggeredGridItemSpan.FullLine, key = "empty") {
                        EmptyState(searching = query.isNotBlank() || selectedTag != null)
                    }
                    else -> items(current, key = { it.id }) { note ->
                        NoteCard(
                            note,
                            onClick = { if (selecting) toggle(note.id) else onOpenNote(note.id) },
                            onLongClick = { toggle(note.id) },
                            selected = note.id in selection,
                            modifier = Modifier.animateItem(),
                            attachments = attachments[note.id].orEmpty(),
                            fileOf = vm.store::file,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = selecting,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                SelectionBar(
                    count = selection.size,
                    onCancel = { selection = emptySet() },
                    onDelete = { confirmDelete = true },
                )
            }
        }
    }

    if (confirmDelete) {
        val count = selection.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(if (count == 1) "Not silinsin mi?" else "$count not silinsin mi?") },
            text = { Text("Ekleri ve bağlantılarıyla birlikte cihazından kalıcı olarak silinir.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteNotes(selection)
                    selection = emptySet()
                    confirmDelete = false
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun SelectionBar(count: Int, onCancel: () -> Unit, onDelete: () -> Unit) {
    HorizontalFloatingToolbar(expanded = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("İptal")
            }
            Text(
                "$count seçildi",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Button(
                onClick = onDelete,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sil")
            }
        }
    }
}

@Composable
private fun CaptureFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onQuickNote: () -> Unit,
    onVoiceNote: () -> Unit,
    onNewNote: () -> Unit,
    onAsk: () -> Unit,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(checked = expanded, onCheckedChange = onExpandedChange) {
                val icon by remember {
                    derivedStateOf { if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add }
                }
                Icon(
                    icon,
                    contentDescription = if (expanded) "Menüyü kapat" else "Yeni anı",
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = onQuickNote,
            text = { Text("Hızlı not") },
            icon = { Icon(Icons.Outlined.Bolt, contentDescription = null) },
        )
        FloatingActionButtonMenuItem(
            onClick = onVoiceNote,
            text = { Text("Sesli not") },
            icon = { Icon(Icons.Outlined.Mic, contentDescription = null) },
        )
        FloatingActionButtonMenuItem(
            onClick = onNewNote,
            text = { Text("Uzun not") },
            icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
        )
        FloatingActionButtonMenuItem(
            onClick = onAsk,
            text = { Text("Hafızana sor") },
            icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Hafızanda ara…") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                androidx.compose.material3.IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Aramayı temizle")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    )
}

@Composable
private fun TagFilterRow(tags: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Tümü") },
                shape = CircleShape,
            )
        }
        items(tags, key = { it }) { tag ->
            FilterChip(
                selected = selected == tag,
                onClick = { onSelect(tag) },
                label = { Text("#$tag") },
                shape = CircleShape,
            )
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (searching) Icons.Outlined.Search else Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (searching) "Eşleşen anı yok" else "Kapsülün boş",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (searching) "Farklı kelimeler dene ya da filtreyi kaldır."
            else "Aklındakini bırak. Başlık ve etiketleri Kapsül atar, her şey cihazında kalır.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
