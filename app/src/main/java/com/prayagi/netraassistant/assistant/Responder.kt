package com.prayagi.netraassistant.assistant

import com.prayagi.netraassistant.actions.BatteryInfo
import com.prayagi.netraassistant.actions.BatteryTemp
import com.prayagi.netraassistant.actions.BatteryTime
import com.prayagi.netraassistant.actions.DeviceActions
import com.prayagi.netraassistant.actions.Result
import com.prayagi.netraassistant.actions.VolumeInfo

object Responder {
    fun respondAll(persona: Persona, intents: List<Intent>, device: DeviceActions): String =
        intents.joinToString("\n") { respond(persona, it, device) }

    fun respond(persona: Persona, intent: Intent, device: DeviceActions): String = when (intent) {
        Intent.BatteryLevel -> batteryText(persona, device.battery())
        Intent.BatteryTemp -> tempText(persona, device.batteryTemp())
        Intent.BatteryTime -> timeText(persona, device.batteryTime())
        Intent.Greeting -> greeting(persona)
        Intent.Thanks -> "Aapka swagat hai."
        Intent.Help -> help(persona)
        Intent.VolumeGet -> volumeText(persona, device.volume())
        Intent.VolumeUp -> volumeText(persona, device.volumeStep(true))
        Intent.VolumeDown -> volumeText(persona, device.volumeStep(false))
        Intent.BrightnessGet -> brightnessText(persona, device.brightnessPct())
        Intent.Unknown -> unknown(persona)
    }

    fun batteryText(p: Persona, r: Result<BatteryInfo>): String = when (r) {
        is Result.Value ->
            "Battery ${r.v.levelPct}% hai, ${if (r.v.charging) "charge ho rahi hai" else "charge nahi ho rahi"}."
        is Result.Unavailable -> "Battery: Unavailable (${r.reason})."
    }

    fun volumeText(p: Persona, r: Result<VolumeInfo>): String = when (r) {
        is Result.Value -> "Media volume ${r.v.current} / ${r.v.max} hai."
        is Result.Unavailable -> "Volume: Unavailable (${r.reason})."
    }

    fun brightnessText(p: Persona, r: Result<Int>): String = when (r) {
        is Result.Value -> "Brightness lagbhag ${r.v}% hai. Badalna abhi is beta mein Unavailable hai."
        is Result.Unavailable -> "Brightness: Unavailable (${r.reason})."
    }

    fun unknown(p: Persona): String =
        "Maaf kijiye, abhi main sirf battery, volume aur brightness samajhta hoon."

    fun tempText(p: Persona, r: Result<BatteryTemp>): String = when (r) {
        is Result.Value -> "Battery ka temperature ${"%.1f".format(r.v.celsius)} C hai."
        is Result.Unavailable -> "Temperature: Unavailable (${r.reason})."
    }

    fun timeText(p: Persona, r: Result<BatteryTime>): String = when (r) {
        is Result.Value -> {
            val h = "%.1f".format(r.v.hours)
            "Abhi ke upyog (lagbhag ${r.v.basedOnMa} mA) ke hisab se lagbhag $h ghante chalega. Yeh sirf andaza hai."
        }
        is Result.Unavailable -> "Battery time: Unavailable (${r.reason})."
    }

    fun greeting(p: Persona): String =
        "Namaste! Main aapki kya madad kar sakta hoon? Battery/volume poochhiye ya command dijiye: call, alarm, app kholo, search, SMS."

    fun help(p: Persona): String =
        "Main bata sakta hoon: battery level, temperature, battery kitne ghante chalegi, volume (badhana/ghatana), aur brightness. Aur commands: call karo, alarm lagao, app kholo, search karo, SMS likho - har command pehle aapki haan maangega."
}
