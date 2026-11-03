package com.example.kapsul

import com.example.kapsul.data.Note
import com.example.kapsul.intelligence.LocalIntelligence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalIntelligenceTest {
    private val ai = LocalIntelligence()

    @Test
    fun titleFromRawSentence() {
        assertEquals("Yarın Ahmet ile görüşülecek", ai.suggestTitle("yarın Ahmet ile görüşülecek."))
    }

    @Test
    fun titleFromMarkdownHeading() {
        assertEquals("Sprint planı", ai.suggestTitle("Notlar\n# Sprint planı\n- madde"))
    }

    @Test
    fun longFirstSentenceIsShortened() {
        val title = ai.suggestTitle("Bugün markette çok güzel bir indirim vardı ve bir sürü şey aldım sonra eve döndüm")
        assertTrue(title.endsWith("…"))
    }

    @Test
    fun tagsMeetingAndCalendar() {
        val tags = ai.suggestTags("Yarın Ahmet ile toplantı var, sunum hazırlanacak")
        assertTrue(tags.toString(), "toplantı" in tags)
        assertTrue(tags.toString(), "takvim" in tags)
    }

    @Test
    fun explicitHashtagsComeFirst() {
        assertEquals("kitap", ai.suggestTags("#kitap okumaya devam")[0])
    }

    @Test
    fun relatedFindsHomeRepair() {
        val notes = listOf(
            Note(id = 1, title = "Banyo musluğu tamiri", body = "Usta conta ve teflon bant almamı söyledi. Silikon da lazım."),
            Note(id = 2, title = "Makarna tarifi", body = "250 gram penne, domates, fırında pişir."),
        )
        val related = ai.related("Musluk için conta ve silikon almayı unutma", notes)
        assertEquals(1L, related.first().item.id)
    }

    @Test
    fun answerPointsToTheRightNote() {
        val notes = listOf(
            Note(id = 1, title = "Banyo musluğu tamiri", body = "Usta 12'lik conta ve teflon bant almamı söyledi."),
            Note(id = 2, title = "Makarna tarifi", body = "Videodaki tarif: penne, kabak, domates."),
        )
        val answer = ai.answer("İzlediğim videodaki tarif neydi?", notes)
        assertEquals(2L, answer.sources.first().id)
    }
}
