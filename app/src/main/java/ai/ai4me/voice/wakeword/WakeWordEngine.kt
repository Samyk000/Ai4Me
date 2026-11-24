package ai.ai4me.voice.wakeword

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WakeWordDiagnostics(
    val detectionCount: Int = 0,
    val falsePositiveResets: Int = 0,
    val isEngineReady: Boolean = true,
    val modelName: String = "Hey-Ai4Me-V1-Local"
)

/**
 * Local Wake-Word Engine Interface (Document 1 §3.2 & Document 2 §8)
 * Operates purely locally on low power without invoking heavy LLMs.
 */
interface WakeWordEngine {
    val isListening: StateFlow<Boolean>
    val diagnostics: StateFlow<WakeWordDiagnostics>

    fun startListening(onWakeWordDetected: () -> Unit)
    fun stopListening()
}

class LocalWakeWordDetector(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) : WakeWordEngine {

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _diagnostics = MutableStateFlow(WakeWordDiagnostics())
    override val diagnostics: StateFlow<WakeWordDiagnostics> = _diagnostics.asStateFlow()

    private var detectionJob: Job? = null

    override fun startListening(onWakeWordDetected: () -> Unit) {
        if (_isListening.value) return

        _isListening.value = true
        detectionJob = scope.launch {
            // Low-power background framing simulation for keyword "Hey Ai4Me"
            while (_isListening.value) {
                delay(1000)
                // Keyword spotting runs without network or LLM dependency
            }
        }
    }

    override fun stopListening() {
        _isListening.value = false
        detectionJob?.cancel()
        detectionJob = null
    }

    /**
     * Simulated detection event trigger for testing or system audio triggers
     */
    fun triggerWakeWord(callback: () -> Unit) {
        _diagnostics.value = _diagnostics.value.copy(
            detectionCount = _diagnostics.value.detectionCount + 1
        )
        callback()
    }
}
