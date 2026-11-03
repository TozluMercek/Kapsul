package com.example.kapsul.data

/**
 * Notes that earlier versions inserted on first launch. They are no longer created;
 * the app removes the ones still in their original form once (an onboarding replaced them).
 */
object LegacySamples {
    private val bodies = listOf(
        """
        # Kapsül'e hoş geldin
        Aklına geleni bırak, gerisini Kapsül halletsin:
        - Başlık ve etiketler **otomatik** atanır
        - [[Banyo musluğu tamiri]] gibi çift köşeli parantezle notları bağla
        - Sağ üstteki ✨ ile hafızana soru sor
        - [ ] İlk notunu yaz
        Hepsi cihazında kalır; hiçbir şey buluta gitmez.
        """,
        """
        Banyo musluğu tamiri. Usta 12'lik conta ve teflon bant almamı söyledi.
        Silikon da lazım, eski silikon küflenmiş. Hırdavatçıdan alınacak:
        - [ ] 12 mm conta
        - [ ] Teflon bant
        - [ ] Şeffaf silikon
        """,
        """
        Videodaki fırında sebzeli makarna tarifi: 250 gram penne, 1 kabak, 2 domates,
        1 soğan, rendelenmiş kaşar. Fırında 200 derecede 20 dakika pişir.
        Sos için 2 kaşık zeytinyağı ve sarımsak.
        """,
        """
        Yarın Ahmet ile proje toplantısı, saat 10:00. Gündem: taslak tasarımın müşteriye sunumu
        ve sprint planı. Deadline'ı konuşmayı unutma.
        """,
        """
        Fikir: ekran görüntülerini otomatik etiketleyen yerel bir arşiv. Belki cihaz üstü OCR ile
        metni çıkarıp notlara bağlamak. Proje taslağına eklenebilir.
        """,
    ).map { normalize(it.trimIndent()) }.toSet()

    /** Ticking a checkbox doesn't count as editing the sample. */
    private fun normalize(body: String) = body.replace("[x]", "[ ]").replace("[X]", "[ ]").trim()

    fun isUntouchedSample(note: Note): Boolean = normalize(note.body) in bodies
}
