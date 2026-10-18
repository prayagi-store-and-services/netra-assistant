package com.prayagi.netraassistant.commands

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Visible log of every command Netravan ran (or the user cancelled). In-memory in this beta. */
object ActionLog {
    data class Entry(val time: String, val line: String)

    val entries = mutableStateListOf<Entry>()

    fun add(line: String) {
        val t = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date())
        entries.add(0, Entry(t, line))
        if (entries.size > 200) entries.removeAt(entries.size - 1)
    }
}
