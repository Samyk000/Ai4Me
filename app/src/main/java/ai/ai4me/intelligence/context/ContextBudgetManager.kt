package ai.ai4me.intelligence.context

/**
 * Context Budget Manager (Document 1 §5.3)
 * Enforces dynamic token allocations across system policy, task state,
 * retrieved memory, and recent conversation turns.
 */
class ContextBudgetManager(
    val maxContextTokens: Int = 2048,
    val reservedOutputTokens: Int = 512,
    val reservedSystemTokens: Int = 350,
    val taskStateBudget: Int = 300,
    val memoryBudget: Int = 250
) {
    val availableInputTokens: Int
        get() = (maxContextTokens - reservedOutputTokens).coerceAtLeast(512)

    val recentTurnsBudget: Int
        get() = (availableInputTokens - reservedSystemTokens - taskStateBudget - memoryBudget).coerceAtLeast(300)

    /**
     * Approximate token estimation heuristic (~3.8 characters per token for English/code).
     * Explicitly labeled approximate as required by Document 1 §5.3.
     */
    fun estimateTokens(text: String): Int {
        if (text.isEmpty()) return 0
        return (text.length / 3.8f).toInt().coerceAtLeast(1)
    }

    fun isWithinBudget(currentTokens: Int, additionalText: String, budget: Int): Boolean {
        return (currentTokens + estimateTokens(additionalText)) <= budget
    }
}
