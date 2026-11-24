package ai.ai4me

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import ai.ai4me.hardware.HardwareCapabilityScanner
import ai.ai4me.ui.navigation.Ai4MeNavHost
import ai.ai4me.ui.theme.Ai4MeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val scanner = HardwareCapabilityScanner(applicationContext)
        val spec = scanner.scanDevice()
        val recommendation = scanner.evaluateRecommendation(spec)

        setContent {
            Ai4MeTheme {
                Ai4MeNavHost(
                    hardwareSpec = spec,
                    recommendation = recommendation
                )
            }
        }
    }
}
