package dev.deviceagent.app

import dev.deviceagent.core.agent.TaskStatus
import dev.deviceagent.core.history.AuditEntry
import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.tools.RiskLevel
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Everything the user reads goes through here, so raw enum names (FAILED_AFTER_INVESTIGATION),
 * tool names (file.move), task ids and byte counts never reach the screen.
 */

enum class Tone { SUCCESS, INFO, WARNING, ERROR }

fun statusTone(status: TaskStatus): Tone = when (status) {
    TaskStatus.SUCCESS, TaskStatus.SUCCESS_AFTER_RECOVERY -> Tone.SUCCESS
    TaskStatus.PLAN_ONLY, TaskStatus.CANCELLED -> Tone.INFO
    TaskStatus.PARTIAL_SUCCESS, TaskStatus.WAITING_FOR_USER, TaskStatus.WAITING_FOR_PERMISSION,
    TaskStatus.WAITING_FOR_RESOURCE, TaskStatus.BLOCKED -> Tone.WARNING
    TaskStatus.UNSAFE_TO_CONTINUE, TaskStatus.FAILED_AFTER_INVESTIGATION -> Tone.ERROR
}

fun statusTitle(status: TaskStatus): String = when (status) {
    TaskStatus.SUCCESS -> "All done"
    TaskStatus.SUCCESS_AFTER_RECOVERY -> "Done (after a retry)"
    TaskStatus.PARTIAL_SUCCESS -> "Partly done"
    TaskStatus.PLAN_ONLY -> "Preview ready"
    TaskStatus.WAITING_FOR_USER -> "I need your answer"
    TaskStatus.WAITING_FOR_PERMISSION -> "Permission needed"
    TaskStatus.WAITING_FOR_RESOURCE -> "Waiting for something"
    TaskStatus.BLOCKED -> "I couldn't continue"
    TaskStatus.UNSAFE_TO_CONTINUE -> "Stopped to keep your files safe"
    TaskStatus.FAILED_AFTER_INVESTIGATION -> "Something went wrong"
    TaskStatus.CANCELLED -> "Stopped"
}

fun statusExplanation(status: TaskStatus): String = when (status) {
    TaskStatus.SUCCESS -> "Everything was done and checked."
    TaskStatus.SUCCESS_AFTER_RECOVERY -> "A step failed at first, but I found another way and finished."
    TaskStatus.PARTIAL_SUCCESS -> "Some steps worked and some didn't. See what's left below."
    TaskStatus.PLAN_ONLY -> "Nothing on your device was changed."
    TaskStatus.WAITING_FOR_USER -> "I stopped because a decision is yours to make."
    TaskStatus.WAITING_FOR_PERMISSION -> "Android needs your permission before I can continue."
    TaskStatus.WAITING_FOR_RESOURCE -> "Something I need (like storage or a connection) isn't available right now."
    TaskStatus.BLOCKED -> "I stopped before finishing to stay within the task limits. Details are below."
    TaskStatus.UNSAFE_TO_CONTINUE -> "Continuing could have damaged something, so I stopped."
    TaskStatus.FAILED_AFTER_INVESTIGATION -> "I tried, but couldn't finish. Details are below."
    TaskStatus.CANCELLED -> "You stopped this task."
}

/** The engine's report starts with the raw status name; drop it and translate AI-service errors. */
fun friendlyReport(report: String): String {
    val lines = report.lines()
    val body = if (lines.firstOrNull()?.trim()?.matches(Regex("[A-Z_]+")) == true) lines.drop(1) else lines
    val problem = Regex("^AI provider problem: ([A-Z_]+)$")
    return body.joinToString("\n") { line ->
        val m = problem.find(line.trim())
        if (m != null) providerProblem(m.groupValues[1]) else line
    }.trim()
}

/** AI notes are plain Compose Text, not a Markdown renderer; remove common Markdown syntax. */
fun formatAssistantNote(note: String): String {
    var text = note.replace("\r\n", "\n").replace('\r', '\n')
    text = text.replace(Regex("```[A-Za-z0-9_+.-]*\\n?"), "").replace("```", "")
    text = text.replace(Regex("`([^`\\n]+)`"), "$1")
    text = text.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
    text = text.replace(Regex("__(.*?)__"), "$1")
    text = text.replace(Regex("(?m)^\\s*[-*+]\\s+"), "• ")
    text = text.replace(Regex("(?m)^\\s*#{1,6}\\s+"), "")
    text = text.replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
    return text.lines()
        .map { it.trimEnd() }
        .fold(ArrayList<String>()) { out, line ->
            if (line.isNotBlank() || out.lastOrNull()?.isNotBlank() == true) out += line
            out
        }
        .joinToString("\n")
        .trim()
}

fun providerProblem(kind: String?): String = when (kind) {
    "INVALID_CREDENTIALS" -> "Your API key was rejected. Check that you pasted it correctly in Settings."
    "RATE_LIMITED" -> "The AI service is busy or you've reached a usage limit. Try again in a little while."
    "MODEL_UNAVAILABLE" -> "That AI model isn't available. Pick another one in Settings."
    "CONTEXT_TOO_LARGE" -> "The job was too big for the AI to handle at once. Try a smaller folder or a more specific request."
    "BAD_REQUEST" -> "The AI service couldn't understand the request."
    "SAFETY_BLOCKED" -> "The AI service blocked this request."
    "NETWORK" -> "Couldn't reach the AI service. Check your internet connection."
    else -> "The AI service had a problem. Please try again."
}

// ───────── approvals ─────────

private val CHANGE_VERBS = mapOf(
    "file.append" to "add to a file",
    "file.compress" to "zip files",
    "file.copy" to "copy files",
    "file.delete" to "delete files",
    "file.extract" to "unzip files",
    "file.mkdir" to "create folders",
    "file.move" to "move files",
    "file.move_matching" to "move matching files",
    "file.rename" to "rename files",
    "file.write" to "save a file",
    "media.convert" to "convert media",
    "media.extract_audio" to "extract audio",
)

fun isChangeTool(tool: String) = tool in CHANGE_VERBS

/** "move files", "delete files"… */
fun friendlyTool(tool: String): String = CHANGE_VERBS[tool] ?: "make changes"

fun approvalTitle(tool: String): String = "Allow me to ${friendlyTool(tool)}?"

fun riskLabel(risk: RiskLevel): String = when (risk) {
    RiskLevel.READ -> "Only looks at files"
    RiskLevel.LOW -> "Low risk"
    RiskLevel.MODERATE -> "Changes your files"
    RiskLevel.DESTRUCTIVE -> "Deletes or overwrites files"
    RiskLevel.PRIVILEGED -> "Needs special access"
    RiskLevel.CRITICAL -> "Not allowed"
}

fun riskTone(req: ConfirmationRequest): Tone = when {
    !req.reversible || req.risk.level >= RiskLevel.DESTRUCTIVE.level -> Tone.ERROR
    req.risk.level >= RiskLevel.MODERATE.level -> Tone.WARNING
    else -> Tone.INFO
}

/** Body text for the approval notification (the in-app dialog shows the same facts). */
fun approvalDetails(req: ConfirmationRequest): String = buildString {
    append(req.summary)
    if (req.fileCount > 0) append("\n${req.fileCount} file(s) · ${formatBytes(req.bytes)}")
    append(if (req.reversible) "\nYou can undo this later." else "\nThis can't be undone.")
}

// ───────── formatting ─────────

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = -1
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return String.format(Locale.getDefault(), if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
}

fun formatWhen(ms: Long, now: Long = System.currentTimeMillis()): String {
    fun startOfDay(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))
    val days = Math.round((startOfDay(now) - startOfDay(ms)) / 86_400_000.0)
    return when (days) {
        0L -> "Today, $time"
        1L -> "Yesterday, $time"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms)) + ", " + time
    }
}

fun intervalLabel(hours: Long): String = when {
    hours == 1L -> "Every hour"
    hours == 24L -> "Every day"
    hours == 168L -> "Every week"
    hours % 24L == 0L -> "Every ${hours / 24} days"
    else -> "Every $hours hours"
}

fun providerName(p: ProviderChoice): String = when (p) {
    ProviderChoice.GEMINI -> "Google Gemini"
    ProviderChoice.OPENAI -> "OpenAI"
    ProviderChoice.CLAUDE -> "Anthropic Claude"
}

fun cleanProgress(line: String): String = line.trim().removePrefix("•").trim().take(140)

// ───────── setup ─────────

enum class SetupGap { KEY, MODEL, FOLDERS }

fun setupGaps(s: UiState): List<SetupGap> = buildList {
    if (s.apiKeyMask == null) add(SetupGap.KEY)
    if (s.model.isBlank()) add(SetupGap.MODEL)
    if (!s.useAllFilesAccess && s.roots.isEmpty()) add(SetupGap.FOLDERS)
}

/** Picks a sensible default so people never have to type a model id. Prefers fast, current, general models. */
fun suggestModel(provider: ProviderChoice, ids: List<String>): String? {
    val avoid = listOf("lite", "preview", "exp", "vision", "image", "tts", "audio", "embed", "realtime", "search", "live", "thinking", "robotics")
    val clean = ids.filter { id -> avoid.none { id.contains(it, ignoreCase = true) } }
    val wanted = when (provider) {
        ProviderChoice.GEMINI -> listOf("flash")
        ProviderChoice.OPENAI -> listOf("mini")
        ProviderChoice.CLAUDE -> listOf("sonnet", "haiku")
    }
    for (w in wanted) {
        clean.filter { it.contains(w, ignoreCase = true) }.maxOrNull()?.let { return it }
    }
    return clean.firstOrNull() ?: ids.firstOrNull()
}

// ───────── history ─────────

/** One readable line per meaningful audit event; everything else is noise for a normal user. */
fun describeAudit(e: AuditEntry): String? {
    val d = e.details
    fun str(key: String): String? = d[key]?.jsonPrimitive?.contentOrNull
    return when (e.kind) {
        "confirmation" -> {
            val what = friendlyTool(str("tool").orEmpty())
            when (str("answer")) {
                "ALLOW_ONCE" -> "You allowed me to $what"
                "ALLOW_FOR_TASK" -> "You allowed me to $what for the whole task"
                "DENY" -> "You said no to: $what"
                "CANCEL_TASK" -> "You stopped the task"
                else -> null
            }
        }
        "tool_result" -> {
            val tool = str("tool").orEmpty()
            val ok = d["ok"]?.jsonPrimitive?.booleanOrNull
            when {
                !isChangeTool(tool) -> null
                ok == true -> "Done: ${friendlyTool(tool)}"
                ok == false -> "Couldn't ${friendlyTool(tool)}" + (str("message")?.let { ": ${it.take(120)}" } ?: "")
                else -> null
            }
        }
        "provider_error" -> providerProblem(str("kind"))
        "ask_user" -> str("question")?.let { "I asked: $it" }
        else -> null
    }
}
