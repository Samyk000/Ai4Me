package ai.ai4me.intelligence.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class ModelRuntimeState {
    NOT_INSTALLED,
    DOWNLOADED,
    VALIDATING,
    READY,
    LOADING,
    LOADED,
    GENERATING,
    UNLOADING,
    ERROR
}

data class RuntimeConfig(
    val contextLengthTokens: Int = 2048,
    val cpuThreads: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 6),
    val gpuLayers: Int = 0,
    val chatTemplate: String = "chatml"
)

data class SamplingParams(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 512,
    val stopSequences: List<String> = listOf("<|im_end|>", "<|endoftext|>", "</s>")
)

data class GenerationChunk(
    val token: String,
    val isFinished: Boolean = false,
    val tokensPerSecond: Float = 0f,
    val timeToFirstTokenMs: Long = 0L,
    val finishReason: String? = null
)

/**
 * Replaceable LLM inference interface (Document 1 §1 & Document 2 §4).
 * Decoupled from concrete native inference runtimes.
 */
interface LLMProvider {
    val runtimeState: StateFlow<ModelRuntimeState>
    val loadedModelFile: File?

    suspend fun loadModel(modelFile: File, config: RuntimeConfig): Result<Unit>
    suspend fun unloadModel(): Result<Unit>
    fun generateStream(prompt: String, params: SamplingParams): Flow<GenerationChunk>
    fun cancelGeneration()
}
