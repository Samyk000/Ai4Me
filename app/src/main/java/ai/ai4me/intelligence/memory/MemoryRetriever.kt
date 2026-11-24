package ai.ai4me.intelligence.memory

import ai.ai4me.data.db.DbMemoryItem
import ai.ai4me.data.db.MemoryDao

/**
 * Memory Retrieval with Recency Weighting & Sensitivity Filtering (Document 1 §5.5 & Document 2 §5.5)
 * Matches keywords and applies recency decay, refusing restricted items.
 */
class MemoryRetriever(private val memoryDao: MemoryDao) {

    fun retrieveRelevantMemories(query: String, maxItems: Int = 4): List<DbMemoryItem> {
        val words = query.lowercase().split("\\s+".toRegex()).filter { it.length >= 3 }
        if (words.isEmpty()) {
            // Fallback: return most recent approved non-restricted memories
            return memoryDao.getAllMemories()
                .filter { it.userApproved && it.sensitivity != "restricted" }
                .take(maxItems)
        }

        val candidates = mutableMapOf<String, Pair<DbMemoryItem, Float>>()
        val now = System.currentTimeMillis()

        for (word in words) {
            val matches = memoryDao.searchMemories(word, limit = 8)
            for (item in matches) {
                if (!item.userApproved || item.sensitivity == "restricted") continue

                // Base match score
                var score = 1.0f

                // Recency weighting (exponential decay over 30 days)
                val ageDays = ((now - item.updatedAt) / (1000f * 60f * 60f * 24f)).coerceAtLeast(0f)
                val recencyMultiplier = (1.0f / (1.0f + (ageDays * 0.05f)))
                score *= recencyMultiplier

                val existing = candidates[item.id]
                if (existing == null || existing.second < score) {
                    candidates[item.id] = Pair(item, score)
                }
            }
        }

        return candidates.values
            .sortedByDescending { it.second }
            .take(maxItems)
            .map { it.first }
    }
}
