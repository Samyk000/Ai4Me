package ai.ai4me.intelligence.context

import ai.ai4me.data.db.DbMemoryItem
import ai.ai4me.data.db.DbMessage
import ai.ai4me.data.db.DbTask
import ai.ai4me.data.db.DbTaskStep
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBuilderTest {

    @Test
    fun testAssemblyOrder_StrictlyMatchesSpec() {
        val builder = ContextBuilder(ContextBudgetManager(maxContextTokens = 4096))

        val task = DbTask(
            id = "task-1",
            goal = "Check battery and notifications",
            status = "running",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val step = DbTaskStep(
            id = "step-1",
            taskId = "task-1",
            stepOrder = 1,
            toolName = "get_device_telemetry",
            argumentsJson = "{}",
            status = "completed",
            verificationEvidence = "Battery is 85%",
            startedAt = 1000L
        )

        val memory = DbMemoryItem(
            id = "mem-1",
            category = "preference",
            content = "User prefers concise answers",
            createdAt = 500L,
            updatedAt = 500L
        )

        val messages = listOf(
            DbMessage(id = "m1", conversationId = "c1", role = "user", content = "Hi", createdAt = 100L),
            DbMessage(id = "m2", conversationId = "c1", role = "assistant", content = "Hello! How can I help?", createdAt = 200L)
        )

        val assembled = builder.buildPrompt(
            toolDefinitionsJson = "- get_device_telemetry: checks battery",
            userProfileFacts = listOf("Primary Language: English"),
            activeTask = task,
            taskSteps = listOf(step),
            retrievedMemories = listOf(memory),
            allSessionMessages = messages,
            currentUserRequest = "What is my battery level?",
            followUpObservations = listOf("<observation source=\"battery\">Level: 85%</observation>")
        )

        val prompt = assembled.fullPrompt

        // Verify assembly order (Document 1 §5.2):
        val posPolicy = prompt.indexOf("### SYSTEM POLICY")
        val posTools = prompt.indexOf("### AVAILABLE TOOLS")
        val posProfile = prompt.indexOf("### USER PROFILE FACTS")
        val posTask = prompt.indexOf("### ACTIVE TASK STATE")
        val posMemory = prompt.indexOf("### RETRIEVED MEMORY")
        val posTurns = prompt.indexOf("### RECENT CONVERSATION TURNS")
        val posRequest = prompt.indexOf("### CURRENT USER REQUEST")
        val posObservations = prompt.indexOf("### TOOL OBSERVATIONS")

        assertTrue(posPolicy != -1)
        assertTrue(posTools > posPolicy)
        assertTrue(posProfile > posTools)
        assertTrue(posTask > posProfile)
        assertTrue(posMemory > posTask)
        assertTrue(posTurns > posMemory)
        assertTrue(posRequest > posTurns)
        assertTrue(posObservations > posRequest)

        assertTrue(assembled.estimatedTokens > 0)
        assertTrue(assembled.memoriesIncludedCount == 1)
        assertTrue(assembled.turnsIncludedCount == 2)
    }
}
