@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
    FlowPreview::class,
)

package com.example.kapsul.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kapsul.data.Attachment
import com.example.kapsul.data.AttachmentType
import com.example.kapsul.data.Note
import com.example.kapsul.data.NoteSource
import com.example.kapsul.ui.EditorDraft
import com.example.kapsul.ui.KapsulViewModel
import com.example.kapsul.ui.attachments.AttachmentList
import com.example.kapsul.ui.attachments.AudioRecorderPanel
import com.example.kapsul.ui.attachments.DrawingDialog
import com.example.kapsul.ui.attachments.ImageViewerDialog
import com.example.kapsul.ui.attachments.InsertAction
import com.example.kapsul.ui.attachments.InsertCallbacks
import com.example.kapsul.ui.attachments.InsertMenu
import com.example.kapsul.ui.attachments.PdfViewerDialog
import com.example.kapsul.ui.attachments.StickyColors
import com.example.kapsul.ui.attachments.StickyNoteDialog
import com.example.kapsul.ui.attachments.TableDialog
import com.example.kapsul.ui.attachments.markdownTable
import com.example.kapsul.ui.attachments.rememberSystemInsertLaunchers
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import com.example.kapsul.data.ReminderMode
import com.example.kapsul.data.ReminderRepeat
import com.example.kapsul.ui.background.BackgroundColors
import com.example.kapsul.ui.background.BackgroundSheet
import com.example.kapsul.ui.background.NoteBackground
import com.example.kapsul.ui.background.rememberBackgroundColors
import com.example.kapsul.ui.reminders.ReminderPermissionsDialog
import com.example.kapsul.ui.reminders.ReminderSheet
import com.example.kapsul.ui.reminders.missingReminderPermissions
import com.example.kapsul.ui.reminders.reminderLabel
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.kapsul.ui.components.MarkdownView
import com.example.kapsul.ui.components.tagColors
import com.example.kapsul.util.relativeDate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@Composable
fun EditorScreen(
    noteId: Long,
    vm: KapsulViewModel,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val initial by produceState<Note?>(null, noteId) { value = vm.load(noteId) }
    val note = initial
    if (note == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }
    EditorContent(note, vm, onBack, onOpenNote)
}

@Composable
private fun EditorContent(
    initial: Note,
    vm: KapsulViewModel,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val id = initial.id
    var title by rememberSaveable { mutableStateOf(if (initial.autoTitle) "" else initial.title) }
    var body by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initial.body)) }
    var manualTags by rememberSaveable { mutableStateOf(initial.manualTags) }
    var tags by rememberSaveable { mutableStateOf(initial.tags) }
    var preview by rememberSaveable { mutableStateOf(initial.body.isNotBlank()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }

    var overflowMenu by remember { mutableStateOf(false) }
    var reminderSheet by remember { mutableStateOf(false) }
    var backgroundSheet by remember { mutableStateOf(false) }
    var missingPermissions by remember { mutableStateOf(emptyList<Pair<String, Intent>>()) }
    val context = LocalContext.current

    // Insert menu and the in-app tools it opens.
    var insertMenu by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var drawing by remember { mutableStateOf(false) }
    var tableDialog by remember { mutableStateOf(false) }
    var stickyEditing by remember { mutableStateOf<Attachment?>(null) }
    var stickyNew by remember { mutableStateOf(false) }
    var viewingImage by remember { mutableStateOf<Attachment?>(null) }
    var viewingPdf by remember { mutableStateOf<Attachment?>(null) }
    var deletingAttachment by remember { mutableStateOf<Attachment?>(null) }
    val attachments by remember(id) { vm.attachments(id) }.collectAsStateWithLifecycle(emptyList())

    val launchSystem = rememberSystemInsertLaunchers(
        newPhotoFile = { vm.store.newFile("jpg") },
        uriFor = vm.store::uriFor,
        callbacks = InsertCallbacks(
            onPdf = { vm.importAttachment(id, it, AttachmentType.PDF) },
            onImage = { vm.importAttachment(id, it, AttachmentType.IMAGE, label = "Görüntü") },
            onAudioFile = { vm.importAttachment(id, it, AttachmentType.AUDIO) },
            onPhoto = { vm.addFileAttachment(id, AttachmentType.IMAGE, it, vm.photoLabel()) },
            onScan = { uri, pages -> vm.importAttachment(id, uri, AttachmentType.PDF, label = vm.scanLabel(pages)) },
            onMessage = vm::message,
        ),
    )

    fun onInsert(action: InsertAction) {
        when (action) {
            InsertAction.Recording -> recording = true
            InsertAction.Drawing -> drawing = true
            InsertAction.Sticky -> stickyNew = true
            InsertAction.Table -> tableDialog = true
            else -> launchSystem(action)
        }
    }

    val live by vm.observe(id).collectAsStateWithLifecycle(initial)
    val linked by remember(id) { vm.linked(id) }.collectAsStateWithLifecycle(emptyList())
    val allNotes by vm.notes.collectAsStateWithLifecycle()

    val suggestedTitle = remember(body.text) { vm.ai.suggestTitle(body.text) }
    val autoTags = remember(title, body.text) { vm.ai.suggestTags(title + "\n" + body.text) }
    val shownTags = if (manualTags) tags else autoTags

    val draft = EditorDraft(title, body.text, manualTags, shownTags)
    val latestDraft by rememberUpdatedState(draft)

    // Autosave: quietly persist while typing, and once more on the way out.
    LaunchedEffect(id) {
        snapshotFlow { latestDraft }.drop(1).debounce(600).collect { vm.save(id, it) }
    }
    DisposableEffect(id) {
        onDispose { if (!deleted) vm.close(id, latestDraft) }
    }

    // Related-note suggestions follow the text with a gentle delay.
    var relatedText by remember { mutableStateOf(title + "\n" + body.text) }
    LaunchedEffect(id) {
        snapshotFlow { title + "\n" + body.text }.debounce(800).collect { relatedText = it }
    }
    // derivedStateOf also tracks the view model's dismissed-suggestion list.
    val suggestions by remember(relatedText, linked, allNotes) {
        derivedStateOf { vm.linkSuggestions(id, relatedText, linked) }
    }

    val backgroundColors = rememberBackgroundColors(NoteBackground.decode(live?.background))
    NoteBackgroundTheme(backgroundColors) {
        Scaffold(
            containerColor = if (backgroundColors != null) Color.Transparent else MaterialTheme.colorScheme.surface,
            // A transparent container can't imply a content color; without this, text falls back to black.
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                TopAppBar(
                    colors = if (backgroundColors != null) {
                        TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
                    } else TopAppBarDefaults.topAppBarColors(),
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    actions = {
                        Box {
                            FilledTonalIconButton(onClick = { insertMenu = true }) {
                                Icon(Icons.Outlined.Add, contentDescription = "Ekle")
                            }
                            InsertMenu(
                                expanded = insertMenu,
                                onDismiss = { insertMenu = false },
                                onAction = ::onInsert,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        ModeToggle(preview = preview, onPreviewChange = { preview = it })
                        Spacer(Modifier.width(4.dp))
                        val note = live
                        Box {
                            IconButton(onClick = { overflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Diğer seçenekler")
                            }
                            DropdownMenu(
                                expanded = overflowMenu,
                                onDismissRequest = { overflowMenu = false },
                                shape = RoundedCornerShape(24.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Hatırlatma") },
                                    leadingIcon = { Icon(Icons.Outlined.Alarm, contentDescription = null) },
                                    onClick = { overflowMenu = false; reminderSheet = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Arka plan stili") },
                                    leadingIcon = { Icon(Icons.Outlined.FormatColorFill, contentDescription = null) },
                                    onClick = { overflowMenu = false; backgroundSheet = true },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (note?.pinned == true) "Sabitlemeyi kaldır" else "Sabitle") },
                                    leadingIcon = {
                                        Icon(if (note?.pinned == true) Icons.Filled.PushPin else Icons.Outlined.PushPin, contentDescription = null)
                                    },
                                    onClick = { overflowMenu = false; note?.let(vm::togglePin) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Sil") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                                    onClick = { overflowMenu = false; confirmDelete = true },
                                )
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TitleField(title, suggestedTitle, onTitleChange = { title = it })

                    TagEditor(
                        tags = shownTags,
                        automatic = !manualTags,
                        onRemove = { tag -> manualTags = true; tags = shownTags - tag },
                        onAdd = { tag -> manualTags = true; tags = (shownTags + tag).distinct() },
                    )

                    MetaLine(live ?: initial)

                    live?.reminderAt?.let { at ->
                        AssistChip(
                            onClick = { reminderSheet = true },
                            label = { Text(reminderLabel(at, ReminderRepeat.from(live?.reminderRepeat))) },
                            leadingIcon = {
                                Icon(
                                    if (ReminderMode.from(live?.reminderMode) == ReminderMode.ALARM) Icons.Outlined.Alarm
                                    else Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }

                    Crossfade(targetState = preview, label = "mode") { isPreview ->
                        if (isPreview) {
                            if (body.text.isBlank()) {
                                Text(
                                    "Henüz bir şey yok — yazmaya başlamak için dokun.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { preview = false }
                                        .padding(vertical = 24.dp),
                                )
                            } else {
                                MarkdownView(
                                    markdown = body.text,
                                    onToggleTask = { line -> body = body.copy(text = body.text.toggleTaskAt(line)) },
                                    onWikiLink = { linkTitle -> vm.findByTitle(linkTitle)?.let { onOpenNote(it.id) } },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            BodyField(body, onBodyChange = { body = it })
                        }
                    }

                    if (attachments.isNotEmpty()) {
                        AttachmentList(
                            attachments = attachments,
                            fileOf = vm.store::file,
                            onOpenImage = { viewingImage = it },
                            onOpenPdf = { viewingPdf = it },
                            onEditSticky = { stickyEditing = it },
                            onDelete = { deletingAttachment = it },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestions.forEach { s ->
                                LinkSuggestion(
                                    other = s.item,
                                    onLink = { vm.link(id, s.item.id) },
                                    onDismiss = { vm.dismissSuggestion(id, s.item.id) },
                                )
                            }
                        }
                    }

                    if (linked.isNotEmpty()) {
                        LinkedNotes(
                            notes = linked,
                            onOpen = onOpenNote,
                            onUnlink = { other -> vm.unlink(id, other) },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !preview,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                ) {
                    FormatToolbar(onFormat = { body = body.apply(it) })
                }
            }
        }
    }

    if (reminderSheet) {
        live?.let { note ->
            ReminderSheet(
                note = note,
                onSave = { at, mode, repeat ->
                    vm.setReminder(id, at, mode, repeat)
                    reminderSheet = false
                    missingPermissions = missingReminderPermissions(context, vm.reminders.canScheduleExact(), mode)
                },
                onRemove = {
                    vm.clearReminder(id)
                    reminderSheet = false
                },
                onDismiss = { reminderSheet = false },
            )
        }
    }

    if (missingPermissions.isNotEmpty()) {
        ReminderPermissionsDialog(missingPermissions, onDismiss = { missingPermissions = emptyList() })
    }

    if (backgroundSheet) {
        BackgroundSheet(
            current = NoteBackground.decode(live?.background),
            onSelect = { vm.setBackground(id, it?.encode()) },
            onDismiss = { backgroundSheet = false },
        )
    }

    if (recording) {
        ModalBottomSheet(onDismissRequest = { recording = false }) {
            Text(
                "Ses kaydı",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            AudioRecorderPanel(
                newFile = { vm.store.newFile("m4a") },
                onFinished = { file, duration ->
                    vm.addFileAttachment(id, AttachmentType.AUDIO, file, vm.recordingLabel(), duration)
                    recording = false
                },
                onCancel = { reason ->
                    reason?.let(vm::message)
                    recording = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
            )
        }
    }

    if (drawing) {
        DrawingDialog(
            target = { vm.store.newFile("png") },
            onSaved = { file ->
                vm.addFileAttachment(id, AttachmentType.DRAWING, file, "Çizim")
                drawing = false
            },
            onDismiss = { drawing = false },
        )
    }

    if (stickyNew || stickyEditing != null) {
        val editing = stickyEditing
        StickyNoteDialog(
            initialText = editing?.text.orEmpty(),
            initialColor = editing?.color ?: StickyColors.first(),
            onConfirm = { text, color ->
                vm.saveSticky(id, editing, text, color)
                stickyNew = false
                stickyEditing = null
            },
            onDismiss = { stickyNew = false; stickyEditing = null },
        )
    }

    if (tableDialog) {
        TableDialog(
            onConfirm = { rows, columns ->
                body = body.insertBlock(markdownTable(rows, columns))
                tableDialog = false
            },
            onDismiss = { tableDialog = false },
        )
    }

    viewingImage?.fileName?.let { name ->
        ImageViewerDialog(vm.store.file(name), onDismiss = { viewingImage = null })
    }
    viewingPdf?.let { pdf ->
        pdf.fileName?.let { name ->
            PdfViewerDialog(vm.store.file(name), pdf.label, onDismiss = { viewingPdf = null })
        }
    }

    deletingAttachment?.let { attachment ->
        AlertDialog(
            onDismissRequest = { deletingAttachment = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("Ek silinsin mi?") },
            text = { Text("“${attachment.label}” bu nottan ve cihazından kalıcı olarak silinir.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAttachment(attachment)
                    deletingAttachment = null
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deletingAttachment = null }) { Text("Vazgeç") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("Not silinsin mi?") },
            text = { Text("Bu not ve bağlantıları cihazından kalıcı olarak silinir.") },
            confirmButton = {
                TextButton(onClick = {
                    deleted = true
                    confirmDelete = false
                    vm.delete(id)
                    onBack()
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } },
        )
    }
}

/** Paints the note's background and gives its content a matching color scheme. */
@Composable
private fun NoteBackgroundTheme(colors: BackgroundColors?, content: @Composable () -> Unit) {
    if (colors == null) {
        content()
    } else {
        Box(Modifier.fillMaxSize().background(colors.brush)) {
            MaterialExpressiveTheme(colorScheme = colors.scheme, content = content)
        }
    }
}

@Composable
private fun ModeToggle(preview: Boolean, onPreviewChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        ToggleButton(
            checked = !preview,
            onCheckedChange = { onPreviewChange(false) },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = "Düzenle", modifier = Modifier.size(18.dp))
        }
        ToggleButton(
            checked = preview,
            onCheckedChange = { onPreviewChange(true) },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
        ) {
            Icon(Icons.Outlined.Visibility, contentDescription = "Önizle", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TitleField(title: String, suggested: String, onTitleChange: (String) -> Unit) {
    TextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.headlineMedium,
        placeholder = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (suggested.isNotBlank()) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = "Otomatik başlık",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    suggested.ifBlank { "Başlık" },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        colors = transparentFieldColors(),
    )
}

@Composable
private fun BodyField(value: TextFieldValue, onBodyChange: (TextFieldValue) -> Unit) {
    TextField(
        value = value,
        onValueChange = onBodyChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = transparentFieldColors(),
    )
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

@Composable
private fun TagEditor(
    tags: List<String>,
    automatic: Boolean,
    onRemove: (String) -> Unit,
    onAdd: (String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }

    FlowRow(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            val colors = tagColors(tag)
            InputChip(
                selected = true,
                onClick = { onRemove(tag) },
                label = { Text("#$tag") },
                leadingIcon = if (automatic) {
                    { Icon(Icons.Filled.AutoAwesome, contentDescription = "Otomatik", modifier = Modifier.size(16.dp)) }
                } else null,
                trailingIcon = { Text("✕", style = MaterialTheme.typography.labelSmall) },
                shape = MaterialTheme.shapes.small,
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = colors.container,
                    selectedLabelColor = colors.content,
                    selectedLeadingIconColor = colors.content,
                    selectedTrailingIconColor = colors.content,
                ),
                border = null,
            )
        }
        if (adding) {
            fun commit() {
                val clean = newTag.trim().removePrefix("#").lowercase()
                if (clean.isNotEmpty()) onAdd(clean)
                newTag = ""
                adding = false
            }
            TextField(
                value = newTag,
                onValueChange = { newTag = it },
                singleLine = true,
                placeholder = { Text("etiket") },
                modifier = Modifier.width(140.dp),
                textStyle = MaterialTheme.typography.labelLarge,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        } else {
            InputChip(
                selected = false,
                onClick = { adding = true },
                label = { Text("Etiket") },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

@Composable
private fun MetaLine(note: Note) {
    val source = when (note.source) {
        NoteSource.VOICE -> " · sesli not"
        NoteSource.CHAT -> " · sohbetten"
        NoteSource.TEXT -> ""
    }
    Text(
        "Düzenlendi ${relativeDate(note.updatedAt)}$source",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun FormatToolbar(onFormat: (Format) -> Unit) {
    HorizontalFloatingToolbar(expanded = true) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            FormatButton(Icons.Outlined.Title, "Başlık") { onFormat(Format.Heading) }
            FormatButton(Icons.Outlined.FormatBold, "Kalın") { onFormat(Format.Bold) }
            FormatButton(Icons.AutoMirrored.Outlined.FormatListBulleted, "Liste") { onFormat(Format.Bullet) }
            FormatButton(Icons.Outlined.CheckBox, "Yapılacak") { onFormat(Format.Task) }
            FormatButton(Icons.Outlined.Code, "Kod") { onFormat(Format.Code) }
            FormatButton(Icons.Outlined.FormatQuote, "Alıntı") { onFormat(Format.Quote) }
            FormatButton(Icons.Outlined.Link, "Not bağlantısı") { onFormat(Format.Link) }
        }
    }
}

@Composable
private fun FormatButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
}

@Composable
private fun LinkSuggestion(other: Note, onLink: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialShapes.Cookie4Sided.toShape(),
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.padding(7.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Bu not, “${other.title}” ile ilgili görünüyor. Bağlayayım mı?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Yoksay") }
                FilledTonalButton(onClick = onLink) { Text("Bağla") }
            }
        }
    }
}

@Composable
private fun LinkedNotes(notes: List<Note>, onOpen: (Long) -> Unit, onUnlink: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Bağlantılı notlar · ${notes.size}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
        notes.forEach { other ->
            Surface(
                onClick = { onOpen(other.id) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(other.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            relativeDate(other.updatedAt) + other.tags.joinToString("") { "  #$it" },
                            style = TextStyle.Default.merge(MaterialTheme.typography.labelSmall),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { onUnlink(other.id) }) {
                        Icon(Icons.Outlined.LinkOff, contentDescription = "Bağlantıyı kaldır")
                    }
                }
            }
        }
    }
}
