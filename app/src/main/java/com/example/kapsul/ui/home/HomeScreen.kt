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
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import com.example.kapsul.data.Folder
import com.example.kapsul.data.SettingsRepository
import com.example.kapsul.ui.folders.FolderNameDialog
import com.example.kapsul.ui.folders.FolderPickerDialog
import com.example.kapsul.ui.folders.KapsulDrawer
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    vm: KapsulViewModel,
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onQuickCapture: (voice: Boolean) -> Unit,
    onAsk: () -> Unit,
    onSettings: () -> Unit,
    onTimeline: () -> Unit,
    onTrash: () -> Unit,
) {
    val folders by vm.folders.collectAsStateWithLifecycle()
    val selectedFolder by vm.selectedFolder.collectAsStateWithLifecycle()
    val trash by vm.trash.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    var folderDialog by remember { mutableStateOf<FolderDialog?>(null) }
    var moveDialog by remember { mutableStateOf(false) }
    val clipboardOffer = rememberClipboardOffer(vm)
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

    BackHandler(drawerState.isOpen) { drawerScope.launch { drawerState.close() } }
    fun closeDrawer(then: () -> Unit = {}) {
        drawerScope.launch { drawerState.close() }
        then()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KapsulDrawer(
                folders = folders,
                selectedFolder = selectedFolder,
                trashCount = trash.size,
                onAllNotes = { closeDrawer { vm.selectFolder(null) } },
                onFolder = { folder -> closeDrawer { vm.selectFolder(folder.id) } },
                onTimeline = { closeDrawer(onTimeline) },
                onTrash = { closeDrawer(onTrash) },
                onCreateFolder = { parent -> folderDialog = FolderDialog.Create(parent) },
                onRename = { folderDialog = FolderDialog.Rename(it) },
                onDelete = { folderDialog = FolderDialog.Delete(it) },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(folders.firstOrNull { it.id == selectedFolder }?.name ?: "Kapsül") },
                    subtitle = {
                        val count = if (selectedFolder != null) notes?.size ?: 0 else allNotes?.size ?: 0
                        Text("$count anı")
                    },
                    navigationIcon = {
                        IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Menü")
                        }
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
                    clipboardOffer.text?.let { text ->
                        item(span = StaggeredGridItemSpan.FullLine, key = "clipboard") {
                            ClipboardSuggestion(
                                text = text,
                                onAdd = { vm.saveClipboard(text); clipboardOffer.dismiss() },
                                onDismiss = clipboardOffer::dismiss,
                                modifier = Modifier.animateItem(),
                            )
                        }
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
                        onMove = { moveDialog = true },
                        onDelete = { confirmDelete = true },
                    )
                }
            }
        }
    }

    if (moveDialog) {
        FolderPickerDialog(
            folders = folders,
            current = selection.singleOrNull()?.let { id -> notes?.firstOrNull { it.id == id }?.folderId },
            onPick = { folderId ->
                vm.moveToFolder(selection, folderId)
                selection = emptySet()
                moveDialog = false
            },
            onDismiss = { moveDialog = false },
        )
    }

    when (val dialog = folderDialog) {
        is FolderDialog.Create -> FolderNameDialog(
            title = if (dialog.parentId == null) "Yeni klasör" else "Yeni alt klasör",
            initial = "",
            onConfirm = { vm.createFolder(it, dialog.parentId); folderDialog = null },
            onDismiss = { folderDialog = null },
        )
        is FolderDialog.Rename -> FolderNameDialog(
            title = "Klasörü yeniden adlandır",
            initial = dialog.folder.name,
            onConfirm = { vm.renameFolder(dialog.folder, it); folderDialog = null },
            onDismiss = { folderDialog = null },
        )
        is FolderDialog.Delete -> AlertDialog(
            onDismissRequest = { folderDialog = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("“${dialog.folder.name}” silinsin mi?") },
            text = { Text("Yalnızca klasör silinir; içindeki notlar ve alt klasörler bir üst seviyeye taşınır.") },
            confirmButton = { TextButton(onClick = { vm.deleteFolder(dialog.folder); folderDialog = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { folderDialog = null }) { Text("Vazgeç") } },
        )
        null -> Unit
    }

    if (confirmDelete) {
        val count = selection.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(if (count == 1) "Not çöp kutusuna taşınsın mı?" else "$count not çöp kutusuna taşınsın mı?") },
            text = { Text("30 gün boyunca çöp kutusundan geri yükleyebilirsin.") },
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

private sealed interface FolderDialog {
    data class Create(val parentId: Long?) : FolderDialog
    data class Rename(val folder: Folder) : FolderDialog
    data class Delete(val folder: Folder) : FolderDialog
}

/** Text waiting on the clipboard, offered once per copy. */
private class ClipboardOffer(private val settings: SettingsRepository) {
    var text by mutableStateOf<String?>(null)
        private set

    fun read(context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val description = clipboard.primaryClipDescription ?: return
        if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) && !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) return
        // Checking the description is silent; only reading the clip shows Android's toast.
        val stamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) description.timestamp else 0L
        if (stamp != 0L && stamp == settings.lastClipboardSeen) return
        settings.lastClipboardSeen = stamp
        val clip = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
        if (!clip.isNullOrBlank() && clip.length >= 3) text = clip
    }

    fun dismiss() {
        text = null
    }
}

@Composable
private fun rememberClipboardOffer(vm: KapsulViewModel): ClipboardOffer {
    val context = LocalContext.current
    val settings by vm.settings.settings.collectAsStateWithLifecycle()
    val offer = remember { ClipboardOffer(vm.settings) }
    val window = LocalWindowInfo.current
    // Android 10+ only lets the focused app read the clipboard.
    LaunchedEffect(settings.clipboardSuggestions) {
        if (!settings.clipboardSuggestions) {
            offer.dismiss()
            return@LaunchedEffect
        }
        snapshotFlow { window.isWindowFocused }.collect { focused -> if (focused) offer.read(context) }
    }
    return offer
}

@Composable
private fun ClipboardSuggestion(text: String, onAdd: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Panodaki metin not olarak eklensin mi?", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.align(Alignment.End).padding(top = 4.dp)) {
                TextButton(onClick = onDismiss) { Text("Yoksay") }
                FilledTonalButton(onClick = onAdd) { Text("Not olarak ekle") }
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, onCancel: () -> Unit, onMove: () -> Unit, onDelete: () -> Unit) {
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
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = onMove) {
                Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = "Klasöre taşı")
            }
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
