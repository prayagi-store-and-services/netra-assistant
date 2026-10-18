package com.prayagi.netraassistant.commands

/** A phone action Netra One can run after the user confirms it. */
sealed class PhoneCommand {
    abstract fun describe(): String

    /** Opens the dialer with the number filled in. The user still taps the call button. */
    data class Call(val number: String) : PhoneCommand() {
        override fun describe() = "Dialer khulega: $number (call aap khud dabayenge)"
    }

    /** Sets an alarm in the phone's clock app. */
    data class SetAlarm(val hour: Int, val minutes: Int, val label: String) : PhoneCommand() {
        override fun describe(): String {
            val h12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val ampm = if (hour < 12) "AM" else "PM"
            return "Alarm set hoga: $h12:%02d $ampm".format(minutes) + if (label.isNotBlank()) " ($label)" else ""
        }
    }

    /** Opens another app by name. */
    data class OpenApp(val appName: String, val packageId: String) : PhoneCommand() {
        override fun describe() = "$appName khulega"
    }

    /** Opens the browser with a web search. The browser uses the internet, not this app. */
    data class WebSearch(val query: String) : PhoneCommand() {
        override fun describe() = "Browser mein search khulega: $query"
    }

    /** Opens the SMS composer with number and message filled in. The user taps send. */
    data class Sms(val number: String, val body: String) : PhoneCommand() {
        override fun describe() = "SMS khulega: $number ko" + if (body.isNotBlank()) ", likha hua: $body" else " (khali)"
    }
}
