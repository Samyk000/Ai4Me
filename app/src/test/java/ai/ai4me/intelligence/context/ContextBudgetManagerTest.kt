package ai.ai4me.intelligence.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBudgetManagerTest {

    @Test
    fun testTokenAllocation_WithinLimits() {
        val manager = ContextBudgetManager(maxContextTokens = 2048)

        assertEquals(2048, manager.maxContextTokens)
        assertEquals(512, manager.reservedOutputTokens)
        assertEquals(350, manager.reservedSystemTokens)

        // Available input tokens = 2048 - 512 = 1536
        assertEquals(1536, manager.availableInputTokens)

        // Recent turns gets remaining headroom
        assertTrue(manager.recentTurnsBudget > 500)
    }

    @Test
    fun testTokenEstimation() {
        val manager = ContextBudgetManager()
        val text = "Hello world this is a test sentence."
        val tokens = manager.estimateTokens(text)
        assertTrue(tokens in 8..12)
    }

    @Test
    fun testIsWithinBudget() {
        val manager = ContextBudgetManager()
        val currentTokens = 200
        val shortAddition = "Short addition"
        val hugeAddition = "Very long text ".repeat(300)

        assertTrue(manager.isWithinBudget(currentTokens, shortAddition, 250))
        assertFalse(manager.isWithinBudget(currentTokens, hugeAddition, 250))
    }
}
