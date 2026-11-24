package ai.ai4me.data.models

import kotlinx.serialization.Serializable

@Serializable
data class FeaturedModel(
    val id: String,
    val displayName: String,
    val publisher: String,
    val repoId: String,
    val fileName: String,
    val downloadUrl: String,
    val parameterCount: String,
    val quantization: String,
    val format: String,
    val sizeBytes: Long,
    val license: String,
    val chatTemplate: String,
    val contextRecommendationTokens: Int,
    val estimatedRamMinGb: Float,
    val estimatedRamMaxGb: Float,
    val supportedProfile: String,
    val compatibilityNotes: String
)

@Serializable
data class HuggingFaceModelSummary(
    val id: String,
    val author: String? = null,
    val downloads: Int = 0,
    val likes: Int = 0,
    val private: Boolean = false,
    val gated: Boolean = false
)

data class HuggingFaceGgufFile(
    val repoId: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

enum class DownloadState {
    QUEUED,
    CONNECTING,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadProgress(
    val modelId: String,
    val fileName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val state: DownloadState,
    val errorMessage: String? = null
) {
    val progressPercent: Float = if (totalBytes > 0) {
        (bytesDownloaded.toFloat() / totalBytes.toFloat()) * 100f
    } else 0f
}

data class InstalledModel(
    val id: String,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val architecture: String,
    val quantization: String,
    val contextLength: Int,
    val isLoaded: Boolean = false
) {
    val sizeGb: Float = sizeBytes / (1024f * 1024f * 1024f)
}
