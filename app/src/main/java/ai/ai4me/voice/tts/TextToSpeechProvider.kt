package ai.ai4me.voice.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

enum class TtsState {
    IDLE,
    SYNTHESIZING,
    SPEAKING,
    STOPPED,
    ERROR
}

data class TtsConfig(
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.05f,
    val locale: Locale = Locale.getDefault()
)

/**
 * Offline Text-to-Speech Interface (Document 1 §4.3 & Document 2 §8)
 * Supports chunked playback and instant barge-in interruption.
 */
interface TextToSpeechProvider {
    val ttsState: StateFlow<TtsState>
    suspend fun speak(text: String, config: TtsConfig = TtsConfig()): Result<Unit>
    fun stop()
    fun shutdown()
}

class LocalTextToSpeechEngine(
    context: Context
) : TextToSpeechProvider {

    private val _ttsState = MutableStateFlow(TtsState.IDLE)
    override val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = CompletableDeferred<Boolean>()
    private var currentSpeakingJob: CompletableDeferred<Unit>? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isInitialized.complete(true)
            } else {
                isInitialized.complete(false)
                _ttsState.value = TtsState.ERROR
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _ttsState.value = TtsState.SPEAKING
            }

            override fun onDone(utteranceId: String?) {
                _ttsState.value = TtsState.IDLE
                currentSpeakingJob?.complete(Unit)
            }

            override fun onError(utteranceId: String?) {
                _ttsState.value = TtsState.ERROR
                currentSpeakingJob?.complete(Unit)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                _ttsState.value = TtsState.STOPPED
                currentSpeakingJob?.complete(Unit)
            }
        })
    }

    override suspend fun speak(text: String, config: TtsConfig): Result<Unit> = withContext(Dispatchers.Main) {
        val ready = isInitialized.await()
        if (!ready || tts == null) {
            return@withContext Result.failure(Exception("TextToSpeech initialization failed"))
        }

        stop() // Clear any active utterances before new speech

        val engine = tts!!
        engine.setPitch(config.pitch)
        engine.setSpeechRate(config.speechRate)

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val job = CompletableDeferred<Unit>()
        currentSpeakingJob = job

        _ttsState.value = TtsState.SYNTHESIZING
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)

        if (result == TextToSpeech.SUCCESS) {
            job.await()
            Result.success(Unit)
        } else {
            _ttsState.value = TtsState.ERROR
            Result.failure(Exception("TextToSpeech speak failed with code: $result"))
        }
    }

    /**
     * Instant Barge-in Interruption (Document 1 §3.2 & Document 2 §8)
     * Aborts audio playback immediately when user speech begins.
     */
    override fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        currentSpeakingJob?.complete(Unit)
        _ttsState.value = TtsState.IDLE
    }

    override fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
