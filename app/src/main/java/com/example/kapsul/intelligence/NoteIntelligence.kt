package com.example.kapsul.intelligence

import com.example.kapsul.data.Note

data class Scored<T>(val item: T, val score: Float)

data class MemoryAnswer(val text: String, val sources: List<Note>)

/**
 * Everything the app "understands" about notes goes through this interface.
 *
 * The default implementation ([LocalIntelligence]) is a fully on-device, deterministic
 * engine (TF-IDF + Turkish-aware keyword rules). A local LLM / on-device embedding
 * model can replace it later without touching the UI — nothing here ever leaves the device.
 */
interface NoteIntelligence {
    fun suggestTitle(body: String): String
    fun suggestTags(text: String): List<String>
    fun related(text: String, candidates: List<Note>, limit: Int = 3): List<Scored<Note>>
    fun search(query: String, notes: List<Note>, limit: Int = 50): List<Scored<Note>>
    fun answer(question: String, notes: List<Note>): MemoryAnswer
}
