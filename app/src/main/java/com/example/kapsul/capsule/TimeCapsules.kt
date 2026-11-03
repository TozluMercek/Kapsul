package com.example.kapsul.capsule

import com.example.kapsul.data.Note
import com.example.kapsul.data.NoteRepository

/** Sealing and opening of time-capsule notes. */
object TimeCapsules {
    /** Encrypts the body away until [until]; the title stays visible. */
    fun seal(note: Note, until: Long): Note = note.copy(
        capsuleUntil = until,
        capsuleCipher = CapsuleCrypto.seal(note.body),
        body = "",
    )

    /** Unseals [note] if its day has come. Returns the opened note, or null if it stays sealed. */
    suspend fun openIfDue(repository: NoteRepository, note: Note, now: Long = System.currentTimeMillis()): Note? {
        val until = note.capsuleUntil ?: return null
        val cipher = note.capsuleCipher ?: return null
        if (until > now) return null
        val body = runCatching { CapsuleCrypto.open(cipher) }.getOrNull() ?: return null
        val opened = note.copy(body = body, capsuleCipher = null, updatedAt = now)
        repository.save(opened)
        return opened
    }
}
