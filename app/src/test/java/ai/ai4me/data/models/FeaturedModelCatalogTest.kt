package ai.ai4me.data.models

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FeaturedModelCatalogTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testFeaturedModelsJson_ParsesAndMatchesSpec() {
        val catalogFile = File("src/main/assets/models/featured_models.json")
        assertTrue(catalogFile.exists())

        val content = catalogFile.readText()
        val models = json.decodeFromString<List<FeaturedModel>>(content)

        assertTrue(models.isNotEmpty())
        assertEquals(4, models.size)

        // Verify each model has valid properties
        models.forEach { model ->
            assertTrue(model.id.isNotBlank())
            assertTrue(model.displayName.isNotBlank())
            assertEquals("GGUF", model.format)
            assertTrue(model.sizeBytes > 100_000_000L) // Must be substantial model file size
            assertTrue(model.estimatedRamMinGb > 0f)
            assertTrue(model.estimatedRamMaxGb >= model.estimatedRamMinGb)
            assertTrue(model.downloadUrl.startsWith("https://huggingface.co/"))
        }

        // Verify Balanced Profile recommendation exists
        val balancedModel = models.find { it.supportedProfile == "BALANCED" }
        assertNotNull(balancedModel)
    }
}
