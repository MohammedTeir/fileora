package dev.deviceagent.core.tools.system

import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ParamType.STRING
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.RiskLevel
import dev.deviceagent.core.tools.Tool
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolResult
import dev.deviceagent.core.tools.ToolSpec
import dev.deviceagent.core.tools.file.FileDeleteTool
import dev.deviceagent.core.tools.file.FileAppendTool
import dev.deviceagent.core.tools.file.FileCopyTool
import dev.deviceagent.core.tools.file.FileHashTool
import dev.deviceagent.core.tools.file.FileListTool
import dev.deviceagent.core.tools.file.FileMkdirTool
import dev.deviceagent.core.tools.file.FileMoveTool
import dev.deviceagent.core.tools.file.FileReadTool
import dev.deviceagent.core.tools.file.FileRenameTool
import dev.deviceagent.core.tools.file.FileSearchTool
import dev.deviceagent.core.tools.file.FileStatTool
import dev.deviceagent.core.tools.file.FileWriteTool
import dev.deviceagent.core.util.jsonObjectOf
import dev.deviceagent.core.util.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class SystemStorageTool : Tool {
    override val spec = ToolSpec(
        "system.storage",
        "Free space (bytes) of an accessible folder, or of every accessible root when no path is given. " +
            "Check this before large copies.",
        listOf(ParamSpec("path", STRING, "Optional absolute path", required = false)),
        RiskLevel.READ,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) = Plan("Check free storage")

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult = withContext(Dispatchers.IO) {
        try {
            val paths = args.str("path")?.let { listOf(it) } ?: ctx.fs.roots
            ToolResult.success(
                jsonObjectOf(
                    "volumes" to paths.map {
                        val free = ctx.fs.freeSpaceBytes(it)
                        jsonObjectOf("path" to it, "free_bytes" to free, "free_space_known" to (free != null))
                    },
                ),
            )
        } catch (e: dev.deviceagent.core.fs.FsException) {
            ToolResult.failure(e.kind, e.message ?: "I/O error", e.path)
        }
    }
}

object DefaultTools {
    fun all(): List<Tool> = listOf(
        FileListTool(), FileSearchTool(), FileStatTool(), FileHashTool(), FileReadTool(),
        FileMkdirTool(), FileCopyTool(), FileWriteTool(), FileAppendTool(), FileMoveTool(), FileRenameTool(), FileDeleteTool(),
        SystemStorageTool(),
    )
}
