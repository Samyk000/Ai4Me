package ai.ai4me.data.db

import android.content.ContentValues
import android.database.Cursor

class ToolAuditDao(private val dbHelper: Ai4MeDatabaseHelper) {

    fun insertAudit(audit: DbToolAudit) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", audit.id)
            put("task_id", audit.taskId)
            put("tool_name", audit.toolName)
            put("risk_level", audit.riskLevel)
            put("decision", audit.decision)
            put("result_summary", audit.resultSummary)
            put("timestamp", audit.timestamp)
        }
        db.insertWithOnConflict(Ai4MeDatabaseHelper.TABLE_TOOL_AUDITS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAuditsForTask(taskId: String): List<DbToolAudit> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            Ai4MeDatabaseHelper.TABLE_TOOL_AUDITS,
            null,
            "task_id = ?",
            arrayOf(taskId),
            null, null,
            "timestamp ASC"
        )
        val list = mutableListOf<DbToolAudit>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseAudit(it))
            }
        }
        return list
    }

    private fun parseAudit(cursor: Cursor): DbToolAudit {
        return DbToolAudit(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            taskId = cursor.getString(cursor.getColumnIndexOrThrow("task_id")),
            toolName = cursor.getString(cursor.getColumnIndexOrThrow("tool_name")),
            riskLevel = cursor.getString(cursor.getColumnIndexOrThrow("risk_level")),
            decision = cursor.getString(cursor.getColumnIndexOrThrow("decision")),
            resultSummary = cursor.getString(cursor.getColumnIndexOrThrow("result_summary")),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
        )
    }
}
