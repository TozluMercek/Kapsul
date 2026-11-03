package com.example.kapsul.intelligence

import com.example.kapsul.data.Note
import com.example.kapsul.util.relativeDate
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

private val TR = Locale.forLanguageTag("tr")

class LocalIntelligence : NoteIntelligence {

    // ---------------------------------------------------------------- title

    override fun suggestTitle(body: String): String {
        val lines = body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("```") }
            .toList()
        if (lines.isEmpty()) return ""

        lines.firstOrNull { HEADING.matches(it) }?.let {
            return it.trimStart('#').trim().capitalizeTr()
        }

        val first = lines.first().replace(LIST_PREFIX, "").replace(INLINE_MARKS, "").trim()
        val sentence = first.split(SENTENCE_END).first().trimEnd('.', '!', '?', ':', ',', ';').trim()
        val words = sentence.split(WHITESPACE).filter { it.isNotEmpty() }
        val title = if (words.size <= 7) sentence else words.take(6).joinToString(" ") + "…"
        return title.capitalizeTr()
    }

    // ----------------------------------------------------------------- tags

    override fun suggestTags(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val explicit = HASHTAG.findAll(text).map { it.groupValues[1].lowercase(TR) }.toList()

        val words = text.lowercase(TR).split(NON_WORD).filter { it.length >= 2 }
        val scores = mutableMapOf<String, Int>()
        for ((tag, rule) in TAG_RULES) {
            var score = words.count { w -> rule.exact.contains(w) || rule.prefixes.any { w.startsWith(it) } }
            if (rule.raw.any { text.contains(it) }) score += 2
            if (score > 0) scores[tag] = score
        }
        val inferred = scores.entries.sortedByDescending { it.value }.map { it.key }
        return (explicit + inferred).distinct().take(4)
    }

    // ------------------------------------------------------ related / search

    override fun related(text: String, candidates: List<Note>, limit: Int): List<Scored<Note>> {
        val queryTokens = tokens(text)
        if (queryTokens.size < 2 || candidates.isEmpty()) return emptyList()
        val queryTags = suggestTags(text).toSet()
        val index = Index(candidates)
        val q = index.vector(queryTokens)
        return candidates.indices
            .map { i ->
                val sharedTags = candidates[i].tags.count { it in queryTags }
                Scored(candidates[i], cosine(q, index.docs[i]) + 0.06f * sharedTags)
            }
            .filter { it.score >= 0.18f }
            .sortedByDescending { it.score }
            .take(limit)
    }

    override fun search(query: String, notes: List<Note>, limit: Int): List<Scored<Note>> {
        val needle = query.trim().lowercase(TR)
        if (needle.isEmpty()) return notes.map { Scored(it, 0f) }
        val index = Index(notes)
        val q = index.vector(tokens(needle))
        return notes.indices
            .map { i ->
                val n = notes[i]
                val literal = n.title.lowercase(TR).contains(needle) ||
                    n.body.lowercase(TR).contains(needle) ||
                    n.tags.any { it.contains(needle.removePrefix("#")) }
                Scored(n, cosine(q, index.docs[i]) + if (literal) 1f else 0f)
            }
            .filter { it.score > 0.08f }
            .sortedByDescending { it.score }
            .take(limit)
    }

    // ----------------------------------------------------------- answering

    override fun answer(question: String, notes: List<Note>): MemoryAnswer {
        val hits = search(question, notes, limit = 4).filter { it.score > 0.12f }
        if (hits.isEmpty()) {
            return MemoryAnswer(
                "Arşivinde bununla ilgili bir şey bulamadım. İstersen bunu yeni bir not olarak kaydedebilirim.",
                emptyList(),
            )
        }
        val best = hits.first().item
        val snippet = bestSnippet(question, best.body)
        val text = buildString {
            append("En yakın eşleşme “${best.title}” — ${relativeDate(best.updatedAt)}.")
            if (snippet.isNotBlank()) append("\n\n“$snippet”")
            if (hits.size > 1) append("\n\nBununla ilgili ${hits.size - 1} not daha buldum.")
        }
        return MemoryAnswer(text, hits.map { it.item })
    }

    private fun bestSnippet(question: String, body: String): String {
        val q = tokens(question).toSet()
        val parts = body.split('\n', '.', '!', '?')
            .map { it.replace(LIST_PREFIX, "").trim() }
            .filter { it.length > 3 }
        val best = parts.maxByOrNull { part -> tokens(part).count { it in q } } ?: return ""
        return if (best.length > 180) best.take(177) + "…" else best
    }

    // -------------------------------------------------------------- tf-idf

    private inner class Index(notes: List<Note>) {
        private val tokenized = notes.map { n ->
            tokens(n.title) + tokens(n.title) + n.tags.map(::stem) + tokens(n.body)
        }
        private val idf: Map<String, Float> = run {
            val df = HashMap<String, Int>()
            tokenized.forEach { doc -> doc.toSet().forEach { df[it] = (df[it] ?: 0) + 1 } }
            val n = tokenized.size.toFloat()
            df.mapValues { (_, count) -> ln((n + 1f) / (count + 1f)) + 1f }
        }
        private val defaultIdf = ln(tokenized.size + 1f) + 1f
        val docs: List<Map<String, Float>> = tokenized.map(::vector)

        fun vector(doc: List<String>): Map<String, Float> {
            if (doc.isEmpty()) return emptyMap()
            val tf = doc.groupingBy { it }.eachCount()
            val raw = tf.mapValues { (term, c) -> (1f + ln(c.toFloat())) * (idf[term] ?: defaultIdf) }
            val norm = sqrt(raw.values.sumOf { (it * it).toDouble() }).toFloat()
            return if (norm == 0f) raw else raw.mapValues { it.value / norm }
        }
    }

    private fun cosine(a: Map<String, Float>, b: Map<String, Float>): Float {
        val (small, large) = if (a.size <= b.size) a to b else b to a
        var dot = 0f
        for ((k, v) in small) dot += v * (large[k] ?: 0f)
        return dot
    }

    private fun tokens(text: String): List<String> =
        text.lowercase(TR)
            .split(NON_WORD)
            .filter { it.length >= 2 && it !in STOPWORDS }
            .map(::stem)

    /** Turkish is agglutinative; a 5-character prefix is a surprisingly good cheap stemmer. */
    private fun stem(word: String): String = if (word.length > 5) word.take(5) else word

    private fun String.capitalizeTr(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(TR) else it.toString() }

    private class TagRule(
        val prefixes: List<String>,
        val exact: Set<String> = emptySet(),
        val raw: List<String> = emptyList(),
    )

    private companion object {
        val HEADING = Regex("^#{1,6}\\s+.+")
        val LIST_PREFIX = Regex("^(- \\[[ xX]]\\s*|[-*+]\\s+|\\d+[.)]\\s+|>\\s*)")
        val INLINE_MARKS = Regex("[*_`]|\\[\\[|]]")
        val SENTENCE_END = Regex("(?<=[.!?])\\s+")
        val WHITESPACE = Regex("\\s+")
        val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        val HASHTAG = Regex("(?<![\\p{L}\\p{N}])#([\\p{L}][\\p{L}\\p{N}_]*)")

        val TAG_RULES: Map<String, TagRule> = linkedMapOf(
            "toplantı" to TagRule(
                listOf("toplantı", "görüş", "meeting", "buluş", "randevu", "sunum", "zoom", "ajanda"),
            ),
            "görev" to TagRule(
                listOf("yapılacak", "todo", "unutma", "hatırlat", "lazım", "gerekiyor", "bitir", "teslim"),
                raw = listOf("- [ ]", "- [x]"),
            ),
            "alışveriş" to TagRule(
                listOf("alışveriş", "market", "ekmek", "satın", "sipariş", "sepet", "indirim", "yumurta", "peynir"),
                exact = setOf("süt", "al", "alınacak"),
            ),
            "fikir" to TagRule(
                listOf("fikir", "belki", "acaba", "öneri", "idea", "ilham", "konsept", "hayal"),
            ),
            "proje" to TagRule(
                listOf("proje", "sprint", "taslak", "müşteri", "deadline", "milestone", "roadmap", "tasarım", "prototip"),
            ),
            "kod" to TagRule(
                listOf("kotlin", "android", "bug", "deploy", "commit", "fonksiyon", "python", "github", "compose", "gradle"),
                exact = setOf("kod", "kodu", "api", "sql", "hata", "class", "fun"),
                raw = listOf("```"),
            ),
            "tarif" to TagRule(
                listOf("tarif", "fırın", "pişir", "yemek", "gram", "kaşık", "bardak", "soğan", "domates", "hamur", "zeytinyağ"),
            ),
            "ev" to TagRule(
                listOf("tamir", "boya", "musluk", "matkap", "vida", "tesisat", "usta", "dolap", "mutfak", "banyo", "priz", "dübel", "silikon"),
                exact = setOf("ev", "evde", "eve", "evin", "evi"),
            ),
            "finans" to TagRule(
                listOf("fatura", "ödeme", "bütçe", "harcama", "maaş", "kredi", "banka", "borç", "tasarruf", "kira"),
                exact = setOf("tl", "lira"),
                raw = listOf("₺"),
            ),
            "sağlık" to TagRule(
                listOf("doktor", "ilaç", "hastane", "egzersiz", "uyku", "diyet", "vitamin", "tahlil", "antrenman"),
                exact = setOf("spor", "koşu"),
            ),
            "öğrenme" to TagRule(
                listOf("öğren", "kurs", "kitap", "makale", "video", "izle", "eğitim", "sınav", "podcast", "ders"),
                exact = setOf("oku", "okumak"),
            ),
            "takvim" to TagRule(
                listOf("yarın", "bugün", "haftaya", "pazartesi", "salı", "çarşamba", "perşembe", "cuma", "cumartesi", "pazar"),
            ),
        )

        val STOPWORDS = setOf(
            "ve", "ile", "bir", "bu", "şu", "o", "da", "de", "mi", "mı", "mu", "mü", "ki", "ya", "ne",
            "için", "gibi", "çok", "daha", "en", "ama", "fakat", "veya", "hem", "her", "bana", "beni",
            "benim", "sen", "ben", "biz", "siz", "onlar", "olan", "olarak", "kadar", "sonra", "önce",
            "hangi", "neydi", "nedir", "nasıl", "neden", "niye", "şey", "şeyi", "şeyler", "var", "yok",
            "geçen", "konuşmuştuk", "olan", "değil", "diye", "the", "a", "an", "and", "or", "of", "to",
            "in", "is", "it", "for", "on", "with",
        )
    }
}
