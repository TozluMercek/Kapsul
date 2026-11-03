package com.example.kapsul.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttachmentType { IMAGE, DRAWING, AUDIO, PDF, STICKY }

/**
 * Something added to a note from the insert menu. Files live in the app's private
 * storage ([AttachmentStore]); a sticky note keeps its content in [text] and [color].
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(Note::class, ["id"], ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId")],
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val type: AttachmentType,
    /** File name inside the attachments directory, null for sticky notes. */
    val fileName: String? = null,
    /** Original file name or a generated label ("Ses kaydı · 14:05"). */
    val label: String = "",
    val text: String = "",
    val color: Int = 0,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
