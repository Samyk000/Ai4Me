package ai.ai4me.automation.apps

import ai.ai4me.intelligence.agent.AgentTool
import ai.ai4me.intelligence.agent.ToolResult
import ai.ai4me.intelligence.agent.ToolRiskLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface AppLauncher {
    fun launchApp(appName: String): Result<String>
}

/**
 * Launch App Tool (Document 1 §3.3 & Document 2 §7)
 * Safely launches installed Android applications through verified package intents.
 */
class LaunchAppTool(
    private val appLauncher: AppLauncher
) : AgentTool {
    override val name: String = "launch_app"
    override val description: String = "Launches an installed application on the device by its display name."
    override val parameterSchemaJson: String = """{"appName":"string"}"""
    override val riskLevel: ToolRiskLevel = ToolRiskLevel.AUTOMATIC

    override suspend fun execute(argumentsJson: String): ToolResult {
        val json = Json { ignoreUnknownKeys = true }
        val obj = try {
            json.parseToJsonElement(argumentsJson).jsonObject
        } catch (e: Exception) {
            return ToolResult(
                success = false,
                status = "failed",
                dataJson = "{}",
                errorMessage = "Invalid JSON arguments: ${e.message}",
                verificationEvidence = "Schema parse failed"
            )
        }

        val appName = obj["appName"]?.jsonPrimitive?.content.orEmpty()
        if (appName.isBlank()) {
            return ToolResult(
                success = false,
                status = "failed",
                dataJson = "{}",
                errorMessage = "App name cannot be empty.",
                verificationEvidence = "Validation failed"
            )
        }

        val result = appLauncher.launchApp(appName)
        return if (result.isSuccess) {
            val packageName = result.getOrNull().orEmpty()
            ToolResult(
                success = true,
                status = "completed",
                dataJson = """{"appName":"$appName","packageName":"$packageName"}""",
                verificationEvidence = "App launch intent dispatched successfully for package: $packageName"
            )
        } else {
            ToolResult(
                success = false,
                status = "failed",
                dataJson = "{}",
                errorMessage = "Could not launch app '$appName': ${result.exceptionOrNull()?.message}",
                verificationEvidence = "Package manager query returned no matching launch intent"
            )
        }
    }
}
