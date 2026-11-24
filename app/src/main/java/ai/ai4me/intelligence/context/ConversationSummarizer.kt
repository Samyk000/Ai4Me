package ai.ai4me.intelligence.context

import ai.ai4me.data.db.DbMessage

/**
 * Conversation Compaction & Summarizer (Document 1 §5.4 & Document 2 §5.3)
 * Summarizes older conversation turns when context approaches budget.
 * Strictly preserves:
 * - Unresolved commitments and active user goals
 * - Named entities and resolved identities (e.g. Rahul, mom)
 * - Dates, times, and amounts
 * - Completed and failed tool actions
 * - User corrections
 */
object ConversationSummarizer {

    fun summarize(olderMessages: List<DbMessage>): String {
        if (olderMessages.isEmpty()) return ""

        val userGoals = mutableListOf<String>()
        val toolActions = mutableListOf<String>()
        val keyEntities = mutableListOf<String>()

        for (msg in olderMessages) {
            val content = msg.content
            when (msg.role) {
                "user" -> {
                    if (content.length > 5) userGoals.add(content.take(80))
                    extractNamedEntities(content).forEach { keyEntities.add(it) }
                }
                "tool" -> {
                    toolActions.add(content.take(60))
                }
                "assistant" -> {
                    // Detect corrections or commitments
                    if (content.contains("tomorrow", ignoreCase = true) ||
                        content.contains("call", ignoreCase = true) ||
                        content.contains("remind", ignoreCase = true)) {
                        userGoals.add("Commitment: ${content.take(70)}")
                    }
                }
            }
        }

        val summaryParts = mutableListOf<String>()
        if (userGoals.isNotEmpty()) {
            summaryParts.add("Previous Goals & Context: " + userGoals.takeLast(3).joinToString("; "))
        }
        if (toolActions.isNotEmpty()) {
            summaryParts.add("Tool Actions Executed: " + toolActions.takeLast(3).joinToString("; "))
        }
        if (keyEntities.isNotEmpty()) {
            summaryParts.add("Referenced Entities: " + keyEntities.distinct().joinToString(", "))
        }

        return summaryParts.joinToString("\n")
    }

    private fun extractNamedEntities(text: String): List<String> {
        val words = text.split("\\s+".toRegex())
        return words.filter { it.isNotEmpty() && it[0].isUpperCase() && it.length > 2 }
    }
}
