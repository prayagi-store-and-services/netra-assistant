package com.prayagi.netraassistant.fileassistant

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.collect

/** On-device Gemini Nano via ML Kit. Runs only where the phone reports the model available; download starts only on user tap. */
class NanoAnswerer {
    private var model: GenerativeModel? = null

    private fun client(): GenerativeModel = model ?: Generation.getClient().also { model = it }

    suspend fun status(): Int = try {
        client().checkStatus()
    } catch (t: Throwable) {
        FeatureStatus.UNAVAILABLE
    }

    suspend fun isAvailable(): Boolean = status() == FeatureStatus.AVAILABLE

    suspend fun downloadModel() {
        try {
            client().download().collect { }
        } catch (t: Throwable) {
            // Download failed or unsupported; the screen keeps showing excerpts only.
        }
    }

    suspend fun compose(query: String, passages: List<Passage>): String? {
        val sb = StringBuilder()
        sb.append("Answer the question using ONLY the passages below. If the passages do not contain the answer, say: Is file mein jawab nahin mila. Cite passages as [1], [2].\n\n")
        passages.take(3).forEachIndexed { i, p ->
            sb.append("[").append(i + 1).append("] (").append(p.docName).append(", page ").append(p.page).append(") ").append(p.snippet.trim()).append("\n\n")
        }
        sb.append("Question: ").append(query)
        return try {
            val response = client().generateContent(sb.toString())
            response.candidates.firstOrNull()?.text?.trim()?.takeIf { it.isNotEmpty() }
        } catch (t: Throwable) {
            null
        }
    }
}
