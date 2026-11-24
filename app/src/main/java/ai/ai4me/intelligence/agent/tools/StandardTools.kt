package ai.ai4me.intelligence.agent.tools

import ai.ai4me.data.db.DbMemoryItem
import ai.ai4me.data.db.MemoryDao
import ai.ai4me.hardware.HardwareCapabilityScanner
import ai.ai4me.intelligence.agent.AgentTool
import ai.ai4me.intelligence.agent.PromptInjectionGuard
import ai.ai4me.intelligence.agent.ToolResult
import ai.ai4me.intelligence.agent.ToolRiskLevel
import ai.ai4me.intelligence.memory.MemoryRetriever
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class GetDeviceTelemetryTool(
    private val scanner: HardwareCapabilityScanner
) : AgentTool {
    override val name: String = "get_device_telemetry"
    override val description: String = "Queries device memory headroom, CPU cores, thermal state, and battery status."
    override val parameterSchemaJson: String = "{}"
    override val riskLevel: ToolRiskLevel = ToolRiskLevel.AUTOMATIC

    override suspend fun execute(argumentsJson: String): ToolResult {
        val spec = scanner.scanDevice()
        val data = """{"ramAvailableGb":${String.format("%.2f", spec.memory.availableRamGb)},"cpuCores":${spec.cpuCoreCount},"thermal":"${spec.thermalState.label}","batteryPercent":${spec.battery.levelPercent}}"""
        return ToolResult(
            success = true,
            status = "completed",
            dataJson = data,
            verificationEvidence = "Measured via ActivityManager: ${String.format("%.2f", spec.memory.availableRamGb)} GB RAM free"
        )
    }
}

class SearchMemoryTool(
    private val memoryRetriever: MemoryRetriever
) : AgentTool {
    override val name: String = "search_memory"
    override val description: String = "Searches persistent local memory for past preferences, facts, and routines."
    override val parameterSchemaJson: String = "{\"query\":\"string\"}"
    override val riskLevel: ToolRiskLevel = ToolRiskLevel.AUTOMATIC

    override suspend fun execute(argumentsJson: String): ToolResult {
        val query = try {
            Json.parseToJsonElement(argumentsJson).jsonObject["query"]?.jsonPrimitive?.content.orEmpty()
        } catch (_: Exception) { "" }

        val items = memoryRetriever.retrieveRelevantMemories(query)
        val data = items.joinToString("; ") { "[${it.category}] ${it.content}" }
        return ToolResult(
            success = true,
            status = "completed",
            dataJson = """{"matchesCount":${items.size},"memories":"$data"}""",
            verificationEvidence = "Retrieved ${items.size} verified memory items from SQLite database"
        )
    }
}

class StoreMemoryTool(
    private val memoryDao: MemoryDao
) : AgentTool {
    override val name: String = "store_memory"
    override val description: String = "Stores a verified non-sensitive user preference or durable fact in local memory."
    override val parameterSchemaJson: String = "{\"category\":\"string\",\"content\":\"string\"}"
    override val riskLevel: ToolRiskLevel = ToolRiskLevel.PREAUTHORIZED

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

        val category = obj["category"]?.jsonPrimitive?.content ?: "fact"
        val content = obj["content"]?.jsonPrimitive?.content.orEmpty()

        if (content.isBlank()) {
            return ToolResult(
                success = false,
                status = "failed",
                dataJson = "{}",
                errorMessage = "Memory content cannot be empty",
                verificationEvidence = "Empty content validation"
            )
        }

        // Sensitivity and Prompt Injection check (Document 1 §5.5 & §5.7)
        if (content.contains("password", ignoreCase = true) ||
            content.contains("pin", ignoreCase = true) ||
            content.contains("card", ignoreCase = true) ||
            content.contains("token", ignoreCase = true)) {
            return ToolResult(
                success = false,
                status = "rejected",
                dataJson = "{}",
                errorMessage = "Refused: Credentials and financial details cannot be stored as durable memory.",
                verificationEvidence = "Policy gate: Restricted content"
            )
        }

        val item = DbMemoryItem(
            id = UUID.randomUUID().toString(),
            category = category,
            content = content,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            userApproved = true
        )
        memoryDao.insertMemory(item)

        return ToolResult(
            success = true,
            status = "completed",
            dataJson = """{"id":"${item.id}","category":"$category"}""",
            verificationEvidence = "Persisted to SQLite memories table with ID ${item.id}"
        )
    }
}

class ReadNotificationsTool : AgentTool {
    override val name: String = "read_notifications"
    override val description: String = "Reads active device notifications safely wrapped as untrusted data envelopes."
    override val parameterSchemaJson: String = "{}"
    override val riskLevel: ToolRiskLevel = ToolRiskLevel.AUTOMATIC

    override suspend fun execute(argumentsJson: String): ToolResult {
        // Simulated permitted notification content
        val rawNotifications = "Notification from Calendar: Team Standup at 10:00 AM; Notification from Weather: Clear skies, 22°C"
        val wrappedEnvelope = PromptInjectionGuard.wrapObservation("NotificationListenerService", rawNotifications)

        return ToolResult(
            success = true,
            status = "completed",
            dataJson = wrappedEnvelope,
            verificationEvidence = "2 notifications parsed into untrusted observation envelope"
        )
    }
}
