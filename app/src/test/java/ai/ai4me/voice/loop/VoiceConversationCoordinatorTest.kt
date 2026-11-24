package ai.ai4me.voice.loop

import ai.ai4me.data.db.DbTask
import ai.ai4me.intelligence.agent.AgentExecutionState
import ai.ai4me.intelligence.agent.AgentTaskRunner
import ai.ai4me.intelligence.llm.LLMProvider
import ai.ai4me.intelligence.llm.ModelRuntimeManager
import ai.ai4me.ui.components.AssistantState
import ai.ai4me.voice.focus.AudioFocusManager
import ai.ai4me.voice.stt.SpeechToTextProvider
import ai.ai4me.voice.stt.SttState
import ai.ai4me.voice.stt.SttTranscript
import ai.ai4me.voice.tts.TextToSpeechProvider
import ai.ai4me.voice.tts.TtsConfig
import ai.ai4me.voice.tts.TtsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceConversationCoordinatorTest {

    private val dummyStt = object : SpeechToTextProvider {
        private val _state = MutableStateFlow(SttState.IDLE)
        override val sttState: StateFlow<SttState> = _state
        override fun startListening(): Flow<SttTranscript> = emptyFlow()
        override fun stopListening() { _state.value = SttState.IDLE }
        override fun cancel() { _state.value = SttState.IDLE }
    }

    private val dummyTts = object : TextToSpeechProvider {
        private val _state = MutableStateFlow(TtsState.IDLE)
        override val ttsState: StateFlow<TtsState> = _state
        var stopCalled = false

        override suspend fun speak(text: String, config: TtsConfig): Result<Unit> {
            _state.value = TtsState.SPEAKING
            return Result.success(Unit)
        }

        override fun stop() {
            stopCalled = true
            _state.value = TtsState.IDLE
        }

        override fun shutdown() {}
    }

    private val dummyFocus = object : AudioFocusManager {
        override fun requestAudioFocus(): Boolean = true
        override fun abandonAudioFocus() {}
    }

    private val dummyRunner = object : AgentTaskRunner {
        override val executionState: StateFlow<AgentExecutionState> = MutableStateFlow(AgentExecutionState())
        override suspend fun executeTask(goal: String, llmProvider: LLMProvider): DbTask {
            return DbTask(id = "1", goal = goal, status = "completed", createdAt = 0L, updatedAt = 0L)
        }
    }

    @Test
    fun testInitialState_IsIdle() {
        val coordinator = VoiceConversationCoordinator(
            sttProvider = dummyStt,
            ttsProvider = dummyTts,
            audioFocusController = dummyFocus,
            runtimeManager = ModelRuntimeManager(),
            orchestrator = dummyRunner
        )

        assertEquals(AssistantState.IDLE, coordinator.voiceState.value)
        assertEquals("", coordinator.currentTranscript.value)
    }

    @Test
    fun testBargeInInterruption_StopsTtsAndCancels() {
        val runtimeManager = ModelRuntimeManager()
        val coordinator = VoiceConversationCoordinator(
            sttProvider = dummyStt,
            ttsProvider = dummyTts,
            audioFocusController = dummyFocus,
            runtimeManager = runtimeManager,
            orchestrator = dummyRunner
        )

        assertFalse(dummyTts.stopCalled)
        coordinator.interruptAndListen()
        assertTrue(dummyTts.stopCalled)
    }

    @Test
    fun testStopSession_ClearsTranscript() {
        val coordinator = VoiceConversationCoordinator(
            sttProvider = dummyStt,
            ttsProvider = dummyTts,
            audioFocusController = dummyFocus,
            runtimeManager = ModelRuntimeManager(),
            orchestrator = dummyRunner
        )

        coordinator.stopVoiceSession()
        assertEquals(AssistantState.IDLE, coordinator.voiceState.value)
        assertEquals("", coordinator.currentTranscript.value)
    }
}
