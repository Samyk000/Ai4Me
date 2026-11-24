package ai.ai4me.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite Database Helper (Document 1 §4.7 & Document 2 §5.1)
 * High-performance, zero-bloat persistent storage with indexes for fast memory retrieval.
 */
class Ai4MeDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "ai4me.db"
        const val DATABASE_VERSION = 1

        const val TABLE_CONVERSATIONS = "conversations"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_MEMORIES = "memories"
        const val TABLE_TASKS = "tasks"
        const val TABLE_TASK_STEPS = "task_steps"
        const val TABLE_TOOL_AUDITS = "tool_audits"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Conversations Table
        db.execSQL("""
            CREATE TABLE $TABLE_CONVERSATIONS (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                active_model_id TEXT,
                summary TEXT,
                archived_at INTEGER
            )
        """.trimIndent())

        // Messages Table
        db.execSQL("""
            CREATE TABLE $TABLE_MESSAGES (
                id TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                token_count_estimate INTEGER DEFAULT 0,
                status TEXT DEFAULT 'completed',
                parent_message_id TEXT,
                FOREIGN KEY (conversation_id) REFERENCES $TABLE_CONVERSATIONS(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_messages_conv ON $TABLE_MESSAGES(conversation_id, created_at)")

        // Memories Table (Indexed for keyword search and recency weighting)
        db.execSQL("""
            CREATE TABLE $TABLE_MEMORIES (
                id TEXT PRIMARY KEY,
                category TEXT NOT NULL,
                content TEXT NOT NULL,
                source_conversation_id TEXT,
                source_message_id TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                confidence REAL DEFAULT 1.0,
                sensitivity TEXT DEFAULT 'low',
                expires_at INTEGER,
                user_approved INTEGER DEFAULT 1
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_memories_category ON $TABLE_MEMORIES(category)")
        db.execSQL("CREATE INDEX idx_memories_updated ON $TABLE_MEMORIES(updated_at)")

        // Tasks Table
        db.execSQL("""
            CREATE TABLE $TABLE_TASKS (
                id TEXT PRIMARY KEY,
                goal TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                current_step_index INTEGER DEFAULT 0,
                max_steps INTEGER DEFAULT 5,
                summary TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_tasks_updated ON $TABLE_TASKS(updated_at)")

        // Task Steps Table
        db.execSQL("""
            CREATE TABLE $TABLE_TASK_STEPS (
                id TEXT PRIMARY KEY,
                task_id TEXT NOT NULL,
                step_order INTEGER NOT NULL,
                tool_name TEXT NOT NULL,
                arguments_json TEXT NOT NULL,
                status TEXT NOT NULL,
                result_json TEXT,
                verification_evidence TEXT,
                started_at INTEGER NOT NULL,
                completed_at INTEGER,
                FOREIGN KEY (task_id) REFERENCES $TABLE_TASKS(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_task_steps_task ON $TABLE_TASK_STEPS(task_id, step_order)")

        // Tool Audits Table
        db.execSQL("""
            CREATE TABLE $TABLE_TOOL_AUDITS (
                id TEXT PRIMARY KEY,
                task_id TEXT,
                tool_name TEXT NOT NULL,
                risk_level TEXT NOT NULL,
                decision TEXT NOT NULL,
                result_summary TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_tool_audits_time ON $TABLE_TOOL_AUDITS(timestamp)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade migrations when schema increments
    }
}
