package ai.ai4me.voice.wakeword

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WakeWordEngineTest {

    @Test
    fun `initial state is not listening and diagnostics are default`() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val engine = LocalWakeWordDetector(scope = testScope)

        assertFalse(engine.isListening.value)
        assertEquals(0, engine.diagnostics.value.detectionCount)
        assertTrue(engine.diagnostics.value.isEngineReady)
        assertEquals("Hey-Ai4Me-V1-Local", engine.diagnostics.value.modelName)
    }

    @Test
    fun `startListening sets isListening to true`() = runTest {
        val engine = LocalWakeWordDetector(scope = this)

        engine.startListening {}
        assertTrue(engine.isListening.value)

        engine.stopListening()
        assertFalse(engine.isListening.value)
    }

    @Test
    fun `triggerWakeWord increments detection count and fires callback`() = runTest {
        val engine = LocalWakeWordDetector(scope = this)
        var callbackFired = false

        engine.startListening {
            callbackFired = true
        }

        engine.triggerWakeWord {
            callbackFired = true
        }

        assertTrue(callbackFired)
        assertEquals(1, engine.diagnostics.value.detectionCount)

        engine.stopListening()
    }

    @Test
    fun `stopListening cleanly stops background detection job`() = runTest {
        val engine = LocalWakeWordDetector(scope = this)

        engine.startListening {}
        assertTrue(engine.isListening.value)

        engine.stopListening()
        assertFalse(engine.isListening.value)

        // Multiple calls to stopListening should be safe
        engine.stopListening()
        assertFalse(engine.isListening.value)
    }
}
