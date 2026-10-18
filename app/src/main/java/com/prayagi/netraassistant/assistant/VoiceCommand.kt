package com.prayagi.netraassistant.assistant

/** Recognises the wake name prefix ("Netravan, battery", "hey Netra volume up"). */
object VoiceCommand {
    data class Parsed(val persona: Persona?, val text: String)

    private val lead = setOf("hey", "hi", "ok", "okay", "he", "हे")
    private val names = setOf("netravan", "netravaan", "netrwaan", "netra", "नेत्रवान", "नेत्रा", "नेत्र")

    private fun tokens(t: String): List<String> =
        t.split(Regex("[^\\p{L}\\p{N}\\p{M}]+")).filter { it.isNotEmpty() }

    fun parse(raw: String): Parsed {
        val tk = tokens(raw.trim().lowercase())
        if (tk.isEmpty()) return Parsed(null, "")
        var i = 0
        if (tk[0] in lead && tk.size > 1 && tk[1] in names) i = 1
        return if (tk[i] in names) {
            Parsed(Persona.NETRAVAN, tk.drop(i + 1).joinToString(" "))
        } else {
            Parsed(null, raw.trim())
        }
    }
}
