package dev.deviceagent.core.tools.file

import dev.deviceagent.core.agent.PrivacyMode
import dev.deviceagent.core.tools.ErrorKind
import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ParamType.INTEGER
import dev.deviceagent.core.tools.ParamType.STRING
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.RiskLevel
import dev.deviceagent.core.tools.Tool
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolResult
import dev.deviceagent.core.tools.ToolSpec
import dev.deviceagent.core.tools.TRASH_DIR
import dev.deviceagent.core.util.jsonObjectOf
import dev.deviceagent.core.util.long
import dev.deviceagent.core.util.str
import kotlinx.serialization.json.JsonObject

const val CONTENT_SEARCH_MAX_BYTES_PER_FILE = 256 * 1024
const val CONTENT_SEARCH_MAX_TOTAL_BYTES = 10 * 1024 * 1024
const val CONTENT_SEARCH_MAX_RESULTS = 200
private const val CONTENT_SEARCH_MAX_WALK_ENTRIES = 50_000

class FileContentSearchTool : Tool {
    override val spec = ToolSpec(
        "file.search_content",
        "Search text content below an approved folder. Binary files are skipped; reading is bounded and results contain only short untrusted snippets.",
        listOf(
            ParamSpec("root", STRING, "Absolute path of the approved folder to search"),
            ParamSpec("query", STRING, "Case-insensitive text to find"),
            ParamSpec("max_results", INTEGER, "Maximum matching lines (default 50)", required = false),
        ),
        RiskLevel.READ,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) =
        Plan("Search content below ${args.str("root")} for ${args.str("query")}", reads = listOfNotNull(args.str("root")))

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val root = args.str("root")!!
        val query = args.str("query")?.trim() ?: ""
        if (query.isEmpty()) return ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "query must not be empty")
        if (ctx.privacyMode == PrivacyMode.NAMES_ONLY) {
            return ToolResult.failure(
                ErrorKind.INVALID_ARGUMENTS,
                "Content search is disabled in names-only privacy mode; explicitly allow file contents first",
                root,
            )
        }
        if (ctx.fs.stat(root)?.isDirectory != true) return ToolResult.failure(ErrorKind.NOT_FOUND, "No such directory", root)

        return guarded {
            val maxResults = (args.long("max_results") ?: 50L).coerceIn(1L, CONTENT_SEARCH_MAX_RESULTS.toLong()).toInt()
            val needle = query.lowercase()
            val matches = ArrayList<JsonObject>()
            var visited = 0
            var scannedFiles = 0
            var skippedBinary = 0
            var totalRead = 0
            var truncated = false

            fun walk(dir: String, depth: Int) {
                if (depth > 15 || matches.size >= maxResults || totalRead >= CONTENT_SEARCH_MAX_TOTAL_BYTES) {
                    truncated = true
                    return
                }
                for (entry in ctx.fs.list(dir)) {
                    if (matches.size >= maxResults || totalRead >= CONTENT_SEARCH_MAX_TOTAL_BYTES) {
                        truncated = true
                        return
                    }
                    if (entry.name == TRASH_DIR || entry.isSymlink) continue
                    if (++visited > CONTENT_SEARCH_MAX_WALK_ENTRIES) {
                        truncated = true
                        return
                    }
                    if (entry.isDirectory) {
                        walk(entry.path, depth + 1)
                        continue
                    }
                    val remaining = CONTENT_SEARCH_MAX_TOTAL_BYTES - totalRead
                    val limit = minOf(CONTENT_SEARCH_MAX_BYTES_PER_FILE, remaining)
                    if (limit <= 0) {
                        truncated = true
                        return
                    }
                    val bytes = ctx.fs.readBytes(entry.path, limit)
                    totalRead += bytes.size
                    val fileTruncated = entry.size > bytes.size
                    val decoded = TextDecoder.decode(bytes, fileTruncated)
                    if (decoded == null) {
                        skippedBinary++
                        continue
                    }
                    scannedFiles++
                    decoded.text.split('\n').forEachIndexed { index, line ->
                        if (matches.size >= maxResults) {
                            truncated = true
                            return@forEachIndexed
                        }
                        if (line.lowercase().contains(needle)) {
                            val snippet = line.trim().take(240)
                            matches += jsonObjectOf(
                                "path" to entry.path,
                                "line" to index + 1,
                                "snippet" to snippet,
                                "truncated_file" to fileTruncated,
                            )
                        }
                    }
                    if (fileTruncated) truncated = true
                }
            }

            walk(root, 0)
            ToolResult.success(
                jsonObjectOf(
                    "root" to root,
                    "query" to query,
                    "matches" to matches,
                    "scanned_files" to scannedFiles,
                    "skipped_binary" to skippedBinary,
                    "bytes_read" to totalRead,
                    "truncated" to truncated,
                ),
            )
        }
    }
}
