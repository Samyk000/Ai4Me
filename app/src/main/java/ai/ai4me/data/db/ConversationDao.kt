package ai.ai4me.data.db

import android.content.ContentValues
import android.database.Cursor

class ConversationDao(private val dbHelper: Ai4MeDatabaseHelper) {

    fun insertConversation(conversation: DbConversation) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", conversation.id)
            put("title", conversation.title)
            put("created_at", conversation.createdAt)
            put("updated_at", conversation.updatedAt)
            put("active_model_id", conversation.activeModelId)
            put("summary", conversation.summary)
            put("archived_at", conversation.archivedAt)
        }
        db.insertWithOnConflict(Ai4MeDatabaseHelper.TABLE_CONVERSATIONS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateConversationSummary(id: String, summary: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("summary", summary)
            put("updated_at", System.currentTimeMillis())
        }
        db.update(Ai4MeDatabaseHelper.TABLE_CONVERSATIONS, values, "id = ?", arrayOf(id))
    }

    fun getConversation(id: String): DbConversation? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            Ai4MeDatabaseHelper.TABLE_CONVERSATIONS,
            null,
            "id = ?",
            arrayOf(id),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) parseConversation(it) else null
        }
    }

    fun insertMessage(message: DbMessage) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", message.id)
            put("conversation_id", message.conversationId)
            put("role", message.role)
            put("content", message.content)
            put("created_at", message.createdAt)
            put("token_count_estimate", message.tokenCountEstimate)
            put("status", message.status)
            put("parent_message_id", message.parentMessageId)
        }
        db.insertWithOnConflict(Ai4MeDatabaseHelper.TABLE_MESSAGES, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getRecentMessages(conversationId: String, limit: Int = 20): List<DbMessage> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            Ai4MeDatabaseHelper.TABLE_MESSAGES,
            null,
            "conversation_id = ?",
            arrayOf(conversationId),
            null, null,
            "created_at DESC",
            limit.toString()
        )
        val list = mutableListOf<DbMessage>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseMessage(it))
            }
        }
        return list.reversed() // Oldest first within the recent window
    }

    private fun parseConversation(cursor: Cursor): DbConversation {
        return DbConversation(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
            activeModelId = cursor.getString(cursor.getColumnIndexOrThrow("active_model_id")),
            summary = cursor.getString(cursor.getColumnIndexOrThrow("summary")),
            archivedAt = if (cursor.isNull(cursor.getColumnIndexOrThrow("archived_at"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("archived_at"))
        )
    }

    private fun parseMessage(cursor: Cursor): DbMessage {
        return DbMessage(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            conversationId = cursor.getString(cursor.getColumnIndexOrThrow("conversation_id")),
            role = cursor.getString(cursor.getColumnIndexOrThrow("role")),
            content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            tokenCountEstimate = cursor.getInt(cursor.getColumnIndexOrThrow("token_count_estimate")),
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
            parentMessageId = cursor.getString(cursor.getColumnIndexOrThrow("parent_message_id"))
        )
    }
}
