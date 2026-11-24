package ai.ai4me.data.db

import android.content.ContentValues
import android.database.Cursor

class TaskDao(private val dbHelper: Ai4MeDatabaseHelper) {

    fun insertTask(task: DbTask) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", task.id)
            put("goal", task.goal)
            put("status", task.status)
            put("created_at", task.createdAt)
            put("updated_at", task.updatedAt)
            put("current_step_index", task.currentStepIndex)
            put("max_steps", task.maxSteps)
            put("summary", task.summary)
        }
        db.insertWithOnConflict(Ai4MeDatabaseHelper.TABLE_TASKS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateTaskStatus(id: String, status: String, summary: String? = null) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("status", status)
            put("updated_at", System.currentTimeMillis())
            if (summary != null) put("summary", summary)
        }
        db.update(Ai4MeDatabaseHelper.TABLE_TASKS, values, "id = ?", arrayOf(id))
    }

    fun insertTaskStep(step: DbTaskStep) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", step.id)
            put("task_id", step.taskId)
            put("step_order", step.stepOrder)
            put("tool_name", step.toolName)
            put("arguments_json", step.argumentsJson)
            put("status", step.status)
            put("result_json", step.resultJson)
            put("verification_evidence", step.verificationEvidence)
            put("started_at", step.startedAt)
            put("completed_at", step.completedAt)
        }
        db.insertWithOnConflict(Ai4MeDatabaseHelper.TABLE_TASK_STEPS, null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateTaskStep(id: String, status: String, resultJson: String?, verificationEvidence: String?, completedAt: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("status", status)
            put("result_json", resultJson)
            put("verification_evidence", verificationEvidence)
            put("completed_at", completedAt)
        }
        db.update(Ai4MeDatabaseHelper.TABLE_TASK_STEPS, values, "id = ?", arrayOf(id))
    }

    fun getAllTasks(limit: Int = 30): List<DbTask> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            Ai4MeDatabaseHelper.TABLE_TASKS,
            null,
            null, null, null, null,
            "created_at DESC",
            limit.toString()
        )
        val list = mutableListOf<DbTask>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseTask(it))
            }
        }
        return list
    }

    fun getTaskSteps(taskId: String): List<DbTaskStep> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            Ai4MeDatabaseHelper.TABLE_TASK_STEPS,
            null,
            "task_id = ?",
            arrayOf(taskId),
            null, null,
            "step_order ASC"
        )
        val list = mutableListOf<DbTaskStep>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(parseTaskStep(it))
            }
        }
        return list
    }

    private fun parseTask(cursor: Cursor): DbTask {
        return DbTask(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            goal = cursor.getString(cursor.getColumnIndexOrThrow("goal")),
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
            currentStepIndex = cursor.getInt(cursor.getColumnIndexOrThrow("current_step_index")),
            maxSteps = cursor.getInt(cursor.getColumnIndexOrThrow("max_steps")),
            summary = cursor.getString(cursor.getColumnIndexOrThrow("summary"))
        )
    }

    private fun parseTaskStep(cursor: Cursor): DbTaskStep {
        return DbTaskStep(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            taskId = cursor.getString(cursor.getColumnIndexOrThrow("task_id")),
            stepOrder = cursor.getInt(cursor.getColumnIndexOrThrow("step_order")),
            toolName = cursor.getString(cursor.getColumnIndexOrThrow("tool_name")),
            argumentsJson = cursor.getString(cursor.getColumnIndexOrThrow("arguments_json")),
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
            resultJson = cursor.getString(cursor.getColumnIndexOrThrow("result_json")),
            verificationEvidence = cursor.getString(cursor.getColumnIndexOrThrow("verification_evidence")),
            startedAt = cursor.getLong(cursor.getColumnIndexOrThrow("started_at")),
            completedAt = if (cursor.isNull(cursor.getColumnIndexOrThrow("completed_at"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("completed_at"))
        )
    }
}
