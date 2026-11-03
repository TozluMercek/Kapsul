package com.example.kapsul.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kapsul.KapsulApplication
import com.example.kapsul.data.Attachment
import com.example.kapsul.data.AttachmentType
import com.example.kapsul.data.Note
import com.example.kapsul.data.NoteLink
import com.example.kapsul.data.NoteSource
import com.example.kapsul.data.ReminderMode
import com.example.kapsul.data.ReminderRepeat
import com.example.kapsul.data.LegacySamples
import com.example.kapsul.intelligence.Scored
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EditorDraft(
    val title: String,
    val body: String,
    val manualTags: Boolean,
    val tags: List<String>,
)

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val sources: List<Note> = emptyList(),
    val savedNoteId: Long? = null,
)

sealed interface UiEvent {
    /** A quick note was saved; [suggestion] is an older note it looks related to. */
    data class Captured(val note: Note, val suggestion: Note?) : UiEvent
    data class Message(val text: String) : UiEvent
}

class KapsulViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as KapsulApplication
    private val repo = app.repository
    val ai = app.intelligence
    val settings = app.settings
    // Declared before `init` below: properties initialize in order, and init uses it.
    val store = app.attachmentStore

    val notes: StateFlow<List<Note>?> =
        repo.notes.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    val visibleNotes: StateFlow<List<Note>?> =
        combine(notes, _query, _selectedTag) { all, q, tag ->
            if (all == null) return@combine null
            val filtered = if (tag == null) all else all.filter { tag in it.tags }
            if (q.isBlank()) filtered else ai.search(q, filtered).map { it.item }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tags: StateFlow<List<String>> =
        notes.map { list ->
            list.orEmpty().flatMap { it.tags }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .map { it.key }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    /** Link suggestions the user said "no" to, so we don't keep asking. */
    private val dismissedLinks = mutableStateListOf<NoteLink>()

    val chat = mutableStateListOf<ChatMessage>()
    var thinking by mutableStateOf(false)
        private set
    private var nextMessageId = 0L

    init {
        viewModelScope.launch {
            removeLegacySamples()
            store.deleteLegacyPageImages()
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    // ------------------------------------------------------------ capture

    /** Saves a quick note; [recording] is a finished voice recording to attach. */
    fun quickCapture(text: String, source: NoteSource, recording: Pair<File, Long>? = null) {
        val body = text.trim()
        if (body.isEmpty() && recording == null) return
        viewModelScope.launch {
            val note = if (body.isNotEmpty()) {
                createFromText(body, source)
            } else {
                val title = "Sesli not · ${timeLabel()}"
                val blank = Note(title = title, tags = listOf("ses"), source = NoteSource.VOICE, autoTitle = false)
                blank.copy(id = repo.create(blank))
            }
            if (recording != null) {
                val (file, duration) = recording
                repo.addAttachment(
                    Attachment(noteId = note.id, type = AttachmentType.AUDIO, fileName = file.name, label = "Ses kaydı · ${timeLabel()}", durationMs = duration)
                )
            }
            val others = repo.all().filter { it.id != note.id }
            val suggestion = if (body.isEmpty()) null else withContext(Dispatchers.Default) {
                ai.related(body, others, limit = 1).firstOrNull()?.item
            }
            _events.emit(UiEvent.Captured(note, suggestion))
        }
    }

    private fun timeLabel(): String =
        SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("tr")).format(Date())

    // -------------------------------------------------------- attachments

    /** Attachments grouped by note, for thumbnails and badges on the home grid. */
    val attachmentsByNote: StateFlow<Map<Long, List<Attachment>>> =
        repo.allAttachments.map { list -> list.groupBy { it.noteId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun attachments(noteId: Long): Flow<List<Attachment>> = repo.attachments(noteId)

    fun addFileAttachment(noteId: Long, type: AttachmentType, file: File, label: String, durationMs: Long = 0) {
        viewModelScope.launch {
            repo.addAttachment(Attachment(noteId = noteId, type = type, fileName = file.name, label = label, durationMs = durationMs))
        }
    }

    /** Copies a picked PDF / image / audio file into the note. */
    fun importAttachment(noteId: Long, uri: Uri, type: AttachmentType, label: String? = null) {
        viewModelScope.launch {
            try {
                val fallback = when (type) {
                    AttachmentType.PDF -> "pdf"
                    AttachmentType.AUDIO -> "m4a"
                    else -> "jpg"
                }
                val imported = store.import(uri, fallback)
                val duration = if (type == AttachmentType.AUDIO) {
                    withContext(Dispatchers.IO) { store.audioDurationMs(imported.file) }
                } else 0L
                repo.addAttachment(
                    Attachment(
                        noteId = noteId,
                        type = type,
                        fileName = imported.file.name,
                        label = label ?: imported.displayName,
                        durationMs = duration,
                    )
                )
            } catch (e: Exception) {
                _events.emit(UiEvent.Message("Dosya eklenemedi"))
            }
        }
    }

    fun saveSticky(noteId: Long, existing: Attachment?, text: String, color: Int) {
        viewModelScope.launch {
            if (existing == null) {
                repo.addAttachment(Attachment(noteId = noteId, type = AttachmentType.STICKY, text = text, color = color, label = "Yapışkan not"))
            } else {
                repo.updateAttachment(existing.copy(text = text, color = color))
            }
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch { repo.deleteAttachment(attachment) }
    }

    fun photoLabel() = "Fotoğraf · ${timeLabel()}"
    fun recordingLabel() = "Ses kaydı · ${timeLabel()}"
    fun scanLabel(pages: Int) = "Taranan belge · $pages sayfa · ${timeLabel()}"

    fun message(text: String) {
        _events.tryEmit(UiEvent.Message(text))
    }

    private suspend fun createFromText(body: String, source: NoteSource): Note {
        val note = Note(
            title = ai.suggestTitle(body),
            body = body,
            tags = ai.suggestTags(body),
            source = source,
        )
        val id = repo.create(note)
        linkWikiReferences(id, body)
        return note.copy(id = id)
    }

    fun newNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repo.create(Note())) }
    }

    // ------------------------------------------------------------- editor

    suspend fun load(id: Long): Note? = repo.get(id)
    fun observe(id: Long): Flow<Note?> = repo.note(id)
    fun linked(id: Long): Flow<List<Note>> = repo.linked(id)

    fun save(id: Long, draft: EditorDraft) {
        viewModelScope.launch { persist(id, draft) }
    }

    /**
     * Called when the editor closes: persists the last edits and drops the note
     * if it was left empty (no text and nothing attached).
     */
    fun close(id: Long, draft: EditorDraft) {
        viewModelScope.launch {
            val current = repo.get(id) ?: return@launch
            val empty = draft.title.isBlank() && draft.body.isBlank()
            if (empty && repo.attachmentsOf(id).isEmpty()) {
                repo.delete(current)
            } else {
                persist(id, draft)
            }
        }
    }

    private suspend fun persist(id: Long, draft: EditorDraft) {
        val current = repo.get(id) ?: return
        val autoTitle = draft.title.isBlank()
        val suggested = ai.suggestTitle(draft.body)
        val updated = current.copy(
            // A body-less note (only a recording, photo…) keeps the title it was given.
            title = if (!autoTitle) draft.title.trim() else suggested.ifBlank {
                current.title.ifBlank { repo.attachmentsOf(id).firstOrNull()?.label.orEmpty() }
            },
            body = draft.body,
            autoTitle = autoTitle,
            manualTags = draft.manualTags,
            tags = if (draft.manualTags) draft.tags else ai.suggestTags(draft.title + "\n" + draft.body),
        )
        if (updated == current) return
        repo.save(updated.copy(updatedAt = System.currentTimeMillis()))
        linkWikiReferences(id, draft.body)
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repo.save(note.copy(pinned = !note.pinned)) }
    }

    // --------------------------------------------------------- reminders

    val reminders = app.reminders

    fun setReminder(id: Long, at: Long, mode: ReminderMode, repeat: ReminderRepeat) {
        viewModelScope.launch {
            val note = repo.get(id) ?: return@launch
            val updated = note.copy(reminderAt = at, reminderMode = mode.name, reminderRepeat = repeat.name)
            repo.save(updated)
            reminders.schedule(updated)
        }
    }

    fun clearReminder(id: Long) {
        viewModelScope.launch {
            reminders.cancel(id)
            repo.get(id)?.let { repo.save(it.copy(reminderAt = null, reminderMode = null, reminderRepeat = null)) }
        }
    }

    // -------------------------------------------------------- background

    fun setBackground(id: Long, background: String?) {
        viewModelScope.launch { repo.get(id)?.let { repo.save(it.copy(background = background)) } }
    }

    fun deleteNotes(ids: Collection<Long>) {
        val targets = ids.toList()
        viewModelScope.launch {
            targets.forEach { id -> reminders.cancel(id); repo.get(id)?.let { repo.delete(it) } }
            _events.emit(UiEvent.Message(if (targets.size == 1) "Not silindi" else "${targets.size} not silindi"))
        }
    }

    fun delete(id: Long) {
        reminders.cancel(id)
        viewModelScope.launch { repo.get(id)?.let { repo.delete(it) } }
    }

    fun link(a: Long, b: Long) {
        viewModelScope.launch { repo.link(a, b) }
    }

    fun unlink(a: Long, b: Long) {
        viewModelScope.launch { repo.unlink(a, b) }
    }

    fun dismissSuggestion(a: Long, b: Long) {
        dismissedLinks += NoteLink.of(a, b)
    }

    /** "This note looks related to X — link it?" */
    fun linkSuggestions(id: Long, text: String, linked: List<Note>): List<Scored<Note>> {
        val linkedIds = linked.mapTo(HashSet()) { it.id }
        val candidates = notes.value.orEmpty().filter {
            it.id != id && it.id !in linkedIds && NoteLink.of(id, it.id) !in dismissedLinks
        }
        return ai.related(text, candidates, limit = 2)
    }

    fun findByTitle(title: String): Note? =
        notes.value?.firstOrNull { it.title.equals(title.trim(), ignoreCase = true) }

    /** `[[Başka not]]` in a body creates a real, bi-directional link. */
    private suspend fun linkWikiReferences(id: Long, body: String) {
        val titles = WIKI_LINK.findAll(body).map { it.groupValues[1].trim() }.toSet()
        if (titles.isEmpty()) return
        repo.all()
            .filter { n -> n.id != id && titles.any { it.equals(n.title, ignoreCase = true) } }
            .forEach { repo.link(id, it.id) }
    }

    // --------------------------------------------------------------- chat

    fun ask(text: String) {
        val question = text.trim()
        if (question.isEmpty()) return
        val previousUserMessage = chat.lastOrNull { it.fromUser }
        chat += ChatMessage(nextMessageId++, fromUser = true, text = question)

        viewModelScope.launch {
            thinking = true
            val explicitSave = SAVE_COMMAND.find(question)?.groupValues?.get(2)?.trim()
            val reply = when {
                !explicitSave.isNullOrEmpty() -> saveReply(explicitSave)
                SAVE_THAT.containsMatchIn(question) && previousUserMessage != null ->
                    saveReply(previousUserMessage.text, markMessage = previousUserMessage.id)
                else -> {
                    val all = repo.all()
                    val answer = withContext(Dispatchers.Default) { ai.answer(question, all) }
                    delay(350) // let the loading indicator breathe
                    ChatMessage(nextMessageId++, fromUser = false, text = answer.text, sources = answer.sources)
                }
            }
            thinking = false
            chat += reply
        }
    }

    private suspend fun saveReply(text: String, markMessage: Long? = null): ChatMessage {
        val note = createFromText(text, NoteSource.CHAT)
        markMessage?.let { markSaved(it, note.id) }
        val tags = note.tags.joinToString(" ") { "#$it" }
        return ChatMessage(
            nextMessageId++,
            fromUser = false,
            text = "Kaydettim: “${note.title}”" + if (tags.isNotEmpty()) "  $tags" else "",
            sources = listOf(note),
        )
    }

    fun saveMessageAsNote(message: ChatMessage) {
        if (message.savedNoteId != null) return
        viewModelScope.launch {
            val note = createFromText(message.text, NoteSource.CHAT)
            markSaved(message.id, note.id)
            _events.emit(UiEvent.Message("Not olarak kaydedildi: “${note.title}”"))
        }
    }

    private fun markSaved(messageId: Long, noteId: Long) {
        val index = chat.indexOfFirst { it.id == messageId }
        if (index >= 0) chat[index] = chat[index].copy(savedNoteId = noteId)
    }

    // --------------------------------------------------------------- seed

    /** Older versions seeded sample notes; remove the ones nobody changed, once. */
    private suspend fun removeLegacySamples() {
        val prefs = app.getSharedPreferences("kapsul", Context.MODE_PRIVATE)
        if (prefs.getBoolean("samples_removed", false)) return
        repo.all().filter(LegacySamples::isUntouchedSample).forEach { repo.delete(it) }
        prefs.edit().putBoolean("samples_removed", true).apply()
    }

    private companion object {
        val WIKI_LINK = Regex("\\[\\[([^\\]]+)]]")
        val SAVE_COMMAND = Regex(
            "^(kaydet|not al|not et|hatırla)\\s*[:：-]\\s*(.+)",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val SAVE_THAT = Regex("(bunu|onu)?\\s*(yeni bir )?not olarak kaydet|bunu kaydet", RegexOption.IGNORE_CASE)
    }
}
