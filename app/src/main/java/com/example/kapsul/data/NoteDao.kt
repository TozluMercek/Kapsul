package com.example.kapsul.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observe(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun trashedBefore(before: Long): List<Note>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun trashed(): List<Note>

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL AND deletedAt IS NULL")
    suspend fun withReminders(): List<Note>

    @Query("SELECT * FROM notes WHERE capsuleUntil IS NOT NULL AND capsuleCipher IS NOT NULL AND deletedAt IS NULL")
    suspend fun sealedCapsules(): List<Note>

    @Query("SELECT COUNT(*) FROM notes WHERE deletedAt IS NULL")
    suspend fun count(): Int

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: NoteLink)

    @Delete
    suspend fun deleteLink(link: NoteLink)

    @Query(
        """
        SELECT n.* FROM notes n
        JOIN note_links l ON (l.aId = :id AND n.id = l.bId) OR (l.bId = :id AND n.id = l.aId)
        WHERE n.deletedAt IS NULL
        ORDER BY n.updatedAt DESC
        """
    )
    fun observeLinked(id: Long): Flow<List<Note>>

    @Query("SELECT * FROM note_links")
    fun observeLinks(): Flow<List<NoteLink>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt")
    fun observeAttachments(noteId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments ORDER BY createdAt")
    fun observeAllAttachments(): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId")
    suspend fun attachmentsOf(noteId: Long): List<Attachment>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun attachment(id: Long): Attachment?

    @Insert
    suspend fun insertAttachment(attachment: Attachment): Long

    @Update
    suspend fun updateAttachment(attachment: Attachment)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    // ------------------------------------------------------------ folders

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun observeFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders")
    suspend fun folders(): List<Folder>

    @Insert
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("UPDATE notes SET folderId = :to WHERE folderId = :from")
    suspend fun moveNotesBetweenFolders(from: Long, to: Long?)

    @Query("UPDATE folders SET parentId = :to WHERE parentId = :from")
    suspend fun reparentFolders(from: Long, to: Long?)
}
