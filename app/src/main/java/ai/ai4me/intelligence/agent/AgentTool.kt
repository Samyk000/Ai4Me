package ai.ai4me.intelligence.agent

enum class ToolRiskLevel {
    AUTOMATIC,      // Read permitted data, summarize, inspect state
    PREAUTHORIZED,  // Routine tasks configured by user (e.g. save memory, create reminder)
    SENSITIVE       // Sensitive actions requiring explicit confirmation (payments, destructive deletion)
}

data class ToolResult(
    val success: Boolean,
    val status: String,
    val dataJson: String,
    val errorMessage: String? = null,
    val verificationEvidence: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface AgentTool {
    val name: String
    val description: String
    val parameterSchemaJson: String
    val riskLevel: ToolRiskLevel

    suspend fun execute(argumentsJson: String): ToolResult
}
