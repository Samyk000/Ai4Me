package ai.ai4me.data.db

data class DbConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val activeModelId: String? = null,
    val summary: String? = null,
    val archivedAt: Long? = null
)

data class DbMessage(
    val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val createdAt: Long,
    val tokenCountEstimate: Int = 0,
    val status: String = "completed",
    val parentMessageId: String? = null
)

data class DbMemoryItem(
    val id: String,
    val category: String, // "preference", "fact", "routine", "entity"
    val content: String,
    val sourceConversationId: String? = null,
    val sourceMessageId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val confidence: Float = 1.0f,
    val sensitivity: String = "low", // "low", "medium", "restricted"
    val expiresAt: Long? = null,
    val userApproved: Boolean = true
)

data class DbTask(
    val id: String,
    val goal: String,
    val status: String, // "completed", "partially_completed", "failed", "cancelled", "unverified", "needs_user_input"
    val createdAt: Long,
    val updatedAt: Long,
    val currentStepIndex: Int = 0,
    val maxSteps: Int = 5,
    val summary: String? = null
)

data class DbTaskStep(
    val id: String,
    val taskId: String,
    val stepOrder: Int,
    val toolName: String,
    val argumentsJson: String,
    val status: String, // "pending", "running", "completed", "failed", "verified"
    val resultJson: String? = null,
    val verificationEvidence: String? = null,
    val startedAt: Long,
    val completedAt: Long? = null
)

data class DbToolAudit(
    val id: String,
    val taskId: String?,
    val toolName: String,
    val riskLevel: String, // "AUTOMATIC", "PREAUTHORIZED", "SENSITIVE"
    val decision: String, // "ALLOWED", "CONFIRMED", "REJECTED"
    val resultSummary: String,
    val timestamp: Long
)
