package com.prayagi.netraassistant.assistant

/** One assistant, one voice: Netravan. Warm Hinglish. */
enum class Persona(val displayName: String, val tagline: String) {
    NETRAVAN("Netravan", "Aapka on-device command assistant");

    fun greeting(): String =
        "Namaste, main Netravan hoon. Battery, volume ya brightness ke baare mein poochhiye."
}
