package com.example.kapsul.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class NoteSource { TEXT, VOICE, CHAT }

/** NOTIFICATION posts a normal notification; ALARM rings until dismissed, full screen. */
enum class ReminderMode {
    NOTIFICATION, ALARM;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: NOTIFICATION
    }
}

enum class ReminderRepeat {
    NONE, DAILY, WEEKLY;

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: NONE
    }
}

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val tags: List<String> = emptyList(),
    val source: NoteSource = NoteSource.TEXT,
    val pinned: Boolean = false,
    /** Title was generated on-device; it keeps following the body until the user writes one. */
    val autoTitle: Boolean = true,
    /** Tags were edited by hand; stop re-tagging automatically. */
    val manualTags: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** No longer used (the note-style feature was removed); kept so existing databases stay valid. */
    val cover: String? = null,
    /** No longer used; see [cover]. */
    val pageStyle: String? = null,
    /** Next reminder time (epoch millis), null when no reminder is set. */
    val reminderAt: Long? = null,
    /** [ReminderMode] name. */
    val reminderMode: String? = null,
    /** [ReminderRepeat] name. */
    val reminderRepeat: String? = null,
    /** Encoded background style (see `NoteBackground`), null for the default. */
    val background: String? = null,
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()
}

/**
 * Undirected link between two notes. Stored once with [aId] < [bId] so a link
 * shows up on both sides (bi-directional linking).
 */
@Entity(
    tableName = "note_links",
    primaryKeys = ["aId", "bId"],
    foreignKeys = [
        ForeignKey(Note::class, ["id"], ["aId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Note::class, ["id"], ["bId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("bId")],
)
data class NoteLink(val aId: Long, val bId: Long) {
    companion object {
        fun of(x: Long, y: Long) = NoteLink(minOf(x, y), maxOf(x, y))
    }
}

class Converters {
    @TypeConverter
    fun tagsToString(tags: List<String>): String = tags.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToTags(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    @TypeConverter
    fun sourceToString(source: NoteSource): String = source.name

    @TypeConverter
    fun stringToSource(value: String): NoteSource = NoteSource.valueOf(value)

    @TypeConverter
    fun attachmentTypeToString(type: AttachmentType): String = type.name

    @TypeConverter
    fun stringToAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
