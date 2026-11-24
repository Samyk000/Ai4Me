package ai.ai4me.data.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Public Hugging Face Hub discovery client (Document 2 §1.4 & §3.2).
 * Strictly guarantees public browsing and downloading without login.
 */
class HuggingFaceHubClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun searchGgufModels(query: String): Result<List<HuggingFaceModelSummary>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://huggingface.co/api/models?search=$encodedQuery&filter=gguf&limit=25"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Ai4Me-Android/0.1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Hub API request failed with HTTP ${response.code}"))
                }
                val bodyString = response.body?.string().orEmpty()
                val jsonElement = json.parseToJsonElement(bodyString)
                val results = mutableListOf<HuggingFaceModelSummary>()

                jsonElement.jsonArray.forEach { item ->
                    val obj = item.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content.orEmpty()
                    val author = obj["author"]?.jsonPrimitive?.content
                    val downloads = obj["downloads"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val likes = obj["likes"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val isPrivate = obj["private"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    val isGated = obj["gated"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

                    if (id.isNotBlank()) {
                        results.add(
                            HuggingFaceModelSummary(
                                id = id,
                                author = author,
                                downloads = downloads,
                                likes = likes,
                                private = isPrivate,
                                gated = isGated
                            )
                        )
                    }
                }
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRepositoryGgufFiles(repoId: String): Result<List<HuggingFaceGgufFile>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://huggingface.co/api/models/$repoId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Ai4Me-Android/0.1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to inspect repository files: HTTP ${response.code}"))
                }
                val bodyString = response.body?.string().orEmpty()
                val jsonElement = json.parseToJsonElement(bodyString)
                val siblings = jsonElement.jsonObject["siblings"]?.jsonArray
                val ggufFiles = mutableListOf<HuggingFaceGgufFile>()

                siblings?.forEach { fileItem ->
                    val rfilename = fileItem.jsonObject["rfilename"]?.jsonPrimitive?.content.orEmpty()
                    if (rfilename.endsWith(".gguf", ignoreCase = true)) {
                        val downloadUrl = "https://huggingface.co/$repoId/resolve/main/$rfilename"
                        ggufFiles.add(
                            HuggingFaceGgufFile(
                                repoId = repoId,
                                fileName = rfilename,
                                downloadUrl = downloadUrl,
                                sizeBytes = 0L // Sizing resolved prior to download via HEAD request
                            )
                        )
                    }
                }
                Result.success(ggufFiles)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
