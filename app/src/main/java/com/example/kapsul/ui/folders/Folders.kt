package com.example.kapsul.ui.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.example.kapsul.data.Folder

/** Folders in tree order, each with its depth (0 = top level). */
fun flattenFolders(folders: List<Folder>): List<Pair<Folder, Int>> {
    val children = folders.groupBy { it.parentId }
    val known = folders.map { it.id }.toSet()
    val result = mutableListOf<Pair<Folder, Int>>()
    fun visit(parent: Long?, depth: Int) {
        children[parent].orEmpty().sortedBy { it.name.lowercase() }.forEach {
            result += it to depth
            visit(it.id, depth + 1)
        }
    }
    visit(null, 0)
    // Orphans (parent deleted elsewhere) still show at the top level.
    folders.filter { it.parentId != null && it.parentId !in known }.forEach { result += it to 0; visit(it.id, 1) }
    return result
}

/** Side drawer: all notes, timeline, trash and the folder tree. */
@Composable
fun KapsulDrawer(
    folders: List<Folder>,
    selectedFolder: Long?,
    trashCount: Int,
    onAllNotes: () -> Unit,
    onFolder: (Folder) -> Unit,
    onTimeline: () -> Unit,
    onTrash: () -> Unit,
    onCreateFolder: (parentId: Long?) -> Unit,
    onRename: (Folder) -> Unit,
    onDelete: (Folder) -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            Text(
                "Kapsül",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp),
            )
            NavigationDrawerItem(
                label = { Text("Tüm notlar") },
                icon = { Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null) },
                selected = selectedFolder == null,
                onClick = onAllNotes,
            )
            NavigationDrawerItem(
                label = { Text("Zaman çizelgesi") },
                icon = { Icon(Icons.Outlined.Timeline, contentDescription = null) },
                selected = false,
                onClick = onTimeline,
            )
            NavigationDrawerItem(
                label = { Text("Çöp kutusu") },
                icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                badge = { if (trashCount > 0) Badge { Text("$trashCount") } },
                selected = false,
                onClick = onTrash,
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                Text("Klasörler", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                IconButton(onClick = { onCreateFolder(null) }) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = "Yeni klasör")
                }
            }
            if (folders.isEmpty()) {
                Text(
                    "Etiketlerin yanında istersen klasörler de kullanabilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            flattenFolders(folders).forEach { (folder, depth) ->
                var menu by remember { mutableStateOf(false) }
                NavigationDrawerItem(
                    label = { Text(folder.name) },
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                    selected = selectedFolder == folder.id,
                    onClick = { onFolder(folder) },
                    badge = {
                        Box {
                            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Klasör seçenekleri", modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Alt klasör ekle") },
                                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                    onClick = { menu = false; onCreateFolder(folder.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Yeniden adlandır") },
                                    leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
                                    onClick = { menu = false; onRename(folder) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Sil") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                                    onClick = { menu = false; onDelete(folder) },
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(start = (depth * 20).dp),
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

/** Pick a folder for one or more notes; "Klasör yok" moves them back to the top level. */
@Composable
fun FolderPickerDialog(folders: List<Folder>, current: Long?, onPick: (Long?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Klasöre taşı") },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                item {
                    FolderChoice(Icons.Outlined.FolderOff, "Klasör yok", 0, current == null) { onPick(null) }
                }
                items(flattenFolders(folders), key = { it.first.id }) { (folder, depth) ->
                    FolderChoice(Icons.Outlined.Folder, folder.name, depth, current == folder.id) { onPick(folder.id) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun FolderChoice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    depth: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = (depth * 20).dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FolderNameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Klasör adı") },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
