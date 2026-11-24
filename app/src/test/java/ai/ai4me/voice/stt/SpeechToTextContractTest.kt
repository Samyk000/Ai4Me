package ai.ai4me.voice.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechToTextContractTest {

    @Test
    fun testSttTranscript_Properties() {
        val partial = SttTranscript(text = "Hello as", isFinal = false, confidence = 0.8f)
        assertFalse(partial.isFinal)
        assertEquals("Hello as", partial.text)

        val finalResult = SttTranscript(text = "Hello assistant", isFinal = true, confidence = 0.96f)
        assertTrue(finalResult.isFinal)
        assertEquals("Hello assistant", finalResult.text)
        assertTrue(finalResult.confidence > 0.9f)
    }
}
