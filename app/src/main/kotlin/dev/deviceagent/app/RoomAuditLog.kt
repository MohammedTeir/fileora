package dev.deviceagent.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.deviceagent.core.history.AuditEntry
import dev.deviceagent.core.history.AuditLog
import dev.deviceagent.core.history.UndoStore
import dev.deviceagent.core.security.Redactor
import dev.deviceagent.core.tools.UndoAction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Entity(tableName = "audit_entries")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val taskId: String,
    val kind: String,
    val detailsJson: String,
)

@Dao
interface AuditDao {
    @Insert
    fun insert(entry: AuditEntity)

    @Query("SELECT * FROM audit_entries WHERE (:taskId IS NULL OR taskId = :taskId) ORDER BY id ASC")
    fun entries(taskId: String?): List<AuditEntity>

    @Query("DELETE FROM audit_entries WHERE taskId = :taskId")
    fun deleteTask(taskId: String)
}

@Entity(tableName = "undo_actions")
data class UndoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val kind: String,
    val path: String,
    val otherPath: String? = null,
    val previous: ByteArray? = null,
    val existed: Boolean = false,
)

@Dao
interface UndoDao {
    @Insert
    fun insertAll(actions: List<UndoEntity>)

    @Query("SELECT * FROM undo_actions WHERE taskId = :taskId ORDER BY id ASC")
    fun load(taskId: String): List<UndoEntity>

    @Query("SELECT COUNT(*) FROM undo_actions WHERE taskId = :taskId")
    fun count(taskId: String): Int

    @Query("DELETE FROM undo_actions WHERE taskId = :taskId")
    fun clear(taskId: String)

    @Query("SELECT taskId FROM undo_actions ORDER BY id DESC LIMIT 1")
    fun latestTaskId(): String?
}

@Database(entities = [AuditEntity::class, UndoEntity::class], version = 1, exportSchema = false)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
    abstract fun undoDao(): UndoDao

    companion object {
        @Volatile private var instance: AgentDatabase? = null

        fun get(context: Context): AgentDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AgentDatabase::class.java,
                "device-agent.db",
            ).build().also { instance = it }
        }
    }
}

/** Persistent audit storage. Redaction happens before the JSON reaches Room. */
class RoomAuditLog(
    private val dao: AuditDao,
    private val redactor: Redactor,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : AuditLog {
    override fun record(taskId: String, kind: String, details: kotlinx.serialization.json.JsonObject) {
        val safe = redactor.redact(details)
        dao.insert(AuditEntity(timestampMs = nowMs(), taskId = taskId, kind = kind, detailsJson = safe.toString()))
    }

    override fun entries(taskId: String?): List<AuditEntry> = dao.entries(taskId).mapNotNull { row ->
        runCatching {
            AuditEntry(row.timestampMs, row.taskId, row.kind, Json.parseToJsonElement(row.detailsJson).jsonObject)
        }.getOrNull()
    }
}

class RoomUndoStore(private val dao: UndoDao) : UndoStore {
    override fun append(taskId: String, actions: List<UndoAction>) {
        dao.insertAll(actions.map { action ->
            when (action) {
                is UndoAction.MoveBack -> UndoEntity(taskId = taskId, kind = "move", path = action.from, otherPath = action.to)
                is UndoAction.DeleteCreated -> UndoEntity(taskId = taskId, kind = "delete", path = action.path)
                is UndoAction.RestoreFile -> UndoEntity(taskId = taskId, kind = "restore", path = action.path, previous = action.previous, existed = action.existed)
            }
        })
    }

    override fun load(taskId: String): List<UndoAction> = dao.load(taskId).mapNotNull { row ->
        when (row.kind) {
            "move" -> row.otherPath?.let { UndoAction.MoveBack(row.path, it) }
            "delete" -> UndoAction.DeleteCreated(row.path)
            "restore" -> UndoAction.RestoreFile(row.path, row.previous, row.existed)
            else -> null
        }
    }

    override fun clear(taskId: String) = dao.clear(taskId)
    override fun latestTaskId(): String? = dao.latestTaskId()
}
