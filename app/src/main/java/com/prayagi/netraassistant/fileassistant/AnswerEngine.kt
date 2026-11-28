package com.prayagi.netraassistant.fileassistant

class AnswerEngine(private val store: DocumentStore, private val nano: NanoAnswerer) {

    data class Answer(val text: String, val citations: List<Passage>, val aiUsed: Boolean)

    fun retrieve(query: String): List<Passage> = store.search(query)

    suspend fun answer(query: String, useAi: Boolean): Answer {
        val passages = retrieve(query)
        if (passages.isEmpty()) return Answer("Is file mein jawab nahin mila.", emptyList(), false)
        if (useAi && nano.isAvailable()) {
            val ai = nano.compose(query, passages)
            if (ai != null) return Answer(ai, passages, true)
        }
        val text = passages.take(3).mapIndexed { i, p ->
            "[" + (i + 1) + "] " + p.snippet.trim() + " (" + p.docName + ", page " + p.page + ")"
        }.joinToString("\n\n")
        return Answer(text, passages, false)
    }
}
