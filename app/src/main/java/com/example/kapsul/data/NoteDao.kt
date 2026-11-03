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
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observe(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL")
    suspend fun withReminders(): List<Note>

    @Query("SELECT COUNT(*) FROM notes")
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
}
