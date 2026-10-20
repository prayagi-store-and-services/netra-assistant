package com.prayagi.netraassistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.prayagi.netraassistant.actions.DeviceActions
import com.prayagi.netraassistant.actions.Result
import com.prayagi.netraassistant.assistant.IntentParser
import com.prayagi.netraassistant.assistant.Persona
import com.prayagi.netraassistant.assistant.Responder
import com.prayagi.netraassistant.assistant.VoiceCommand
import com.prayagi.netraassistant.commands.ActionLog
import com.prayagi.netraassistant.commands.CommandExecutor
import com.prayagi.netraassistant.commands.CommandParser
import com.prayagi.netraassistant.commands.ConfirmWords
import com.prayagi.netraassistant.commands.PhoneCommand
import com.prayagi.netraassistant.voice.HandsFree
import com.prayagi.netraassistant.voice.HandsFreePrefs
import com.prayagi.netraassistant.voice.HandsFreeService
import com.prayagi.netraassistant.voice.VoiceEngine

private data class Msg(val fromUser: Boolean, val text: String)

private fun ringColor(s: HandsFree.State): Color = when (s) {
    HandsFree.State.OFF -> Color(0xFF9E9E9E)
    HandsFree.State.LISTENING -> Color(0xFF42A5F5)
    HandsFree.State.HEARD -> Color(0xFF00E5FF)
    HandsFree.State.ACTING -> Color(0xFFFFB300)
    HandsFree.State.DONE -> Color(0xFF66BB6A)
    HandsFree.State.ERROR -> Color(0xFFEF5350)
}

private fun ringText(s: HandsFree.State): String = when (s) {
    HandsFree.State.OFF -> "Mic band hai"
    HandsFree.State.LISTENING -> "Sun raha hoon - \"Netra One\" boliye"
    HandsFree.State.HEARD -> "Sun liya"
    HandsFree.State.ACTING -> "Kaam kar raha hoon"
    HandsFree.State.DONE -> "Ho gaya"
    HandsFree.State.ERROR -> "Voice Unavailable"
}

/** The one big state of the app. Tap toggles listening - it does real work. */
@Composable
private fun StatusRing(state: HandsFree.State, onTap: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "ring")
    val breath by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "breath"
    )
    val a = if (state == HandsFree.State.LISTENING) breath else 1f
    val c = ringColor(state)
    Canvas(Modifier.size(170.dp).clickable(onClick = onTap)) {
        drawCircle(c.copy(alpha = 0.22f * a))
        drawCircle(c.copy(alpha = a), radius = size.minDimension / 2f * 0.6f)
        drawCircle(Color.White.copy(alpha = 0.85f), radius = size.minDimension / 2f * 0.18f)
    }
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val device = remember { DeviceActions(ctx.applicationContext) }
    val executor = remember { CommandExecutor(ctx.applicationContext) }
    val engine = remember { VoiceEngine(ctx.applicationContext) }
    val prefs = remember { HandsFreePrefs(ctx) }
    DisposableEffect(Unit) { onDispose { engine.shutdown() } }
    val persona = Persona.NETRA_ONE
    var input by remember { mutableStateOf("") }
    var speakReplies by remember { mutableStateOf(true) }
    var keepListening by remember { mutableStateOf(true) }
    var showOptIn by remember { mutableStateOf(false) }
    var pendingCommand by remember { mutableStateOf<PhoneCommand?>(null) }
    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val logs = remember {
        mapOf(Persona.NETRA_ONE to mutableStateListOf(Msg(false, Persona.NETRA_ONE.greeting())))
    }

    fun speakThen(reply: String) {
        if (speakReplies) {
            engine.speak(reply) { if (keepListening && hasMic && !HandsFree.running && engine.canListen()) engine.startListening() }
        } else if (keepListening && hasMic && !HandsFree.running && engine.canListen()) {
            engine.startListening()
        }
    }

    fun handle(raw: String) {
        val parsed = VoiceCommand.parse(raw)
        val p = persona
        val q = if (parsed.persona != null) parsed.text.ifBlank { "hello" } else parsed.text
        val log = logs.getValue(p)
        log.add(Msg(true, raw))
        val low = q.trim().lowercase()
        val pending = pendingCommand
        if (pending != null) {
            when {
                ConfirmWords.isConfirm(low) -> {
                    pendingCommand = null
                    val line = when (val r = executor.run(pending)) {
                        is Result.Value -> r.v
                        is Result.Unavailable -> "Unavailable (${r.reason})"
                    }
                    ActionLog.add("RAN: ${pending.describe()} -> $line")
                    log.add(Msg(false, line))
                    speakThen(line)
                }
                ConfirmWords.isCancel(low) -> {
                    pendingCommand = null
                    ActionLog.add("CANCELLED: ${pending.describe()}")
                    val line = "Theek hai, cancel kar diya."
                    log.add(Msg(false, line))
                    speakThen(line)
                }
                else -> {
                    val line = "Pehle iska jawab dijiye: ${pending.describe()}. Haan ya nahi?"
                    log.add(Msg(false, line))
                    speakThen(line)
                }
            }
            return
        }
        val cmd = CommandParser.parse(q)
        if (cmd != null) {
            pendingCommand = cmd
            val line = "${cmd.describe()}. Chalayen? Haan ya nahi boliye."
            log.add(Msg(false, line))
            speakThen(line)
            return
        }
        val reply = Responder.respondAll(p, IntentParser.parseAll(q), device)
        log.add(Msg(false, reply))
        speakThen(reply)
    }
    SideEffect { engine.onResult = { handle(it) } }

    fun startHandsFree() {
        engine.stopListening(); engine.stopSpeaking()
        prefs.setEnabled(true)
        ContextCompat.startForegroundService(ctx, Intent(ctx, HandsFreeService::class.java))
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) engine.status = "Notification permission off: hands-free chalega, par notification shayad na dikhe. Yahi se Stop kar sakte hain."
        startHandsFree()
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasMic = ok
        if (!ok) engine.status = "Microphone permission denied. Voice: Unavailable. Type kar sakte hain."
    }

    // Default ON: mic permission milte hi hands-free listening khud shuru (user ne band na kiya ho to).
    LaunchedEffect(hasMic) {
        if (hasMic && prefs.enabled() && !HandsFree.running && engine.canListen()) {
            val needNotif = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needNotif) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else startHandsFree()
        }
    }

    val ring = when {
        HandsFree.running -> HandsFree.state
        engine.speaking -> HandsFree.State.ACTING
        engine.listening -> HandsFree.State.LISTENING
        else -> HandsFree.State.OFF
    }

    fun onRingTap() {
        when {
            HandsFree.running -> ctx.startService(Intent(ctx, HandsFreeService::class.java).setAction(HandsFreeService.ACTION_STOP)).also { prefs.setEnabled(false) }
            !hasMic -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            engine.listening -> engine.stopListening()
            engine.canListen() -> engine.startListening()
            engine.onlineAvailable() -> showOptIn = true
            else -> engine.status = "Voice: Unavailable (is phone par speech recognizer nahi). Type instead."
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

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusRing(ring) { onRingTap() }
        Text(ringText(ring), fontSize = 16.sp)
        Text(
            if (HandsFree.running) HandsFree.transcript.ifBlank { HandsFree.lastStatus } else engine.status,
            fontSize = 13.sp
        )
    }

    pendingCommand?.let { pc ->
        Text("Command confirm karein (boliye ya dabaiye):", fontSize = 13.sp)
        Text(pc.describe(), fontSize = 16.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                pendingCommand = null
                val line = when (val r = executor.run(pc)) {
                    is Result.Value -> r.v
                    is Result.Unavailable -> "Unavailable (${r.reason})"
                }
                ActionLog.add("RAN: ${pc.describe()} -> $line")
                logs.getValue(persona).add(Msg(false, line))
                speakThen(line)
            }) { Text("Haan") }
            OutlinedButton(onClick = {
                pendingCommand = null
                ActionLog.add("CANCELLED: ${pc.describe()}")
                val line = "Theek hai, cancel kar diya."
                logs.getValue(persona).add(Msg(false, line))
                speakThen(line)
            }) { Text("Nahi") }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Hands-free listening (wake name)", fontSize = 13.sp)
        Switch(checked = HandsFree.running, onCheckedChange = { on ->
            if (on) {
                if (!hasMic) {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else if (!engine.canListen()) {
                    engine.status = "Hands-free: Unavailable jab tak voice kaam na kare (offline is phone par Unavailable; online voice allow karein)."
                } else {
                    val needNotif = Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    if (needNotif) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else startHandsFree()
                }
            } else {
                prefs.setEnabled(false)
                ctx.startService(Intent(ctx, HandsFreeService::class.java).setAction(HandsFreeService.ACTION_STOP))
            }
        })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Speak replies", fontSize = 13.sp)
        Switch(checked = speakReplies, onCheckedChange = { speakReplies = it; if (!it) engine.stopSpeaking() })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Keep listening after each reply", fontSize = 13.sp)
        Switch(checked = keepListening, onCheckedChange = { keepListening = it })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { engine.hindi = !engine.hindi }) { Text(if (engine.hindi) "Hindi" else "English") }
    }
    Text(
        "Hands-free mic on rakhta hai (notification mein Stop button) aur battery use karta hai. \"Netra One\" ya \"Netra\" bole bina baat ignore hoti hai. Phone restart ke baad khud nahi chalti - app kholte hi wapas chalu ho jaati hai. Status: ${HandsFree.lastStatus}",
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
fun LogScreen() {
    Text("Command Log", fontSize = 18.sp)
    if (ActionLog.entries.isEmpty()) {
        Text("Abhi tak koi command nahi chala. Commands: call, alarm, app kholo, search, SMS - har ek pehle aapki haan maangega.")
    }
    ActionLog.entries.forEach { e ->
        Text("${e.time} - ${e.line}", fontSize = 13.sp)
    }
    Text("Yeh log sirf is session ke liye hai; app band hone par mit jaata hai.", fontSize = 12.sp)
}

@Composable
fun PermissionsScreen() {
    Text("Permissions", fontSize = 18.sp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Microphone (RECORD_AUDIO): asked the first time you tap the ring. Why: to hear your voice commands. Audio is processed by the phone's speech recognizer and this app does not save it.")
        Text("Hands-free (FOREGROUND_SERVICE_MICROPHONE): ON by default once mic permission is granted, so Netra One answers without any tap. Switch it off here anytime; your choice is remembered. A notification with a Stop button shows while the mic is on.")
        Text("Notifications (POST_NOTIFICATIONS, Android 13+): asked so the hands-free notification can show. If denied, hands-free still runs and you stop it from the app.")
        Text("Battery: read through the standard battery broadcast. Why: to answer battery questions.")
        Text("Volume: uses the media volume controls. Why: to read or step volume.")
        Text("Brightness: read-only. Why: to report current brightness. Changing it needs a Settings permission that is not requested yet (Unavailable).")
        Text("Alarms (SET_ALARM): lets the clock app set an alarm you dictate. Granted at install, no prompt.")
        Text("Not requested: internet, contacts, location, storage, accessibility.")
    }
}

@Composable
fun PrivacyScreen() {
    Text("Privacy", fontSize = 18.sp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Voice is turned into text on this phone when the phone supports offline recognition. If it does not, voice shows Unavailable.")
        Text("Online voice is OFF unless you allow it in the dialog. If allowed, what you say is sent as audio by the phone's speech service to Google's servers. This app itself has no internet permission and sends nothing.")
        Text("Hands-free listens continuously while it is on, but only speech that starts with \"Netra One\" or \"Netra\" is acted on (plus a bare haan/nahi while a command confirmation waits). Everything else is discarded by the recognizer and never saved.")
        Text("Spoken replies use the phone's text-to-speech engine. Other music or videos pause while the assistant speaks.")
        Text("Messages are not saved after you close the app in this beta. Your hands-free on/off choice is the only setting kept on the phone.")
        Text("Commands (call, alarm, open app, search, SMS) only open the phone's own apps, and only after you confirm. Netra One sends nothing itself; the dialer, SMS app or browser then do their own work.")
        Text("The command log lives only until you close the app.")
        Text("Any future cloud or AI step that sends data will show exactly what is sent and where, and will need your explicit opt-in.")
    }
}
