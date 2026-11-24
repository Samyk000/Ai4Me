package ai.ai4me.data.db

import android.content.ContentValues
import android.database.Cursor

class MemoryDao(private val dbHelper: Ai4MeDatabaseHelper) {

    fun insertMemory(item: DbMemoryItem) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", item.id)
            put("category", item.category)
            put("content", item.content)
            put("source_conversation_id", item.sourceConversationId)
            put("source_message_id", item.sourceMessageId)
            put("created_at", item.createdAt)
            put("updated_at", item.updatedAt)
            put("confidence", item.confidence)
            put("sensitivity", item.sensitivity)
            put("expires_at", item.expiresAt)
            put("user_approved", if (item.userApproved) 1 else 0)
        }
        db.insertWithOnConflict(Ai4MeDatabaseHelper.TABLE_MEMORIES, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllMemories(): List<DbMemoryItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            Ai4MeDatabaseHelper.TABLE_MEMORIES,
            null,
            null, null, null, null,
            "updated_at DESC"
        )
        val list = mutableListOf<DbMemoryItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseMemory(it))
            }
        }
        return list
    }

    fun searchMemories(keyword: String, limit: Int = 5): List<DbMemoryItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            Ai4MeDatabaseHelper.TABLE_MEMORIES,
            null,
            "content LIKE ? AND sensitivity != 'restricted'",
            arrayOf("%$keyword%"),
            null, null,
            "updated_at DESC",
            limit.toString()
        )
        val list = mutableListOf<DbMemoryItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseMemory(it))
            }
        }
        return list
    }

    fun deleteMemory(id: String) {
        val db = dbHelper.writableDatabase
        db.delete(Ai4MeDatabaseHelper.TABLE_MEMORIES, "id = ?", arrayOf(id))
    }

    private fun parseMemory(cursor: Cursor): DbMemoryItem {
        return DbMemoryItem(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
            content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
            sourceConversationId = cursor.getString(cursor.getColumnIndexOrThrow("source_conversation_id")),
            sourceMessageId = cursor.getString(cursor.getColumnIndexOrThrow("source_message_id")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
            confidence = cursor.getFloat(cursor.getColumnIndexOrThrow("confidence")),
            sensitivity = cursor.getString(cursor.getColumnIndexOrThrow("sensitivity")),
            expiresAt = if (cursor.isNull(cursor.getColumnIndexOrThrow("expires_at"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("expires_at")),
            userApproved = cursor.getInt(cursor.getColumnIndexOrThrow("user_approved")) == 1
        )
    }
}
