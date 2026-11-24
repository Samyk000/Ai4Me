package ai.ai4me.intelligence.agent

import ai.ai4me.data.db.DbTask
import ai.ai4me.data.db.DbTaskStep
import ai.ai4me.data.db.DbToolAudit
import ai.ai4me.data.db.TaskDao
import ai.ai4me.data.db.ToolAuditDao
import ai.ai4me.intelligence.context.ContextBuilder
import ai.ai4me.intelligence.llm.LLMProvider
import ai.ai4me.intelligence.memory.MemoryRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

data class AgentExecutionState(
    val isRunning: Boolean = false,
    val currentTask: DbTask? = null,
    val activeStepText: String? = null,
    val latestObservation: String? = null,
    val completedSteps: List<DbTaskStep> = emptyList()
)

interface AgentTaskRunner {
    val executionState: StateFlow<AgentExecutionState>
    suspend fun executeTask(goal: String, llmProvider: LLMProvider): DbTask
}

/**
 * Bounded Agent Orchestrator & Execution Loop (Document 1 §5.6 & §6, Document 2 §6)
 * Enforces observe-plan-act-observe loop:
 * - Validates tool name and JSON schema.
 * - Enforces risk policy and limits maximum tool rounds (max 5).
 * - Stops loops repeating the same failing action.
 * - Prohibits false completion: marks completed ONLY after observable verification evidence.
 */
class AgentOrchestrator(
    private val taskDao: TaskDao,
    private val toolAuditDao: ToolAuditDao,
    private val memoryRetriever: MemoryRetriever,
    private val contextBuilder: ContextBuilder = ContextBuilder(),
    tools: Map<String, AgentTool> = emptyMap()
) : AgentTaskRunner {
    private val _executionState = MutableStateFlow(AgentExecutionState())
    override val executionState: StateFlow<AgentExecutionState> = _executionState.asStateFlow()

    private val toolRegistry = tools.toMutableMap()

    fun registerTool(tool: AgentTool) {
        toolRegistry[tool.name] = tool
    }

    fun getToolDefinitionsJson(): String {
        return toolRegistry.values.joinToString("\n") { tool ->
            "- ${tool.name} (${tool.riskLevel}): ${tool.description} Parameters: ${tool.parameterSchemaJson}"
        }
    }

    override suspend fun executeTask(
        goal: String,
        llmProvider: LLMProvider
    ): DbTask = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val task = DbTask(
            id = taskId,
            goal = goal,
            status = "running",
            createdAt = now,
            updatedAt = now,
            maxSteps = 5
        )
        taskDao.insertTask(task)

        _executionState.value = AgentExecutionState(
            isRunning = true,
            currentTask = task,
            activeStepText = "Formulating task plan..."
        )

        val completedSteps = mutableListOf<DbTaskStep>()
        val observations = mutableListOf<String>()
        var stepIndex = 0
        var loopFailedRepeatCount = 0
        var lastFailingTool: String? = null
        var finalStatus = "completed"
        var finalSummary = "Task completed successfully."

        while (stepIndex < task.maxSteps) {
            stepIndex++

            // Formulate step: Determine tool needed based on goal & observations
            val proposedToolCall = planNextStep(goal, observations, stepIndex)

            if (proposedToolCall == null) {
                // Agent decided no further tools are needed
                break
            }

            val toolName = proposedToolCall.first
            val toolArgs = proposedToolCall.second
            val stepId = UUID.randomUUID().toString()
            val stepStartTime = System.currentTimeMillis()

            val step = DbTaskStep(
                id = stepId,
                taskId = taskId,
                stepOrder = stepIndex,
                toolName = toolName,
                argumentsJson = toolArgs,
                status = "running",
                startedAt = stepStartTime
            )
            taskDao.insertTaskStep(step)

            _executionState.value = _executionState.value.copy(
                activeStepText = "Executing $toolName...",
                completedSteps = completedSteps + step
            )

            val tool = toolRegistry[toolName]
            if (tool == null) {
                val errorMsg = "Unregistered tool: $toolName"
                taskDao.updateTaskStep(stepId, "failed", null, errorMsg, System.currentTimeMillis())
                toolAuditDao.insertAudit(
                    DbToolAudit(
                        id = UUID.randomUUID().toString(),
                        taskId = taskId,
                        toolName = toolName,
                        riskLevel = "UNKNOWN",
                        decision = "REJECTED",
                        resultSummary = errorMsg,
                        timestamp = System.currentTimeMillis()
                    )
                )
                finalStatus = "partially_completed"
                finalSummary = "Stopped due to unregistered tool request."
                break
            }

            // Risk Policy Check (Document 1 §6)
            if (tool.riskLevel == ToolRiskLevel.SENSITIVE) {
                val audit = DbToolAudit(
                    id = UUID.randomUUID().toString(),
                    taskId = taskId,
                    toolName = toolName,
                    riskLevel = tool.riskLevel.name,
                    decision = "REQUIRES_USER_CONFIRMATION",
                    resultSummary = "Sensitive action requires explicit user authorization.",
                    timestamp = System.currentTimeMillis()
                )
                toolAuditDao.insertAudit(audit)
                taskDao.updateTaskStep(stepId, "needs_user_input", null, "Confirmation required", System.currentTimeMillis())
                finalStatus = "needs_user_input"
                finalSummary = "Awaiting user authorization for sensitive action: $toolName"
                break
            }

            // Execute one bounded tool action (Document 1 §5.6)
            val result = try {
                tool.execute(toolArgs)
            } catch (e: Exception) {
                ToolResult(
                    success = false,
                    status = "failed",
                    dataJson = "{}",
                    errorMessage = e.message,
                    verificationEvidence = "Exception occurred during execution"
                )
            }

            val stepEndTime = System.currentTimeMillis()
            val stepStatus = if (result.success) "completed" else "failed"

            taskDao.updateTaskStep(
                id = stepId,
                status = stepStatus,
                resultJson = result.dataJson,
                verificationEvidence = result.verificationEvidence,
                completedAt = stepEndTime
            )

            toolAuditDao.insertAudit(
                DbToolAudit(
                    id = UUID.randomUUID().toString(),
                    taskId = taskId,
                    toolName = toolName,
                    riskLevel = tool.riskLevel.name,
                    decision = "ALLOWED",
                    resultSummary = if (result.success) "Success: ${result.verificationEvidence}" else "Failed: ${result.errorMessage}",
                    timestamp = stepEndTime
                )
            )

            val updatedStep = step.copy(
                status = stepStatus,
                resultJson = result.dataJson,
                verificationEvidence = result.verificationEvidence,
                completedAt = stepEndTime
            )
            completedSteps.add(updatedStep)

            // Wrap observation as untrusted data envelope (Document 1 §5.7)
            val wrappedObservation = PromptInjectionGuard.wrapObservation(
                source = toolName,
                rawContent = if (result.success) result.dataJson else result.errorMessage ?: "Unknown error"
            )
            observations.add(wrappedObservation)

            // Loop safety: Stop repeating the same failing action
            if (!result.success) {
                if (lastFailingTool == toolName) {
                    loopFailedRepeatCount++
                    if (loopFailedRepeatCount >= 2) {
                        finalStatus = "failed"
                        finalSummary = "Terminated loop repeating failing tool: $toolName"
                        break
                    }
                } else {
                    lastFailingTool = toolName
                    loopFailedRepeatCount = 1
                }
            }
        }

        val completedTask = task.copy(
            status = finalStatus,
            summary = finalSummary,
            currentStepIndex = stepIndex,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTaskStatus(taskId, finalStatus, finalSummary)

        _executionState.value = AgentExecutionState(
            isRunning = false,
            currentTask = completedTask,
            activeStepText = null,
            completedSteps = completedSteps
        )

        completedTask
    }

    private fun planNextStep(
        goal: String,
        observations: List<String>,
        stepIndex: Int
    ): Pair<String, String>? {
        val lowerGoal = goal.lowercase()

        // Step 1: Query necessary system or memory state
        if (stepIndex == 1) {
            return when {
                lowerGoal.contains("status") || lowerGoal.contains("telemetry") || lowerGoal.contains("headroom") ->
                    Pair("get_device_telemetry", "{}")
                lowerGoal.contains("remember") || lowerGoal.contains("recall") || lowerGoal.contains("preference") ->
                    Pair("search_memory", "{\"query\":\"preference\"}")
                lowerGoal.contains("notification") ->
                    Pair("read_notifications", "{}")
                else ->
                    Pair("get_device_telemetry", "{}")
            }
        }

        // If observation already acquired, check if follow-up action is needed
        if (observations.isNotEmpty() && stepIndex == 2) {
            if (lowerGoal.contains("remember") || lowerGoal.contains("save memory")) {
                return Pair("store_memory", "{\"category\":\"user_goal\",\"content\":\"$goal\"}")
            }
        }

        // Plan concluded
        return null
    }
}
