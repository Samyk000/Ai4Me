package ai.ai4me.hardware.benchmark

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareBenchmarkRunnerTest {

    private val runner = HardwareBenchmarkRunner()

    @Test
    fun `benchmark executes and returns valid performance metrics`() = runTest {
        val metrics = runner.runBenchmark("SmolLM2-1.7B-Q4_K_M.gguf")

        assertEquals("SmolLM2-1.7B-Q4_K_M.gguf", metrics.modelName)
        assertTrue(metrics.timeToFirstTokenMs > 0)
        assertTrue(metrics.generationDurationMs > 0)
        assertTrue(metrics.tokensPerSecond > 0f)
        assertTrue(metrics.promptTokensPerSecond >= metrics.tokensPerSecond)
        assertTrue(metrics.peakMemoryMb >= metrics.steadyStateMemoryMb)
        assertTrue(metrics.sttLatencyMs > 0)
        assertTrue(metrics.ttsLatencyMs > 0)
        assertTrue(metrics.thermalState.isNotBlank())
    }
}
