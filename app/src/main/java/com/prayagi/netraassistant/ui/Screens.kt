package com.prayagi.netraassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import android.Manifest
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import com.prayagi.netraassistant.assistant.VoiceCommand
import com.prayagi.netraassistant.voice.HandsFree
import com.prayagi.netraassistant.voice.HandsFreeService
import com.prayagi.netraassistant.voice.VoiceEngine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayagi.netraassistant.actions.DeviceActions
import com.prayagi.netraassistant.assistant.IntentParser
import com.prayagi.netraassistant.assistant.Persona
import com.prayagi.netraassistant.assistant.Responder

private data class Msg(val fromUser: Boolean, val text: String)

@Composable
fun ChatScreen() {
    val ctx = LocalContext.current
    val device = remember { DeviceActions(ctx.applicationContext) }
    val engine = remember { VoiceEngine(ctx.applicationContext) }
    DisposableEffect(Unit) { onDispose { engine.shutdown() } }
    val persona = Persona.NETRAVAN
    var input by remember { mutableStateOf("") }
    var speakReplies by remember { mutableStateOf(true) }
    var keepListening by remember { mutableStateOf(true) }
    var showOptIn by remember { mutableStateOf(false) }
    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    // One assistant, one conversation (encrypted storage comes later).
    val logs = remember {
        mapOf(
            Persona.NETRAVAN to mutableStateListOf(Msg(false, Persona.NETRAVAN.greeting()))
        )
    }

    fun handle(raw: String) {
        val parsed = VoiceCommand.parse(raw)
        val p = persona
        val q = if (parsed.persona != null) parsed.text.ifBlank { "hello" } else parsed.text
        val log = logs.getValue(p)
        log.add(Msg(true, raw))
        val reply = Responder.respondAll(p, IntentParser.parseAll(q), device)
        log.add(Msg(false, reply))
        if (speakReplies) {
            engine.speak(reply) { if (keepListening && hasMic && !HandsFree.running && engine.canListen()) engine.startListening() }
        } else if (keepListening && hasMic && !HandsFree.running && engine.canListen()) {
            engine.startListening()
        }
    }
    SideEffect { engine.onResult = { handle(it) } }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasMic = ok
        if (ok && engine.canListen()) engine.startListening()
        else if (ok && engine.onlineAvailable()) showOptIn = true
        else if (ok) engine.status = "Offline voice: Unavailable on this phone. Type instead."
        else engine.status = "Microphone permission denied. Voice: Unavailable. You can type instead."
    }
    fun startHandsFree() {
        engine.stopListening(); engine.stopSpeaking()
        ContextCompat.startForegroundService(ctx, Intent(ctx, HandsFreeService::class.java))
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) engine.status = "Notification permission denied: Hands-free runs but its notification may be hidden. Stop it from the app switch."
        startHandsFree()
    }
    LaunchedEffect(Unit) {
        if (hasMic && engine.offlineAvailable()) engine.startListening()
    }

    fun onMic() {
        when {
            !hasMic -> launcher.launch(Manifest.permission.RECORD_AUDIO)
            engine.listening -> engine.stopListening()
            engine.canListen() -> engine.startListening()
            engine.onlineAvailable() -> showOptIn = true
            else -> engine.status = "Voice: Unavailable (no speech recognizer on this phone). Type instead."
        }
    }

    if (showOptIn) {
        AlertDialog(
            onDismissRequest = { showOptIn = false },
            title = { Text("Use online voice?") },
            text = {
                Text(
                    "Offline voice is Unavailable on this phone.\n\n" +
                        "If you allow online voice, the audio of what you say is sent by this phone's speech service to Google's servers to turn it into text. " +
                        "This app itself has no internet access and stores no audio. This choice lasts until you close the app."
                )
            },
            confirmButton = {
                TextButton(onClick = { showOptIn = false; engine.onlineOptIn = true; engine.startListening() }) { Text("Allow online voice") }
            },
            dismissButton = { TextButton(onClick = { showOptIn = false }) { Text("Keep it off") } }
        )
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { onMic() },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        ) {
            Text(
                when {
                    engine.listening -> "Listening... (tap to stop)"
                    engine.speaking -> "Speaking... (tap to talk)"
                    else -> "Tap to speak"
                },
                fontSize = 22.sp
            )
        }
        Text(engine.status, fontSize = 13.sp)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { }) { Text(Persona.NETRAVAN.displayName) }
        OutlinedButton(onClick = { engine.hindi = !engine.hindi }) { Text(if (engine.hindi) "Hindi" else "English") }
    }
    Text("${persona.tagline}. Hands-free mein pehle \"Netravan\" ya \"Netra\" boliye.", fontSize = 12.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Speak replies", fontSize = 13.sp)
        Switch(checked = speakReplies, onCheckedChange = { speakReplies = it; if (!it) engine.stopSpeaking() })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Keep listening after each reply", fontSize = 13.sp)
        Switch(checked = keepListening, onCheckedChange = { keepListening = it })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Hands-free (screen off)", fontSize = 13.sp)
        Switch(checked = HandsFree.running, onCheckedChange = { on ->
            if (on) {
                if (!hasMic) {
                    engine.status = "Allow the microphone first (tap the mic)."
                } else if (!engine.canListen()) {
                    engine.status = "Hands-free: Unavailable until voice works (offline Unavailable on this phone; allow online voice first)."
                } else {
                    val needNotif = Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    if (needNotif) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else startHandsFree()
                }
            } else {
                ctx.startService(Intent(ctx, HandsFreeService::class.java).setAction(HandsFreeService.ACTION_STOP))
            }
        })
    }
    Text(
        "Hands-free keeps the microphone on with a notification and uses battery. Say \"Netravan\" or \"Netra\" first; other speech is ignored. It stops on the notification Stop button, and it does not restart after a phone restart. Status: ${HandsFree.lastStatus}",
        fontSize = 12.sp
    )
    Text(
        "Voice check: offline recognition ${if (engine.offlineAvailable()) "Available" else "Unavailable"}; " +
            "spoken replies ${if (engine.ttsReady) "Available" else "Unavailable"}; Hindi voice ${if (engine.hindiTts) "Available" else "Unavailable"}.",
        fontSize = 12.sp
    )
    logs.getValue(persona).forEach { m ->
        Text((if (m.fromUser) "You: " else persona.displayName + ": ") + m.text)
    }
    // Typing is only a fallback (noisy place, or voice Unavailable).
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text("Type instead (fallback)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedButton(onClick = {
        val q = input.trim()
        if (q.isNotEmpty()) { handle(q); input = "" }
    }) { Text("Send") }
}

@Composable
fun PermissionsScreen() {
    Text("Permissions", fontSize = 18.sp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Microphone (RECORD_AUDIO): asked the first time you tap the mic. Why: to hear your voice commands. Audio is processed by the phone's speech recognizer and this app does not save it.")
        Text("Battery: read through the standard battery broadcast. Why: to answer battery questions.")
        Text("Volume: uses the media volume controls. Why: to read or step volume.")
        Text("Brightness: read-only. Why: to report current brightness. Changing it needs a Settings permission that is not requested yet (Unavailable).")
        Text("Not requested: internet, contacts, location, storage, accessibility.")
    }
}

@Composable
fun PrivacyScreen() {
    Text("Privacy", fontSize = 18.sp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Voice is turned into text on this phone when the phone supports offline recognition. If it does not, voice shows Unavailable.")
        Text("Online voice is OFF unless you allow it in the dialog. If allowed, what you say is sent as audio by the phone's speech service to Google's servers. This app itself has no internet permission and sends nothing.")
        Text("Spoken replies use the phone's text-to-speech engine. Other music or videos pause while the assistant speaks.")
        Text("Messages are not saved after you close the app in this beta.")
        Text("Any future cloud or AI step that sends data will show exactly what is sent and where, and will need your explicit opt-in.")
    }
}
