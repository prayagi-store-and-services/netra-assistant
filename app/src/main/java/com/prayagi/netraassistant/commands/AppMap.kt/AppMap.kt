package com.prayagi.netraassistant.commands

/** Known apps Netravan can open by name. Netra family first, then common apps. */
object AppMap {
    val apps: Map<String, String> = linkedMapOf(
        // Netra family
        "eco" to "com.prayagi.netraeco",
        "ecosystem" to "com.prayagi.netraeco",
        "battery" to "com.aistudio.batterysentinel.ntra",
        "bspn" to "com.aistudio.batterysentinel.ntra",
        "sentinel" to "com.aistudio.batterysentinel.ntra",
        "hub" to "com.aistudio.netrasensorhub.kxmpzq",
        "safety" to "com.aistudio.netrasensorhub.kxmpzq",
        "kbc" to "com.aistudio.reasoningquiz.tark",
        "quiz" to "com.aistudio.reasoningquiz.tark",
        "privacy" to "com.aistudio.sensorguard.prvsec",
        "sensorguard" to "com.aistudio.sensorguard.prvsec",
        "player" to "com.prayagi.netraplayer",
        // Common apps
        "whatsapp" to "com.whatsapp",
        "camera" to "com.android.camera2",
        "settings" to "com.android.settings",
        "chrome" to "com.android.chrome",
        "youtube" to "com.google.android.youtube",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm",
        "photos" to "com.google.android.apps.photos",
        "play store" to "com.android.vending",
        "playstore" to "com.android.vending",
        "phone" to "com.google.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "calculator" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock",
        "files" to "com.google.android.apps.nbu.files"
    )
}
