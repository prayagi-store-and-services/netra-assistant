package com.prayagi.netraassistant.voice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared state of the hands-free service, read by the UI (status ring + text). */
object HandsFree {
    enum class State { OFF, LISTENING, HEARD, ACTING, DONE, ERROR }

    var running by mutableStateOf(false)
    var state by mutableStateOf(State.OFF)
    var lastStatus by mutableStateOf("Off")
    /** What Netra One last heard or asked - shown as the live transcript line. */
    var transcript by mutableStateOf("")
}
