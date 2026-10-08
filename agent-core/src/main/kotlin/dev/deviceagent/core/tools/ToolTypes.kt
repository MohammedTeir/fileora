package dev.deviceagent.core.tools

import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.agent.PrivacyMode
import dev.deviceagent.core.tools.media.MediaBackend
import dev.deviceagent.core.util.bool
import dev.deviceagent.core.util.long
import dev.deviceagent.core.util.str
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.atomic.AtomicInteger

/** Name of the per-root folder that holds files "deleted" by the agent (restorable). */
const val TRASH_DIR = ".agent_trash"

enum class RiskLevel(val level: Int) { READ(0), LOW(1), MODERATE(2), DESTRUCTIVE(3), PRIVILEGED(4), CRITICAL(5) }

enum class ParamType { STRING, INTEGER, BOOLEAN }

data class ParamSpec(
    val name: String,
    val type: ParamType,
    val description: String,
    val required: Boolean = true,
)

data class ToolSpec(
    val name: String,
    val description: String,
    val params: List<ParamSpec>,
    val risk: RiskLevel,
    val mutating: Boolean = risk != RiskLevel.READ,
)

enum class ErrorKind {
    PERMISSION_DENIED, NOT_FOUND, ALREADY_EXISTS, NO_SPACE, INVALID_ARGUMENTS, OUTSIDE_SANDBOX, IO,
    VERIFICATION_FAILED, UNKNOWN_TOOL, REPEATED_STRATEGY, BUDGET_EXHAUSTED, UNKNOWN,
}

data class ToolError(val kind: ErrorKind, val message: String, val path: String? = null)

/** How to reverse a change. Recorded in creation order; applied in reverse order. */
sealed interface UndoAction {
    /** Move [from] back to [to] (undo of move / rename / delete-to-trash). */
    data class MoveBack(val from: String, val to: String) : UndoAction

    /** Remove something the task created (a file, or a directory if it is empty). */
    data class DeleteCreated(val path: String) : UndoAction

    /** Restore the previous bytes of a file, or remove it when it did not exist before the write. */
    data class RestoreFile(val path: String, val previous: ByteArray?, val existed: Boolean) : UndoAction
}

/**
 * A tool invocation proposed by the model. [providerData] is opaque provider-specific data
 * (e.g. a signature the provider wants echoed back); the agent core never interprets it.
 */
data class ToolCall(
    val id: String,
    val name: String,
    val args: JsonObject,
    val providerData: JsonElement? = null,
)

data class ToolResult(
    val ok: Boolean,
    val data: JsonObject = JsonObject(emptyMap()),
    val error: ToolError? = null,
    val undo: List<UndoAction> = emptyList(),
) {
    companion object {
        fun success(data: JsonObject = JsonObject(emptyMap()), undo: List<UndoAction> = emptyList()) =
            ToolResult(true, data, null, undo)

        fun failure(kind: ErrorKind, message: String, path: String? = null, undo: List<UndoAction> = emptyList()) =
            ToolResult(false, JsonObject(emptyMap()), ToolError(kind, message, path), undo)
    }
}

/**
 * What a call would do, computed WITHOUT changing anything. Drives the policy engine,
 * dry-run, verification and confirmation text. A plan with [error] means the call cannot work.
 */
data class Plan(
    val summary: String,
    val reads: List<String> = emptyList(),
    val writes: List<String> = emptyList(),
    val deletes: List<String> = emptyList(),
    val fileCount: Int = 0,
    val bytes: Long = 0,
    val reversible: Boolean = true,
    val error: ToolError? = null,
)

class ToolContext(
    val taskId: String,
    val fs: FileSystem,
    val privacyMode: PrivacyMode = PrivacyMode.CONTENTS_ALLOWED,
    val mediaBackend: MediaBackend? = null,
) {
    private val counter = AtomicInteger()
    fun nextId(): Int = counter.incrementAndGet()
}

interface Tool {
    val spec: ToolSpec
    suspend fun plan(args: JsonObject, ctx: ToolContext): Plan
    suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult
}

class ToolRegistry(tools: List<Tool> = emptyList()) {
    private val byName = LinkedHashMap<String, Tool>()

    init { tools.forEach(::register) }

    fun register(tool: Tool) {
        require(tool.spec.name !in byName) { "Duplicate tool ${tool.spec.name}" }
        byName[tool.spec.name] = tool
    }

    fun get(name: String): Tool? = byName[name]
    fun all(): List<Tool> = byName.values.toList()
    fun specs(): List<ToolSpec> = all().map { it.spec }
}

object ArgValidator {
    /** Returns human-readable problems; empty list means the arguments match the schema. */
    fun validate(spec: ToolSpec, args: JsonObject): List<String> {
        val problems = ArrayList<String>()
        val known = spec.params.map { it.name }.toSet()
        for (key in args.keys) if (key !in known) problems += "unexpected parameter '$key'"
        for (p in spec.params) {
            val v = args[p.name]
            if (v == null || v is JsonNull) {
                if (p.required) problems += "missing required parameter '${p.name}'"
                continue
            }
            val ok = when (p.type) {
                ParamType.STRING -> args.str(p.name) != null
                ParamType.INTEGER -> args.long(p.name) != null
                ParamType.BOOLEAN -> args.bool(p.name) != null
            }
            if (!ok) problems += "parameter '${p.name}' must be of type ${p.type.name.lowercase()}"
        }
        return problems
    }
}
