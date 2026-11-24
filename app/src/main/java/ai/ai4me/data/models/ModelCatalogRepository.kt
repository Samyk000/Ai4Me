package ai.ai4me.data.models

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

/**
 * Model Catalog Repository (Document 2 §1.4 & §3.3)
 * Provides curated starter models and coordinates public Hugging Face discovery.
 */
class ModelCatalogRepository(
    private val context: Context,
    private val hubClient: HuggingFaceHubClient = HuggingFaceHubClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun getFeaturedModels(): List<FeaturedModel> = withContext(Dispatchers.IO) {
        try {
            context.assets.open("models/featured_models.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val content = reader.readText()
                    json.decodeFromString<List<FeaturedModel>>(content)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun searchModels(query: String): Result<List<HuggingFaceModelSummary>> {
        return hubClient.searchGgufModels(query)
    }

    suspend fun getRepositoryFiles(repoId: String): Result<List<HuggingFaceGgufFile>> {
        return hubClient.getRepositoryGgufFiles(repoId)
    }
}
