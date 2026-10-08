package dev.deviceagent.core.verify

import dev.deviceagent.core.fs.FsException
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.TRASH_DIR
import dev.deviceagent.core.tools.ToolCall
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolResult
import dev.deviceagent.core.util.str

data class VerificationResult(val passed: Boolean, val checks: List<String>, val failures: List<String>) {
    companion object {
        fun of(checks: List<String>, failures: List<String>) = VerificationResult(failures.isEmpty(), checks, failures)
    }
}

/**
 * Checks the REAL state after a tool reports success. Exit "ok" is never trusted: a copy that
 * produced an empty file must be caught here, not by the model.
 */
interface Verifier {
    suspend fun verify(call: ToolCall, plan: Plan, result: ToolResult, ctx: ToolContext): VerificationResult
}

class VerifierRegistry(verifiers: Map<String, Verifier> = emptyMap()) {
    private val map = LinkedHashMap(verifiers)
    fun register(toolName: String, v: Verifier) { map[toolName] = v }
    fun forTool(name: String): Verifier? = map[name]

    companion object {
        fun defaults() = VerifierRegistry(
            mapOf(
                "file.mkdir" to MkdirVerifier(),
                "file.copy" to CopyVerifier(),
                "file.move" to MoveVerifier(),
                "file.rename" to MoveVerifier(),
                "file.delete" to DeleteVerifier(),
            ),
        )
    }
}

private const val HASH_LIMIT_BYTES = 256L * 1024 * 1024

private fun <T> safely(failures: MutableList<String>, what: String, block: () -> T?): T? =
    try { block() } catch (e: FsException) { failures += "$what: ${e.message}"; null }

/** (files, bytes) of a tree, ignoring symlinks and the trash folder. */
private fun measure(ctx: ToolContext, path: String): Pair<Int, Long> {
    var files = 0; var bytes = 0L
    fun walk(p: String) {
        val i = ctx.fs.stat(p) ?: return
        if (i.isSymlink) return
        if (!i.isDirectory) { files++; bytes += i.size; return }
        ctx.fs.list(p).filter { it.name != TRASH_DIR }.forEach { walk(it.path) }
    }
    walk(path)
    return files to bytes
}

class MkdirVerifier : Verifier {
    override suspend fun verify(call: ToolCall, plan: Plan, result: ToolResult, ctx: ToolContext): VerificationResult {
        val checks = ArrayList<String>(); val failures = ArrayList<String>()
        val path = call.args.str("path") ?: return VerificationResult.of(emptyList(), listOf("no path"))
        val i = safely(failures, "stat $path") { ctx.fs.stat(path) }
        if (i == null && failures.isEmpty()) failures += "directory $path does not exist"
        else if (i != null) { if (i.isDirectory) checks += "directory exists" else failures += "$path exists but is not a directory" }
        return VerificationResult.of(checks, failures)
    }
}

class CopyVerifier : Verifier {
    override suspend fun verify(call: ToolCall, plan: Plan, result: ToolResult, ctx: ToolContext): VerificationResult {
        val checks = ArrayList<String>(); val failures = ArrayList<String>()
        val src = plan.reads.firstOrNull(); val dst = plan.writes.firstOrNull()
        if (src == null || dst == null) return VerificationResult.of(emptyList(), listOf("incomplete plan"))
        val di = safely(failures, "stat $dst") { ctx.fs.stat(dst) }
        if (di == null) { if (failures.isEmpty()) failures += "destination $dst does not exist"; return VerificationResult.of(checks, failures) }
        checks += "destination exists"
        if (!di.isDirectory) {
            if (di.size != plan.bytes) failures += "size mismatch: expected ${plan.bytes} bytes, found ${di.size}"
            else checks += "size matches (${di.size} bytes)"
            if (plan.bytes > 0 && di.size == 0L) failures += "destination is empty"
            if (failures.isEmpty() && plan.bytes in 1..HASH_LIMIT_BYTES) {
                val a = safely(failures, "hash source") { ctx.fs.sha256(src) }
                val b = safely(failures, "hash destination") { ctx.fs.sha256(dst) }
                if (a != null && b != null) { if (a == b) checks += "content hash matches" else failures += "content differs from source" }
            }
        } else {
            val (files, bytes) = safely(failures, "measure $dst") { measure(ctx, dst) } ?: (0 to 0L)
            if (failures.isEmpty()) {
                if (files != plan.fileCount) failures += "expected ${plan.fileCount} files, found $files"
                else if (bytes != plan.bytes) failures += "expected ${plan.bytes} bytes, found $bytes"
                else checks += "all $files file(s) present with matching sizes"
            }
        }
        return VerificationResult.of(checks, failures)
    }
}

/** Used for file.move and file.rename. */
class MoveVerifier : Verifier {
    override suspend fun verify(call: ToolCall, plan: Plan, result: ToolResult, ctx: ToolContext): VerificationResult {
        val checks = ArrayList<String>(); val failures = ArrayList<String>()
        val src = plan.reads.firstOrNull(); val dst = plan.writes.firstOrNull()
        if (src == null || dst == null) return VerificationResult.of(emptyList(), listOf("incomplete plan"))
        val di = safely(failures, "stat $dst") { ctx.fs.stat(dst) }
        if (di == null) { if (failures.isEmpty()) failures += "destination $dst does not exist"; return VerificationResult.of(checks, failures) }
        checks += "destination exists"
        val si = safely(failures, "stat $src") { ctx.fs.stat(src) }
        if (si != null) failures += "source $src still exists" else if (failures.isEmpty()) checks += "source is gone"
        val (files, bytes) = safely(failures, "measure $dst") { measure(ctx, dst) } ?: (0 to 0L)
        if (failures.isEmpty()) {
            if (files != plan.fileCount || bytes != plan.bytes) failures += "expected ${plan.fileCount} file(s)/${plan.bytes} bytes at destination, found $files/$bytes"
            else checks += "contents intact ($files file(s), $bytes bytes)"
        }
        return VerificationResult.of(checks, failures)
    }
}

class DeleteVerifier : Verifier {
    override suspend fun verify(call: ToolCall, plan: Plan, result: ToolResult, ctx: ToolContext): VerificationResult {
        val checks = ArrayList<String>(); val failures = ArrayList<String>()
        val path = plan.deletes.firstOrNull() ?: return VerificationResult.of(emptyList(), listOf("incomplete plan"))
        val trash = result.data.str("trashed_to")
        if (safely(failures, "stat $path") { ctx.fs.stat(path) } != null) failures += "$path still exists" else if (failures.isEmpty()) checks += "original is gone"
        if (trash == null) failures += "no trash location reported"
        else {
            val ti = safely(failures, "stat trash") { ctx.fs.stat(trash) }
            if (ti == null && failures.isEmpty()) failures += "trash copy missing, item may be lost"
            else if (ti != null) checks += "recoverable copy exists in trash"
        }
        return VerificationResult.of(checks, failures)
    }
}
