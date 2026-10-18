package com.prayagi.netraassistant.assistant

/** One assistant, one voice: Netravan. Warm Hinglish. */
enum class Persona(val displayName: String, val tagline: String) {
    NETRAVAN("Netravan", "Aapka on-device command assistant");

    fun greeting(): String =
        "Namaste, main Netravan hoon. Battery, volume, brightness poochhiye, ya command dijiye: call, alarm, app kholo, search, SMS."
}
