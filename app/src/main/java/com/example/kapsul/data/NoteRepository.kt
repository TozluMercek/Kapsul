package com.example.kapsul.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao, private val store: AttachmentStore) {
    val notes: Flow<List<Note>> = dao.observeAll()
    val trash: Flow<List<Note>> = dao.observeTrash()
    val folders: Flow<List<Folder>> = dao.observeFolders()
    val allAttachments: Flow<List<Attachment>> = dao.observeAllAttachments()

    fun note(id: Long): Flow<Note?> = dao.observe(id)
    fun linked(id: Long): Flow<List<Note>> = dao.observeLinked(id)
    fun attachments(noteId: Long): Flow<List<Attachment>> = dao.observeAttachments(noteId)

    suspend fun get(id: Long): Note? = dao.get(id)
    suspend fun all(): List<Note> = dao.getAll()
    suspend fun withReminders(): List<Note> = dao.withReminders()
    suspend fun sealedCapsules(): List<Note> = dao.sealedCapsules()
    suspend fun create(note: Note): Long = dao.insert(note)
    suspend fun save(note: Note) = dao.update(note)

    /** Rows cascade in the database; the files have to be removed by hand. */
    suspend fun delete(note: Note) {
        dao.attachmentsOf(note.id).forEach { store.delete(it.fileName) }
        dao.delete(note)
    }

    // -------------------------------------------------------------- trash

    suspend fun moveToTrash(note: Note) = dao.update(note.copy(deletedAt = System.currentTimeMillis()))

    suspend fun restore(note: Note) = dao.update(note.copy(deletedAt = null))

    suspend fun emptyTrash() = dao.trashed().forEach { delete(it) }

    /** Trashed notes are kept for [TRASH_DAYS] days, then deleted for good. */
    suspend fun purgeTrash(now: Long = System.currentTimeMillis()) =
        dao.trashedBefore(now - TRASH_DAYS * 86_400_000L).forEach { delete(it) }

    // ------------------------------------------------------------ folders

    suspend fun createFolder(name: String, parentId: Long?): Long = dao.insertFolder(Folder(name = name, parentId = parentId))

    suspend fun renameFolder(folder: Folder, name: String) = dao.updateFolder(folder.copy(name = name))

    /** Its notes and sub-folders move up one level; nothing is deleted with it. */
    suspend fun deleteFolder(folder: Folder) {
        dao.moveNotesBetweenFolders(folder.id, folder.parentId)
        dao.reparentFolders(folder.id, folder.parentId)
        dao.deleteFolder(folder)
    }

    // -------------------------------------------------------------- links

    suspend fun link(a: Long, b: Long) {
        if (a != b) dao.insertLink(NoteLink.of(a, b))
    }

    suspend fun unlink(a: Long, b: Long) = dao.deleteLink(NoteLink.of(a, b))

    suspend fun isEmpty(): Boolean = dao.count() == 0

    // -------------------------------------------------------- attachments

    suspend fun attachment(id: Long): Attachment? = dao.attachment(id)

    suspend fun attachmentsOf(noteId: Long): List<Attachment> = dao.attachmentsOf(noteId)

    suspend fun addAttachment(attachment: Attachment): Long = dao.insertAttachment(attachment)

    suspend fun updateAttachment(attachment: Attachment) = dao.updateAttachment(attachment)

    suspend fun deleteAttachment(attachment: Attachment) {
        store.delete(attachment.fileName)
        dao.deleteAttachment(attachment)
    }

    companion object {
        const val TRASH_DAYS = 30
    }
}
