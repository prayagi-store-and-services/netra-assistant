package com.prayagi.netraassistant.commands

import com.prayagi.netraassistant.bridge.BridgeContract
import java.util.Calendar

/**
 * Rule-based parser for phone commands, English + Hindi/Hinglish + Devanagari.
 * Returns null when the text is not a command (the chat parser then answers instead).
 */
object CommandParser {

    private val callWords = listOf("call", "कॉल", "फोन करो", "फोन लगाओ")
    private val alarmWords = listOf("alarm", "अलार्म")
    private val openWords = listOf("kholo", "खोलो", "खोलिए", "open", "chalu", "चालू")
    private val searchWords = listOf("search", "सर्च", "khojo", "खोजो", "dhundo", "ढूंढो", "ढूंढिए")
    private val smsWords = listOf("sms", "message", "संदेश", "मैसेज", "एसएमएस")
    private val bridgeWords = listOf("bridge", "ब्रिज")
    private val writeWords = listOf("likho", "लिखो", "लिखिए", "bhejo", "भेजो", "भेजिए", "send", "karo", "करो", "लगाओ", "lagao")

    private fun tokens(t: String): List<String> =
        t.split(Regex("[^\\p{L}\\p{N}\\p{M}:+]+")).filter { it.isNotEmpty() }

    private fun has(tk: List<String>, words: List<String>): Boolean =
        tk.any { tok -> words.any { w -> tok == w || (w.length > 3 && tok.startsWith(w)) } }

    private fun digitsFrom(text: String): String {
        val d = text.filter { it.isDigit() || it == '+' }
        return if (d.count { it.isDigit() } >= 5) d else ""
    }

    /** Parses "6 baje", "6:30", "shaam ke 7", "subah 6" into hour/minutes, or null. */
    fun parseTime(tk: List<String>): Pair<Int, Int>? {
        var hour = -1
        var minute = 0
        var pmGuess: Boolean? = null
        for (i in tk.indices) {
            val t = tk[i]
            val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(t)
            if (m != null) {
                hour = m.groupValues[1].toInt(); minute = m.groupValues[2].toInt()
            } else if (t.matches(Regex("^\\d{1,2}$")) && hour < 0) {
                val n = t.toInt()
                if (n in 0..23) hour = n
            }
            when (t) {
                "baje", "बजे", "o'clock" -> {}
                "pm", "shaam", "शाम", "raat", "रात", "dopahar", "दोपहर" -> pmGuess = true
                "am", "subah", "सुबह", "morning" -> pmGuess = false
                "minute", "मिनट" -> if (hour >= 0 && i + 1 < tk.size && tk[i + 1].matches(Regex("^\\d{1,2}$"))) {
                    minute = tk[i + 1].toInt()
                }
            }
        }
        if (hour < 0) return null
        if (pmGuess == true && hour in 1..11) hour += 12
        if (pmGuess == false && hour == 12) hour = 0
        if (pmGuess == null && hour in 1..6) {
            // Ambiguous small hour: assume the next occurrence in the coming hours.
            val now = Calendar.getInstance()
            val nowH = now.get(Calendar.HOUR_OF_DAY)
            var h24 = hour
            if (nowH >= 12 && h24 < 12) h24 += 12
            if (h24 <= nowH && h24 + 12 <= 23) h24 += 12
            hour = h24
        }
        if (hour > 23 || minute > 59) return null
        return hour to minute
    }

    fun parse(input: String): PhoneCommand? {
        val text = input.trim()
        if (text.isEmpty()) return null
        val low = text.lowercase()
        val tk = tokens(low)

        // Alarm: "alarm lagao 6 baje", "अलार्म 6:30"
        if (has(tk, alarmWords)) {
            val time = parseTime(tk) ?: return null
            return PhoneCommand.SetAlarm(time.first, time.second, "")
        }

        // Call: "call karo 98765...", "X ko call lagao"
        if (has(tk, callWords) || (has(tk, listOf("call")) && has(tk, writeWords))) {
            val num = digitsFrom(text)
            return if (num.isNotEmpty()) PhoneCommand.Call(num) else null
        }

        // SMS: "sms likho 98765... ki ...", "98765... par message bhejo ..."
        if (has(tk, smsWords) && has(tk, writeWords)) {
            val num = digitsFrom(text)
            if (num.isEmpty()) return null
            val idx = low.indexOf(num)
            var body = if (idx >= 0) low.substring(idx + num.length) else ""
            body = body.trim().removePrefix("par").removePrefix("pe").removePrefix("ko")
                .removePrefix("ki").removePrefix("ke").removePrefix(":").trim()
                .removePrefix("yeh").removePrefix("ye").trim()
            return PhoneCommand.Sms(num, body)
        }

        // Bridge to a Netra family app: "bridge eco battery-status", "battery ko bridge low-power bhejo"
        if (has(tk, bridgeWords)) {
            val words = low.replace(Regex("[^\\p{L}\\p{N}\\p{M} -]+"), " ").replace(Regex("\\s+"), " ").trim()
            val toks = words.split(" ").filter { it.isNotEmpty() }
            val drop = bridgeWords + writeWords + listOf("command", "कमांड", "ko", "को", "mein", "में")
            for ((name, pkg) in AppMap.apps) {
                // Match the app name as whole tokens, not a substring: "battery-status" must not match the "battery" app.
                val nameToks = name.split(" ")
                val at = (0..toks.size - nameToks.size).firstOrNull { i ->
                    nameToks.indices.all { j -> toks[i + j] == nameToks[j] }
                }
                if (at != null) {
                    val rest = toks.filterIndexed { i, t -> (i < at || i >= at + nameToks.size) && t !in drop }
                    if (rest.isEmpty()) return null
                    val cmd = rest[0].replace('_', '-')
                    if (!BridgeContract.isValidCommand(cmd)) return null
                    val args = rest.drop(1).joinToString(" ")
                    return PhoneCommand.BridgeSend(name.replaceFirstChar { it.uppercase() }, pkg, cmd, args)
                }
            }
            return null
        }

        // Search: "search karo X", "X khojo"
        if (has(tk, searchWords)) {
            var q = low
            for (w in searchWords + writeWords) q = q.replace(w, " ")
            q = q.replace(Regex("\\s+"), " ").trim()
            return if (q.isNotEmpty()) PhoneCommand.WebSearch(q) else null
        }

        // Open app: "whatsapp kholo", "eco open karo"
        if (has(tk, openWords)) {
            val words = low.replace(Regex("[^\\p{L}\\p{N}\\p{M} ]+"), " ").replace(Regex("\\s+"), " ").trim()
            for ((name, pkg) in AppMap.apps) {
                if (words.contains(name)) return PhoneCommand.OpenApp(name.replaceFirstChar { it.uppercase() }, pkg)
            }
            return null
        }

        return null
    }
}
