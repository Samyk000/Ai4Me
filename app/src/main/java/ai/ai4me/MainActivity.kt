package ai.ai4me

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import ai.ai4me.data.models.ModelCatalogRepository
import ai.ai4me.data.models.ModelDownloadManager
import ai.ai4me.hardware.HardwareCapabilityScanner
import ai.ai4me.intelligence.llm.ModelRuntimeManager
import ai.ai4me.ui.navigation.Ai4MeNavHost
import ai.ai4me.ui.theme.Ai4MeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val scanner = HardwareCapabilityScanner(applicationContext)
        val spec = scanner.scanDevice()
        val recommendation = scanner.evaluateRecommendation(spec)

        val catalogRepository = ModelCatalogRepository(applicationContext)
        val downloadManager = ModelDownloadManager(applicationContext)
        val runtimeManager = ModelRuntimeManager()

        setContent {
            Ai4MeTheme {
                Ai4MeNavHost(
                    hardwareSpec = spec,
                    recommendation = recommendation,
                    catalogRepository = catalogRepository,
                    downloadManager = downloadManager,
                    runtimeManager = runtimeManager
                )
            }
        }
    }
}
