package dev.deviceagent.core.history

import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.fs.FsException
import dev.deviceagent.core.security.Redactor
import dev.deviceagent.core.tools.UndoAction
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class AuditEntry(val timestampMs: Long, val taskId: String, val kind: String, val details: JsonObject)

interface AuditLog {
    fun record(taskId: String, kind: String, details: JsonObject)
    fun entries(taskId: String? = null): List<AuditEntry>
}

/** Durable storage for reversible actions. Android supplies a Room implementation; core stays platform-neutral. */
interface UndoStore {
    fun append(taskId: String, actions: List<UndoAction>)
    fun load(taskId: String): List<UndoAction>
    fun clear(taskId: String)
    fun latestTaskId(): String? = null
}

/** Everything is redacted before it is stored. The app swaps in a Room-backed implementation. */
class InMemoryAuditLog(
    private val redactor: Redactor,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : AuditLog {
    private val list = CopyOnWriteArrayList<AuditEntry>()

    override fun record(taskId: String, kind: String, details: JsonObject) {
        list += AuditEntry(nowMs(), taskId, kind, redactor.redact(details) as JsonObject)
    }

    override fun entries(taskId: String?) = if (taskId == null) list.toList() else list.filter { it.taskId == taskId }
}

data class UndoReport(val reverted: Int, val notes: List<String>)

/** Keeps the reversible actions of each task and applies them newest-first. */
class UndoManager(private val store: UndoStore? = null) {
    private val transactions = ConcurrentHashMap<String, MutableList<UndoAction>>()

    fun record(taskId: String, actions: List<UndoAction>) {
        if (actions.isEmpty()) return
        transactions.getOrPut(taskId) { CopyOnWriteArrayList() }.addAll(actions)
        store?.append(taskId, actions)
    }

    fun hasUndo(taskId: String) = transactions[taskId]?.isNotEmpty() == true || store?.load(taskId)?.isNotEmpty() == true
    fun pending(taskId: String): List<UndoAction> = transactions[taskId]?.toList() ?: store?.load(taskId).orEmpty()

    fun undo(taskId: String, fs: FileSystem): UndoReport {
        val actions = transactions.remove(taskId) ?: store?.load(taskId).orEmpty()
        if (actions.isEmpty()) return UndoReport(0, listOf("Nothing to undo"))
        store?.clear(taskId)
        return revert(actions, fs)
    }

    /** Applies [actions] newest-first without touching any stored transaction. */
    fun revert(actions: List<UndoAction>, fs: FileSystem): UndoReport {
        var reverted = 0
        val notes = ArrayList<String>()
        for (a in actions.reversed()) {
            try {
                when (a) {
                    is UndoAction.MoveBack -> {
                        when {
                            fs.stat(a.from) == null -> notes += "Skipped: ${a.from} no longer exists"
                            fs.stat(a.to) != null -> notes += "Skipped: ${a.to} already exists, left ${a.from} in place"
                            else -> {
                                fs.parent(a.to)?.let { fs.mkdirs(it) }
                                fs.move(a.from, a.to)
                                reverted++
                            }
                        }
                    }
                    is UndoAction.DeleteCreated -> {
                        val info = fs.stat(a.path)
                        if (info == null) notes += "Skipped: ${a.path} no longer exists"
                        else try { fs.delete(a.path); reverted++ } catch (e: FsException) {
                            notes += "Left in place: ${a.path} (${e.message})"
                        }
                    }
                    is UndoAction.RestoreFile -> {
                        if (a.existed) {
                            val bytes = a.previous
                            if (bytes == null) notes += "Skipped: previous contents for ${a.path} were unavailable"
                            else {
                                fs.writeBytes(a.path, bytes)
                                reverted++
                            }
                        } else {
                            val info = fs.stat(a.path)
                            if (info == null) notes += "Skipped: ${a.path} no longer exists"
                            else {
                                fs.delete(a.path)
                                reverted++
                            }
                        }
                    }
                }
            } catch (e: FsException) {
                notes += "Could not revert $a: ${e.message}"
            }
        }
        return UndoReport(reverted, notes)
    }
}
