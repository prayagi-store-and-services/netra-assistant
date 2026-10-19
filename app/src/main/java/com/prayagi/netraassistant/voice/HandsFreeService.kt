package com.prayagi.netraassistant.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import com.prayagi.netraassistant.MainActivity
import com.prayagi.netraassistant.actions.DeviceActions
import com.prayagi.netraassistant.actions.Result
import com.prayagi.netraassistant.assistant.IntentParser
import com.prayagi.netraassistant.assistant.Responder
import com.prayagi.netraassistant.assistant.VoiceCommand
import com.prayagi.netraassistant.commands.ActionLog
import com.prayagi.netraassistant.commands.CommandExecutor
import com.prayagi.netraassistant.commands.CommandParser
import com.prayagi.netraassistant.commands.ConfirmWords
import com.prayagi.netraassistant.commands.PhoneCommand

/**
 * Hands-free mode: keeps listening while the screen is off. On by default once mic permission
 * is granted (the user can switch it off; that choice is remembered). Started from the visible
 * app (Android does not allow starting a microphone service from the background), stopped by
 * the notification's Stop button or the switch. Only speech that starts with "Netra One" or
 * "Netra" is acted on - except a bare "haan"/"nahi" while a command confirmation is waiting.
 */
class HandsFreeService : Service() {
    private var engine: VoiceEngine? = null
    private val main = Handler(Looper.getMainLooper())
    private var misses = 0
    private var stopped = false
    private var pending: PhoneCommand? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        if (engine != null) return START_NOT_STICKY
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Hands-free listening", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, HandsFreeService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Netra One is listening")
            .setContentText("Say \"Netra One\" or \"Netra\" first. The microphone is on.")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stop).build())
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)

        val device = DeviceActions(applicationContext)
        val executor = CommandExecutor(applicationContext)
        val e = VoiceEngine(applicationContext)
        engine = e

        fun ask(text: String) {
            HandsFree.state = HandsFree.State.ACTING
            HandsFree.transcript = text
            e.speak(text) {
                HandsFree.state = HandsFree.State.LISTENING
                relisten()
            }
        }

        fun handle(raw: String) {
            val parsed = VoiceCommand.parse(raw)
            val named = parsed.persona != null
            val low = (if (named) parsed.text.ifBlank { "" } else raw).trim().lowercase()
            val pc = pending
            if (pc != null) {
                // While a confirmation waits, a bare haan/nahi (no wake name) also counts.
                when {
                    ConfirmWords.isConfirm(low) -> {
                        pending = null
                        val line = when (val r = executor.run(pc)) {
                            is Result.Value -> r.v
                            is Result.Unavailable -> "Unavailable (${r.reason})"
                        }
                        ActionLog.add("RAN: ${pc.describe()} -> $line")
                        HandsFree.state = HandsFree.State.DONE
                        ask(line)
                    }
                    ConfirmWords.isCancel(low) -> {
                        pending = null
                        ActionLog.add("CANCELLED: ${pc.describe()}")
                        ask("Theek hai, cancel kar diya.")
                    }
                    named -> {
                        ask("Pehle iska jawab dijiye: ${pc.describe()}. Haan ya nahi?")
                    }
                    else -> {
                        HandsFree.lastStatus = "Ignored (confirm pending, no haan/nahi): $raw"
                        relisten()
                    }
                }
                return
            }
            val persona = parsed.persona
            if (!named || persona == null) {
                HandsFree.lastStatus = "Ignored (no Netra One/Netra name): $raw"
                relisten()
                return
            }
            misses = 0
            HandsFree.state = HandsFree.State.HEARD
            HandsFree.transcript = raw
            val q = parsed.text.ifBlank { "hello" }
            val cmd = CommandParser.parse(q)
            if (cmd != null) {
                pending = cmd
                ask("${cmd.describe()}. Chalayen? Haan ya nahi boliye.")
                return
            }
            val reply = Responder.respondAll(persona, IntentParser.parseAll(q), device)
            ask(reply)
        }

        e.onResult = { raw ->
            HandsFree.lastStatus = "Heard: $raw"
            handle(raw)
        }
        e.onGaveUp = {
            misses++
            if (!e.canListen() || misses > 30) {
                HandsFree.lastStatus = "Stopped: voice Unavailable (${e.status})"
                HandsFree.state = HandsFree.State.ERROR
                shutdown()
            } else {
                main.postDelayed({ relisten() }, 1500)
            }
        }
        HandsFree.running = true
        HandsFree.state = HandsFree.State.LISTENING
        HandsFree.lastStatus = "Listening"
        e.startListening()
        return START_NOT_STICKY
    }

    private fun relisten() {
        if (!stopped) {
            engine?.let {
                HandsFree.state = HandsFree.State.LISTENING
                it.startListening()
            }
        }
    }

    private fun shutdown() {
        stopped = true
        main.removeCallbacksAndMessages(null)
        engine?.shutdown()
        engine = null
        pending = null
        HandsFree.running = false
        if (HandsFree.state != HandsFree.State.ERROR) HandsFree.state = HandsFree.State.OFF
        HandsFree.lastStatus = if (HandsFree.state == HandsFree.State.ERROR) HandsFree.lastStatus else "Off"
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (!stopped) shutdown()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.prayagi.netraassistant.STOP_HANDS_FREE"
        private const val CHANNEL = "handsfree"
    }
}
