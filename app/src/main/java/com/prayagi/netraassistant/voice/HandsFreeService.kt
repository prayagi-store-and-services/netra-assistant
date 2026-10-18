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
import com.prayagi.netraassistant.assistant.IntentParser
import com.prayagi.netraassistant.assistant.Persona
import com.prayagi.netraassistant.assistant.Responder
import com.prayagi.netraassistant.assistant.VoiceCommand

/**
 * Hands-free mode: keeps listening while the screen is off. Started by one tap in the app
 * (Android does not allow starting a microphone service from the background), stopped by the
 * notification's Stop button or the switch. Only speech that starts with "Netravan" or "Netra" is acted on.
 */
class HandsFreeService : Service() {
    private var engine: VoiceEngine? = null
    private val main = Handler(Looper.getMainLooper())
    private var misses = 0
    private var stopped = false

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
            .setContentTitle("Netravan is listening")
            .setContentText("Say \"Netravan\" or \"Netra\" first. The microphone is on.")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stop).build())
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)

        val device = DeviceActions(applicationContext)
        val e = VoiceEngine(applicationContext)
        engine = e
        e.onResult = { raw ->
            val parsed = VoiceCommand.parse(raw)
            if (parsed.persona == null) {
                HandsFree.lastStatus = "Ignored (no Netravan/Netra name): $raw"
                relisten()
            } else {
                misses = 0
                val q = parsed.text.ifBlank { "hello" }
                val reply = Responder.respondAll(parsed.persona, IntentParser.parseAll(q), device)
                HandsFree.lastStatus = "Heard: $raw"
                e.speak(reply) { relisten() }
            }
        }
        e.onGaveUp = {
            misses++
            if (!e.canListen() || misses > 30) {
                HandsFree.lastStatus = "Stopped: voice Unavailable (${e.status})"
                shutdown()
            } else {
                main.postDelayed({ relisten() }, 1500)
            }
        }
        HandsFree.running = true
        HandsFree.lastStatus = "Listening"
        e.startListening()
        return START_NOT_STICKY
    }

    private fun relisten() {
        if (!stopped) engine?.startListening()
    }

    private fun shutdown() {
        stopped = true
        main.removeCallbacksAndMessages(null)
        engine?.shutdown()
        engine = null
        HandsFree.running = false
        HandsFree.lastStatus = "Off"
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
