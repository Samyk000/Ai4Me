package ai.ai4me.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

enum class SttState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    ERROR
}

data class SttTranscript(
    val text: String,
    val isFinal: Boolean,
    val confidence: Float = 1.0f
)

/**
 * Offline Speech-to-Text Interface (Document 1 §4.3 & Document 2 §8)
 * Emits partial and final speech recognition results with zero network dependency.
 */
interface SpeechToTextProvider {
    val sttState: StateFlow<SttState>
    fun startListening(): Flow<SttTranscript>
    fun stopListening()
    fun cancel()
}

class LocalSpeechRecognizer(
    private val context: Context
) : SpeechToTextProvider {

    private val _sttState = MutableStateFlow(SttState.IDLE)
    override val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    override fun startListening(): Flow<SttTranscript> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _sttState.value = SttState.ERROR
            close(Exception("Speech recognition is not available on this device"))
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _sttState.value = SttState.LISTENING
            }

            override fun onBeginningOfSpeech() {
                _sttState.value = SttState.TRANSCRIBING
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _sttState.value = SttState.IDLE
            }

            override fun onError(error: Int) {
                _sttState.value = SttState.ERROR
                trySend(SttTranscript(text = "", isFinal = true, confidence = 0f))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalTranscript = matches?.firstOrNull().orEmpty()
                _sttState.value = SttState.IDLE
                trySend(SttTranscript(text = finalTranscript, isFinal = true, confidence = 0.95f))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull().orEmpty()
                if (partialText.isNotBlank()) {
                    trySend(SttTranscript(text = partialText, isFinal = false, confidence = 0.8f))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Document 1 §7 & Document 2 §8: Strictly prefer on-device offline recognition
            putExtra("android.speech.extra.PREFER_OFFLINE", true)
        }

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            _sttState.value = SttState.ERROR
            close(e)
        }

        awaitClose {
            try {
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            _sttState.value = SttState.IDLE
        }
    }

    override fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _sttState.value = SttState.IDLE
    }

    override fun cancel() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _sttState.value = SttState.IDLE
    }
}
