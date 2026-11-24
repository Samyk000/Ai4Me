package ai.ai4me.intelligence.context

import ai.ai4me.data.db.DbMessage
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSummarizerTest {

    @Test
    fun testSummarize_PreservesKeyGoalsAndEntities() {
        val messages = listOf(
            DbMessage(id = "1", conversationId = "c1", role = "user", content = "Remind Rahul that I'll call tomorrow at 3pm", createdAt = 100L),
            DbMessage(id = "2", conversationId = "c1", role = "tool", content = "Drafted message to Rahul", createdAt = 200L),
            DbMessage(id = "3", conversationId = "c1", role = "assistant", content = "I will remind Rahul tomorrow.", createdAt = 300L)
        )

        val summary = ConversationSummarizer.summarize(messages)

        assertTrue(summary.contains("Rahul"))
        assertTrue(summary.contains("Tool Actions Executed") || summary.contains("Drafted"))
        assertTrue(summary.contains("tomorrow") || summary.contains("call"))
    }
}
