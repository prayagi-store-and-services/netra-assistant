package com.prayagi.netraassistant.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Voice input (speech recognition) and output (text to speech).
 * Recognition is on-device only unless the user explicitly opts in to the online recognizer.
 * Spoken replies never take audio focus or interrupt other media; while any media is
 * playing, the reply is skipped instead of spoken.
 */
class VoiceEngine(private val app: Context) {
    var status by mutableStateOf("Tap the mic and speak.")
    var listening by mutableStateOf(false)
    var speaking by mutableStateOf(false)
    var hindi by mutableStateOf(false)
    var onlineOptIn by mutableStateOf(false)
    var ttsReady by mutableStateOf(false)
    var hindiTts by mutableStateOf(false)

    /** Called on the main thread with the recognized text. */
    var onResult: (String) -> Unit = {}
    /** Called on the main thread when the failure is not worth retrying automatically. */
    var onGaveUp: () -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        tts = TextToSpeech(app) { st ->
            main.post {
                ttsReady = st == TextToSpeech.SUCCESS
                if (ttsReady) {
                    hindiTts = (tts?.isLanguageAvailable(Locale("hi", "IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE
                }
            }
        }
    }

    fun offlineAvailable(): Boolean =
        Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(app)

    fun onlineAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(app)

    /** True when listening can start without asking the user anything more. */
    fun canListen(): Boolean = offlineAvailable() || (onlineOptIn && onlineAvailable())

    fun startListening() {
        if (listening) return
        stopSpeaking()
        destroyRecognizer()
        val r: SpeechRecognizer = when {
            offlineAvailable() -> SpeechRecognizer.createOnDeviceSpeechRecognizer(app)
            onlineOptIn && onlineAvailable() -> SpeechRecognizer.createSpeechRecognizer(app)
            else -> {
                status = "Offline voice: Unavailable on this phone. You can opt in to online voice, or type instead."
                onGaveUp()
                return
            }
        }
        recognizer = r
        r.setRecognitionListener(listener)
        val lang = if (hindi) "hi-IN" else "en-IN"
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, !onlineOptIn || offlineAvailable())
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        status = "Listening (${if (offlineAvailable()) "on this phone" else "online, opted in"}, ${if (hindi) "Hindi" else "English"})..."
        listening = true
        r.startListening(i)
    }

    fun stopListening() {
        listening = false
        recognizer?.stopListening()
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onResults(results: Bundle?) {
            listening = false
            val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            if (t.isEmpty()) {
                status = "Did not catch that. Tap the mic."
                onGaveUp()
            } else {
                status = "Heard: $t"
                onResult(t)
            }
        }
        override fun onError(error: Int) {
            listening = false
            status = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Did not hear anything. Tap the mic."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is off. Allow it in the Permissions tab."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, 11 /* SERVER_DISCONNECTED */ ->
                    "Voice: Unavailable (network error from the online recognizer)."
                12 /* LANGUAGE_NOT_SUPPORTED */, 13 /* LANGUAGE_UNAVAILABLE */ ->
                    "${if (hindi) "Hindi" else "English"} offline voice: Unavailable on this phone (language pack missing)."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice is busy. Try again."
                else -> "Voice: Unavailable (recognizer error $error)."
            }
            onGaveUp()
        }
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        val t = tts
        if (t == null || !ttsReady) {
            status = "Spoken replies: Unavailable (no text-to-speech voice ready on this phone)."
            onDone()
            return
        }
        // Netra rule: never interrupt, pause, duck or take focus from playing media - skip.
        if (mediaPlaying()) {
            status = "Media is playing, so the reply was not spoken."
            onDone()
            return
        }
        t.setLanguage(Locale("en", "IN"))
        speaking = true
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { main.post { finishSpeech(); onDone() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { main.post { finishSpeech(); onDone() } }
        })
        // Recheck at the moment of speech: media may have started just now.
        if (mediaPlaying()) {
            speaking = false
            status = "Media is playing, so the reply was not spoken."
            onDone()
            return
        }
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "netra-reply")
    }

    /**
     * True while any media is actively playing: the music-active flag, or any active playback
     * session with media, game or unknown usage (the system's active playback configuration
     * list holds only players that are playing right now).
     * Fail closed: an unreadable playback state counts as playing, so the reply is skipped.
     */
    private fun mediaPlaying(): Boolean = try {
        audio.isMusicActive || audio.activePlaybackConfigurations.any {
            it.audioAttributes.usage in setOf(
                AudioAttributes.USAGE_MEDIA,
                AudioAttributes.USAGE_GAME,
                AudioAttributes.USAGE_UNKNOWN,
            )
        }
    } catch (e: Exception) {
        true
    }

    private fun finishSpeech() {
        speaking = false
    }

    fun stopSpeaking() {
        tts?.stop()
        if (speaking) finishSpeech()
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    fun shutdown() {
        destroyRecognizer()
        stopSpeaking()
        tts?.shutdown()
        tts = null
    }
}
