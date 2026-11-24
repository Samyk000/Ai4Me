package ai.ai4me.intelligence.llm

import ai.ai4me.intelligence.gguf.GgufHeaderInfo
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelRuntimeManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createValidDummyGgufFile(name: String): java.io.File {
        val file = tempFolder.newFile(name)
        FileOutputStream(file).use { fos ->
            val buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(0x46554747) // GGUF magic
            buffer.putInt(3)
            buffer.putLong(64L)
            buffer.putLong(0L)
            fos.write(buffer.array())
        }
        return file
    }

    @Test
    fun testLoadAndUnloadLifecycle() = runTest {
        val manager = ModelRuntimeManager()
        val dummyFile = createValidDummyGgufFile("test_model.gguf")

        assertEquals(ModelRuntimeState.READY, manager.runtimeState.value)
        assertNull(manager.loadedModelFile)

        val loadResult = manager.loadModel(dummyFile, RuntimeConfig())
        assertTrue(loadResult.isSuccess)
        assertEquals(ModelRuntimeState.LOADED, manager.runtimeState.value)
        assertNotNull(manager.loadedModelFile)

        val unloadResult = manager.unloadModel()
        assertTrue(unloadResult.isSuccess)
        assertEquals(ModelRuntimeState.READY, manager.runtimeState.value)
        assertNull(manager.loadedModelFile)
    }

    @Test
    fun testTokenStreaming_EmitsChunksAndMetrics() = runTest {
        val manager = ModelRuntimeManager()
        val dummyFile = createValidDummyGgufFile("streaming_model.gguf")
        manager.loadModel(dummyFile, RuntimeConfig())

        val chunks = manager.generateStream("Hello assistant", SamplingParams()).toList()
        assertTrue(chunks.isNotEmpty())

        val lastChunk = chunks.last()
        assertTrue(lastChunk.isFinished)
        assertEquals("stop", lastChunk.finishReason)
        assertEquals(ModelRuntimeState.LOADED, manager.runtimeState.value)
    }

    @Test
    fun testCancellation_StopsStreamImmediately() = runTest {
        val manager = ModelRuntimeManager()
        val dummyFile = createValidDummyGgufFile("cancel_model.gguf")
        manager.loadModel(dummyFile, RuntimeConfig())

        manager.cancelGeneration()
        val chunks = manager.generateStream("Tell me a long story", SamplingParams()).toList()
        val lastChunk = chunks.lastOrNull()

        assertTrue(lastChunk != null && lastChunk.isFinished)
        assertEquals("cancelled", lastChunk?.finishReason)
    }
}
