package com.prayagi.netraassistant.assistant

/** One assistant, one voice: Netra One. Warm Hinglish. */
enum class Persona(val displayName: String, val tagline: String) {
    NETRA_ONE("Netra One", "Aapka on-device command assistant");

    fun greeting(): String =
        "Namaste, main Netra One hoon. Battery, volume, brightness poochhiye, ya command dijiye: call, alarm, app kholo, search, SMS."
}
