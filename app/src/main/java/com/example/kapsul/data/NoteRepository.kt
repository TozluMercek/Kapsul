package com.example.kapsul.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao, private val store: AttachmentStore) {
    val notes: Flow<List<Note>> = dao.observeAll()
    val allAttachments: Flow<List<Attachment>> = dao.observeAllAttachments()

    fun note(id: Long): Flow<Note?> = dao.observe(id)
    fun linked(id: Long): Flow<List<Note>> = dao.observeLinked(id)
    fun attachments(noteId: Long): Flow<List<Attachment>> = dao.observeAttachments(noteId)

    suspend fun get(id: Long): Note? = dao.get(id)
    suspend fun all(): List<Note> = dao.getAll()
    suspend fun withReminders(): List<Note> = dao.withReminders()
    suspend fun create(note: Note): Long = dao.insert(note)
    suspend fun save(note: Note) = dao.update(note)

    /** Rows cascade in the database; the files have to be removed by hand. */
    suspend fun delete(note: Note) {
        dao.attachmentsOf(note.id).forEach { store.delete(it.fileName) }
        dao.delete(note)
    }

    suspend fun link(a: Long, b: Long) {
        if (a != b) dao.insertLink(NoteLink.of(a, b))
    }

    suspend fun unlink(a: Long, b: Long) = dao.deleteLink(NoteLink.of(a, b))

    suspend fun isEmpty(): Boolean = dao.count() == 0

    suspend fun attachment(id: Long): Attachment? = dao.attachment(id)

    suspend fun attachmentsOf(noteId: Long): List<Attachment> = dao.attachmentsOf(noteId)

    suspend fun addAttachment(attachment: Attachment): Long = dao.insertAttachment(attachment)

    suspend fun updateAttachment(attachment: Attachment) = dao.updateAttachment(attachment)

    suspend fun deleteAttachment(attachment: Attachment) {
        store.delete(attachment.fileName)
        dao.deleteAttachment(attachment)
    }
}
