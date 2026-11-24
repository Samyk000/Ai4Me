package ai.ai4me.voice.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextToSpeechContractTest {

    @Test
    fun testTtsConfig_Defaults() {
        val config = TtsConfig()
        assertEquals(1.0f, config.pitch, 0.01f)
        assertEquals(1.05f, config.speechRate, 0.01f)
        assertTrue(config.locale != null)
    }
}
