package ai.ai4me.voice.loop

import ai.ai4me.intelligence.agent.AgentTaskRunner
import ai.ai4me.intelligence.llm.ModelRuntimeManager
import ai.ai4me.intelligence.llm.SamplingParams
import ai.ai4me.ui.components.AssistantState
import ai.ai4me.voice.focus.AudioFocusManager
import ai.ai4me.voice.stt.SpeechToTextProvider
import ai.ai4me.voice.tts.TextToSpeechProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Voice Conversation Coordinator (Document 1 §3.2, §4.3 & Document 2 §8)
 * Connects STT, TTS, AudioFocus, and Local Inference into a continuous loop
 * with instant barge-in interruption.
 */
class VoiceConversationCoordinator(
    private val sttProvider: SpeechToTextProvider,
    private val ttsProvider: TextToSpeechProvider,
    private val audioFocusController: AudioFocusManager,
    private val runtimeManager: ModelRuntimeManager,
    private val orchestrator: AgentTaskRunner,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) {
    private val _voiceState = MutableStateFlow(AssistantState.IDLE)
    val voiceState: StateFlow<AssistantState> = _voiceState.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private var activeVoiceJob: Job? = null

    /**
     * Start speech capture and conversation loop
     */
    fun startVoiceSession() {
        if (_voiceState.value == AssistantState.SPEAKING) {
            // Barge-in: User interrupted assistant speech
            interruptAndListen()
            return
        }

        if (_voiceState.value == AssistantState.LISTENING) {
            stopVoiceSession()
            return
        }

        activeVoiceJob?.cancel()
        activeVoiceJob = scope.launch {
            audioFocusController.requestAudioFocus()
            _voiceState.value = AssistantState.LISTENING
            _currentTranscript.value = "Listening..."

            var finalInput = ""

            try {
                sttProvider.startListening().collect { transcript ->
                    _currentTranscript.value = transcript.text
                    if (transcript.isFinal) {
                        finalInput = transcript.text
                    }
                }

                if (finalInput.isNotBlank()) {
                    processUserInput(finalInput)
                } else {
                    _voiceState.value = AssistantState.IDLE
                    audioFocusController.abandonAudioFocus()
                }
            } catch (e: Exception) {
                _voiceState.value = AssistantState.ERROR
                _currentTranscript.value = "Voice error: ${e.message}"
                audioFocusController.abandonAudioFocus()
            }
        }
    }

    /**
     * Instant Barge-In Interruption (Document 1 §3.2 & Document 2 §8)
     * Aborts TTS immediately and switches directly into listening mode.
     */
    fun interruptAndListen() {
        ttsProvider.stop()
        runtimeManager.cancelGeneration()
        audioFocusController.abandonAudioFocus()

        _voiceState.value = AssistantState.IDLE
        startVoiceSession()
    }

    fun stopVoiceSession() {
        activeVoiceJob?.cancel()
        activeVoiceJob = null
        sttProvider.stopListening()
        ttsProvider.stop()
        runtimeManager.cancelGeneration()
        audioFocusController.abandonAudioFocus()
        _voiceState.value = AssistantState.IDLE
        _currentTranscript.value = ""
    }

    private suspend fun processUserInput(input: String) = withContext(Dispatchers.IO) {
        _voiceState.value = AssistantState.PROCESSING
        _currentTranscript.value = "Processing: \"$input\""

        val isTask = input.contains("status", ignoreCase = true) ||
                input.contains("remember", ignoreCase = true) ||
                input.contains("notification", ignoreCase = true)

        val responseText = if (isTask) {
            val task = orchestrator.executeTask(input, runtimeManager)
            task.summary ?: "Task completed."
        } else {
            var accumulated = ""
            runtimeManager.generateStream(input, SamplingParams()).collect { chunk ->
                accumulated += chunk.token
            }
            if (accumulated.isBlank()) {
                "I am here and operating offline. What would you like me to do?"
            } else accumulated
        }

        speakResponse(responseText)
    }

    private suspend fun speakResponse(text: String) {
        _voiceState.value = AssistantState.SPEAKING
        _currentTranscript.value = text
        audioFocusController.requestAudioFocus()

        val result = ttsProvider.speak(text)
        if (result.isFailure) {
            _voiceState.value = AssistantState.ERROR
        } else {
            _voiceState.value = AssistantState.IDLE
        }
        audioFocusController.abandonAudioFocus()
    }
}
