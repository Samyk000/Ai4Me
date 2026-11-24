package ai.ai4me.hardware

/**
 * Three hardware-aware recommendation profiles (Document 1 §2)
 * Profiles are recommendations, not promises. They adapt to measured headroom.
 */
enum class HardwareProfileType(
    val title: String,
    val recommendedModelSize: String,
    val recommendedQuantization: String,
    val defaultContextSizeTokens: Int,
    val description: String
) {
    BALANCED(
        title = "Balanced Profile",
        recommendedModelSize = "3B – 4B parameters",
        recommendedQuantization = "Q4_K_M or Q4_0",
        defaultContextSizeTokens = 2048,
        description = "Optimized for stability, moderate context, and conservative memory headroom on 8GB-12GB devices."
    ),
    PERFORMANCE(
        title = "Performance Profile",
        recommendedModelSize = "7B – 8B parameters",
        recommendedQuantization = "Q4_K_M or Q5_K_M",
        defaultContextSizeTokens = 4096,
        description = "Higher quality reasoning and larger context window. Recommended only when measured RAM headroom >= 6GB."
    ),
    BATTERY_SAVER(
        title = "Battery Saver Profile",
        recommendedModelSize = "1B – 2B parameters",
        recommendedQuantization = "Q3_K_M or Q4_0",
        defaultContextSizeTokens = 1024,
        description = "Minimal battery impact, compact context, aggressively released speech and inference buffers."
    )
}

data class HardwareRecommendation(
    val recommendedProfile: HardwareProfileType,
    val canRunLocalModels: Boolean,
    val reasoning: String,
    val warnings: List<String>
)

/**
 * Pure evaluator for hardware profile recommendations (Document 1 §2).
 * Completely decoupled from Android Context for testability and portability.
 */
object HardwareProfileEvaluator {

    fun evaluate(spec: DeviceHardwareSpec): HardwareRecommendation {
        val warnings = mutableListOf<String>()

        if (!spec.is64Bit) {
            warnings.add("32-bit architecture detected. Modern GGUF and ONNX runtimes require 64-bit ABI.")
            return HardwareRecommendation(
                recommendedProfile = HardwareProfileType.BATTERY_SAVER,
                canRunLocalModels = false,
                reasoning = "Device ABI lacks 64-bit support necessary for on-device inference engines.",
                warnings = warnings
            )
        }

        if (spec.storage.freeGb < 4.0f) {
            warnings.add("Low storage: Less than 4 GB free. Downloading models requires sufficient disk space.")
        }

        if (spec.memory.isLowMemory) {
            warnings.add("System is currently reporting low memory pressure.")
        }

        val totalRamGb = spec.memory.totalRamGb
        val availRamGb = spec.memory.availableRamGb

        val profile: HardwareProfileType = when {
            totalRamGb >= 11.5f && availRamGb >= 5.5f -> {
                HardwareProfileType.PERFORMANCE
            }
            totalRamGb >= 5.5f && availRamGb >= 2.5f -> {
                HardwareProfileType.BALANCED
            }
            else -> {
                warnings.add("Available memory headroom is constrained (< 2.5 GB).")
                HardwareProfileType.BATTERY_SAVER
            }
        }

        val reasoning = when (profile) {
            HardwareProfileType.PERFORMANCE ->
                "Measured RAM (${String.format("%.1f", totalRamGb)} GB total, ${String.format("%.1f", availRamGb)} GB free) and ${spec.cpuCoreCount} cores support larger 7B models with 4K context."
            HardwareProfileType.BALANCED ->
                "Recommended for everyday stability: 3B–4B quantized models balance speed, context, and thermal headroom."
            HardwareProfileType.BATTERY_SAVER ->
                "Conservative profile recommended to prevent out-of-memory crashes or aggressive OS process killing."
        }

        return HardwareRecommendation(
            recommendedProfile = profile,
            canRunLocalModels = true,
            reasoning = reasoning,
            warnings = warnings
        )
    }
}
