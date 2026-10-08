package dev.deviceagent.core.agent

import dev.deviceagent.core.ai.AIProvider
import dev.deviceagent.core.ai.GenerateRequest
import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.ai.ProviderErrorKind
import dev.deviceagent.core.ai.ProviderException
import dev.deviceagent.core.ai.ToolDeclaration
import dev.deviceagent.core.ai.ToolResponse
import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.history.AuditLog
import dev.deviceagent.core.history.InMemoryAuditLog
import dev.deviceagent.core.history.UndoManager
import dev.deviceagent.core.policy.ConfirmationResponse
import dev.deviceagent.core.policy.PolicyDecision
import dev.deviceagent.core.policy.PolicyEngine
import dev.deviceagent.core.policy.PolicyView
import dev.deviceagent.core.policy.UserChannel
import dev.deviceagent.core.recovery.ErrorAnalyzer
import dev.deviceagent.core.security.Redactor
import dev.deviceagent.core.tools.ArgValidator
import dev.deviceagent.core.tools.ErrorKind
import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ParamType
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.Tool
import dev.deviceagent.core.tools.ToolCall
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolError
import dev.deviceagent.core.tools.ToolRegistry
import dev.deviceagent.core.tools.ToolResult
import dev.deviceagent.core.util.canonicalJson
import dev.deviceagent.core.util.jsonObjectOf
import dev.deviceagent.core.util.str
import dev.deviceagent.core.verify.VerificationResult
import dev.deviceagent.core.verify.VerifierRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

internal const val ASK_USER = "agent.ask_user"
internal const val ASK_USER_DESCRIPTION = "Ask the user a question when the request is ambiguous or a decision is theirs. Use sparingly."

/**
 * The observe → diagnose → recover loop.
 *
 *   model proposes tool calls → validate → plan (no side effects) → policy → [confirm] →
 *   execute → verify real state → on failure: diagnose, remember, bound the retries → back to model.
 *
 * Pure Kotlin: no Android types. The model never touches an executor directly, it only
 * proposes calls that this class authorizes, runs and checks.
 */
class AgentEngine(
    private val provider: AIProvider,
    private val registry: ToolRegistry,
    private val fs: FileSystem,
    private val policy: PolicyEngine,
    private val user: UserChannel,
    private val verifiers: VerifierRegistry = VerifierRegistry.defaults(),
    val undoManager: UndoManager = UndoManager(),
    private val redactor: Redactor = Redactor(),
    val audit: AuditLog = InMemoryAuditLog(redactor),
    private val log: (String) -> Unit = {},
    private val newTaskId: () -> String = { "task-" + UUID.randomUUID().toString().take(8) },
) {
    private enum class IssueKind { FAILED, DECLINED, BLOCKED, UNSAFE, WAITING_USER }

    private class Issue(val tool: String, val keys: Set<String>, val kind: IssueKind, val error: ToolError?, val note: String)

    private class Run(val taskId: String, val ctx: ToolContext) {
        var steps = 0
        var toolCalls = 0
        var failures = 0
        var stateVersion = 0
        var filesTouched = 0
        var bytesTouched = 0L
        var mutatingSuccesses = 0
        var unverified = 0
        var cancelled = false
        val grants = HashSet<String>()
        val failedAt = HashMap<String, Int>()
        val attemptsPerItem = HashMap<String, Int>()
        val issues = ArrayList<Issue>()
        val attemptLog = ArrayList<AttemptRecord>()
        val proposals = ArrayList<Plan>()
    }

    suspend fun run(goal: String, config: AgentConfig): TaskOutcome {
        val taskId = newTaskId()
        val run = Run(taskId, ToolContext(taskId, fs))
        val declarations = registry.specs().map { ToolDeclaration(it.name, it.description, it.params) } + ToolDeclaration(
            ASK_USER,
            ASK_USER_DESCRIPTION,
            listOf(ParamSpec("question", ParamType.STRING, "A precise question")),
        )
        val system = SystemPrompt.build(fs.roots, config.dryRun, policy.config.mode, registry.specs())
        val messages = ArrayList<Message>().apply { add(Message.User(goal)) }
        audit.record(taskId, "task_start", jsonObjectOf("goal" to goal, "model" to config.model, "dry_run" to config.dryRun, "mode" to policy.config.mode.name))
        log("Task $taskId started")

        var finalText: String? = null
        var stopped: String? = null
        var providerError: ProviderException? = null

        while (true) {
            if (run.cancelled) { stopped = "cancelled by the user"; break }
            if (run.steps >= config.maxSteps) { stopped = "the limit of ${config.maxSteps} AI requests for one task was reached"; break }
            if (run.failures >= config.maxTotalFailures) { stopped = "too many failed operations (${run.failures})"; break }
            run.steps++
            val response = try {
                // snapshot: a provider must never see the history mutate after it was handed over
                provider.generate(GenerateRequest(config.model, system, messages.toList(), declarations, config.temperature))
            } catch (e: ProviderException) {
                providerError = e
                audit.record(taskId, "provider_error", jsonObjectOf("kind" to e.kind.name, "message" to e.message))
                break
            }
            if (response.toolCalls.isEmpty()) { finalText = response.text ?: ""; break }
            response.text?.takeIf { it.isNotBlank() }?.let { log("AI: $it") }
            messages += Message.Assistant(response.text, response.toolCalls)
            val replies = ArrayList<ToolResponse>()
            for (call in response.toolCalls) {
                val content = if (run.cancelled) {
                    jsonObjectOf("ok" to false, "error" to jsonObjectOf("kind" to "CANCELLED", "message" to "The task was cancelled by the user."))
                } else handle(run, call, config)
                replies += ToolResponse(call.id, call.name, content)
            }
            messages += Message.ToolResults(replies)
        }

        val status = determineStatus(run, stopped, providerError, config.dryRun)
        val report = buildReport(run, status, stopped, providerError)
        audit.record(taskId, "task_end", jsonObjectOf("status" to status.name, "failures" to run.failures, "tool_calls" to run.toolCalls))
        log("Task $taskId finished: $status")
        return TaskOutcome(
            taskId = taskId, status = status, modelMessage = finalText?.takeIf { it.isNotBlank() }, report = report,
            steps = run.steps, toolCalls = run.toolCalls, failures = run.failures, attempts = run.attemptLog.toList(),
            proposals = run.proposals.toList(), canUndo = undoManager.hasUndo(taskId), unverifiedChanges = run.unverified,
            stoppedReason = stopped, providerError = providerError?.let { "${it.kind}: ${it.message}" },
        )
    }

    // ───────────────────────────── one tool call ─────────────────────────────

    private suspend fun handle(run: Run, call: ToolCall, config: AgentConfig): JsonObject {
        run.toolCalls++
        audit.record(run.taskId, "tool_call", jsonObjectOf("tool" to call.name, "args" to call.args))
        if (run.toolCalls > config.maxToolCallsPerTask) {
            run.failures++
            return errorResponse(ToolError(ErrorKind.BUDGET_EXHAUSTED, "The limit of ${config.maxToolCallsPerTask} tool calls for one task was reached. Stop and report."))
        }
        if (call.name == ASK_USER) return handleAsk(run, call)

        val tool = registry.get(call.name)
        if (tool == null) {
            run.failures++
            return errorResponse(ToolError(ErrorKind.UNKNOWN_TOOL, "Unknown tool '${call.name}'. Available tools: ${registry.specs().joinToString { it.name }}"))
        }
        val problems = ArgValidator.validate(tool.spec, call.args)
        if (problems.isNotEmpty()) {
            run.failures++
            return errorResponse(ToolError(ErrorKind.INVALID_ARGUMENTS, problems.joinToString("; ")))
        }

        val fingerprint = call.name + canonicalJson(call.args)
        if (run.failedAt[fingerprint] == run.stateVersion) {
            run.failures++
            return errorResponse(ToolError(ErrorKind.REPEATED_STRATEGY, "This exact call already failed and nothing has changed since. Change the approach or investigate first."))
        }
        val mutating = tool.spec.mutating
        val item = itemKey(call.args)
        if (mutating && (run.attemptsPerItem[item] ?: 0) >= 1 + config.maxRecoveryAttempts) {
            run.failures++
            return errorResponse(ToolError(ErrorKind.BUDGET_EXHAUSTED, "Automatic recovery limit reached for '$item' after ${run.attemptsPerItem[item]} failed attempts. Do not retry; report to the user what was tried and what is needed."))
        }

        // 1. plan: what would this do? (no side effects)
        val plan = tool.plan(call.args, run.ctx)
        if (plan.error != null) return failed(run, tool, call, plan, plan.error, config)

        // 2. policy: who allows it?
        val decision = policy.evaluate(tool.spec, plan, PolicyView(run.filesTouched, run.bytesTouched, run.grants))
        audit.record(run.taskId, "policy", jsonObjectOf("tool" to call.name, "decision" to decision::class.simpleName, "plan" to plan.summary))
        when (decision) {
            is PolicyDecision.Deny -> {
                if (decision.proposal) return proposed(run, plan, "Manual mode: recorded as a proposal, not executed.")
                run.issues += Issue(call.name, keysOf(call.args), if (decision.unsafe) IssueKind.UNSAFE else IssueKind.BLOCKED, null, decision.reason)
                return jsonObjectOf("ok" to false, "error" to jsonObjectOf("kind" to "POLICY_DENIED", "message" to decision.reason, "guidance" to "This will not be allowed. Do not look for a workaround; explain it to the user."))
            }
            is PolicyDecision.Confirm -> {
                if (config.dryRun) return proposed(run, plan, "Dry run: would ask the user to confirm (${decision.request.risk}).")
                val answer = user.confirm(decision.request)
                audit.record(run.taskId, "confirmation", jsonObjectOf("tool" to call.name, "answer" to answer.name))
                when (answer) {
                    ConfirmationResponse.ALLOW_ONCE -> {}
                    ConfirmationResponse.ALLOW_FOR_TASK -> decision.request.grantScope?.let { run.grants += it }
                    ConfirmationResponse.DENY -> {
                        run.issues += Issue(call.name, keysOf(call.args), IssueKind.DECLINED, null, "the user declined: ${plan.summary}")
                        return jsonObjectOf("ok" to false, "error" to jsonObjectOf("kind" to "DECLINED_BY_USER", "message" to "The user declined this action.", "guidance" to "Do not retry it or work around it. Offer an alternative or finish."))
                    }
                    ConfirmationResponse.CANCEL_TASK -> {
                        run.cancelled = true
                        return jsonObjectOf("ok" to false, "error" to jsonObjectOf("kind" to "CANCELLED", "message" to "The user cancelled the task."))
                    }
                }
            }
            is PolicyDecision.Allow -> {}
        }
        if (mutating && config.dryRun) return proposed(run, plan, "Dry run: not executed.")

        // 3. execute
        val result = try {
            tool.execute(call.args, run.ctx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ToolResult.failure(ErrorKind.UNKNOWN, "${e.javaClass.simpleName}: ${e.message}")
        }
        if (mutating) run.stateVersion++ // something may have changed, even on failure
        if (!result.ok) {
            undoManager.record(run.taskId, result.undo)
            return failed(run, tool, call, plan, result.error ?: ToolError(ErrorKind.UNKNOWN, "Unknown failure"), config)
        }

        // 4. verify the REAL state
        var verification: VerificationResult? = null
        if (mutating) {
            val verifier = verifiers.forTool(call.name)
            if (verifier == null) run.unverified++
            else {
                verification = try {
                    verifier.verify(call, plan, result, run.ctx)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    VerificationResult(false, emptyList(), listOf("verification crashed: ${e.javaClass.simpleName}"))
                }
                if (!verification.passed) {
                    val rollback = undoManager.revert(result.undo, fs)
                    run.stateVersion++
                    val error = ToolError(ErrorKind.VERIFICATION_FAILED, verification.failures.joinToString("; "), plan.writes.firstOrNull())
                    return failed(run, tool, call, plan, error, config, verification, rolledBack = rollback.reverted)
                }
            }
            undoManager.record(run.taskId, result.undo)
            run.mutatingSuccesses++
            run.filesTouched += plan.fileCount
            run.bytesTouched += plan.bytes
            val keys = keysOf(call.args)
            run.issues.removeAll { it.tool == call.name && it.kind == IssueKind.FAILED && it.keys.any { k -> k in keys } }
        }
        audit.record(run.taskId, "tool_result", jsonObjectOf("tool" to call.name, "ok" to true, "verification" to verification?.let { jsonObjectOf("passed" to it.passed, "checks" to it.checks) }))
        return successResponse(result, verification, mutating, config)
    }

    private suspend fun handleAsk(run: Run, call: ToolCall): JsonObject {
        val question = call.args.str("question")
            ?: return errorResponse(ToolError(ErrorKind.INVALID_ARGUMENTS, "missing required parameter 'question'")).also { run.failures++ }
        val answer = user.ask(question)
        audit.record(run.taskId, "ask_user", jsonObjectOf("question" to question, "answered" to (answer != null)))
        if (answer == null) {
            run.issues += Issue(ASK_USER, setOf(question), IssueKind.WAITING_USER, null, "waiting for the user's answer: $question")
            return jsonObjectOf("ok" to false, "error" to jsonObjectOf("kind" to "NO_USER_AVAILABLE", "message" to "The user cannot answer right now. Stop, and summarize exactly what you need to know."))
        }
        return jsonObjectOf("ok" to true, "answer" to answer)
    }

    // ───────────────────────────── outcomes of a call ─────────────────────────────

    private fun proposed(run: Run, plan: Plan, note: String): JsonObject {
        run.proposals += plan
        return jsonObjectOf("ok" to true, "executed" to false, "would_do" to plan.summary, "files" to plan.fileCount, "bytes" to plan.bytes, "note" to note)
    }

    private fun failed(
        run: Run, tool: Tool, call: ToolCall, plan: Plan, error: ToolError, config: AgentConfig,
        verification: VerificationResult? = null, rolledBack: Int = 0,
    ): JsonObject {
        run.failures++
        run.failedAt[call.name + canonicalJson(call.args)] = run.stateVersion
        val diagnosis = ErrorAnalyzer.analyze(error, fs.roots)
        if (tool.spec.mutating) {
            val item = itemKey(call.args)
            val n = (run.attemptsPerItem[item] ?: 0) + 1
            run.attemptsPerItem[item] = n
            val keys = keysOf(call.args)
            run.issues.removeAll { it.tool == call.name && it.kind == IssueKind.FAILED && it.keys.any { k -> k in keys } }
            run.issues += Issue(call.name, keys, IssueKind.FAILED, error, "${plan.summary}: ${error.message}")
            run.attemptLog += AttemptRecord(run.steps, call.name, item, error.kind.name, error.message, diagnosis.explanation, n)
        }
        audit.record(run.taskId, "tool_result", jsonObjectOf("tool" to call.name, "ok" to false, "error_kind" to error.kind.name, "message" to error.message, "rolled_back" to rolledBack, "diagnosis" to diagnosis.category))
        return errorResponse(error, diagnosis.explanation, diagnosis.suggestions, diagnosis.action.name, verification, rolledBack)
    }

    private fun errorResponse(
        error: ToolError, explanation: String? = null, suggestions: List<String> = emptyList(), action: String? = null,
        verification: VerificationResult? = null, rolledBack: Int = 0,
    ): JsonObject {
        val diag = explanation ?: ErrorAnalyzer.analyze(error, fs.roots).let { d -> d.explanation }
        val sugg = if (explanation != null) suggestions else ErrorAnalyzer.analyze(error, fs.roots).suggestions
        return jsonObjectOf(
            "ok" to false,
            "error" to jsonObjectOf(
                "kind" to error.kind.name, "message" to redactor.redact(error.message), "path" to error.path,
                "diagnosis" to diag, "suggestions" to sugg, "suggested_action" to action,
            ),
            "verification" to verification?.let { jsonObjectOf("passed" to false, "checks" to it.checks, "failures" to it.failures) },
            "rolled_back_changes" to rolledBack.takeIf { it > 0 },
        )
    }

    private fun successResponse(result: ToolResult, verification: VerificationResult?, mutating: Boolean, config: AgentConfig): JsonObject {
        val data = redactor.redact(result.data) as JsonObject
        val capped = if (canonicalJson(data).length <= config.maxResultChars) data
        else jsonObjectOf("truncated" to true, "preview" to canonicalJson(data).take(config.maxResultChars))
        return jsonObjectOf(
            "ok" to true,
            // Structural quarantine: anything that came from the device lives under this key.
            "untrusted_data" to capped,
            "verification" to when {
                verification != null -> jsonObjectOf("passed" to true, "checks" to verification.checks)
                mutating -> jsonObjectOf("passed" to null, "note" to "no automatic verification exists for this tool; confirm with a read-only tool")
                else -> null
            },
        )
    }

    // ───────────────────────────── bookkeeping ─────────────────────────────

    /** The thing being worked on, used to bound retries: base name of the first path-like argument. */
    private fun itemKey(args: JsonObject): String {
        val v = listOf("source", "path", "destination", "root").firstNotNullOfOrNull { args.str(it) } ?: return "?"
        return v.trimEnd('/').substringAfterLast('/').lowercase()
    }

    /** Values that identify what a call was about; a later success sharing any of them resolves an earlier failure. */
    private fun keysOf(args: JsonObject): Set<String> {
        val out = HashSet<String>()
        for (v in args.values) {
            val s = (v as? JsonPrimitive)?.takeIf { it.isString }?.content ?: continue
            out += s
            if (s.startsWith("/")) out += "name:" + s.trimEnd('/').substringAfterLast('/').lowercase()
        }
        return out
    }

    private fun determineStatus(run: Run, stopped: String?, providerError: ProviderException?, dryRun: Boolean): TaskStatus {
        if (run.cancelled) return TaskStatus.CANCELLED
        if (providerError != null) {
            return when (providerError.kind) {
                ProviderErrorKind.INVALID_CREDENTIALS -> TaskStatus.WAITING_FOR_USER
                ProviderErrorKind.NETWORK, ProviderErrorKind.RATE_LIMITED -> TaskStatus.WAITING_FOR_RESOURCE
                else -> if (run.mutatingSuccesses > 0) TaskStatus.PARTIAL_SUCCESS else TaskStatus.FAILED_AFTER_INVESTIGATION
            }
        }
        val open = run.issues
        if (open.isEmpty()) {
            return when {
                stopped != null -> if (run.mutatingSuccesses > 0) TaskStatus.PARTIAL_SUCCESS else TaskStatus.FAILED_AFTER_INVESTIGATION
                dryRun -> TaskStatus.PLAN_ONLY
                run.proposals.isNotEmpty() && policy.config.mode == dev.deviceagent.core.policy.InteractionMode.MANUAL -> TaskStatus.WAITING_FOR_USER
                run.failures > 0 -> TaskStatus.SUCCESS_AFTER_RECOVERY
                else -> TaskStatus.SUCCESS
            }
        }
        if (run.mutatingSuccesses > 0) return TaskStatus.PARTIAL_SUCCESS
        return when {
            open.any { it.kind == IssueKind.UNSAFE } -> TaskStatus.UNSAFE_TO_CONTINUE
            open.any { it.kind == IssueKind.BLOCKED } -> TaskStatus.BLOCKED
            open.any { it.kind == IssueKind.DECLINED } -> TaskStatus.WAITING_FOR_PERMISSION
            open.any { it.kind == IssueKind.WAITING_USER } -> TaskStatus.WAITING_FOR_USER
            open.any { it.error?.kind == ErrorKind.NO_SPACE } -> TaskStatus.WAITING_FOR_RESOURCE
            else -> TaskStatus.FAILED_AFTER_INVESTIGATION
        }
    }

    private fun buildReport(run: Run, status: TaskStatus, stopped: String?, providerError: ProviderException?): String = buildString {
        append(status.name)
        appendLine()
        appendLine("${run.mutatingSuccesses} change(s) completed and verified" + if (run.unverified > 0) ", ${run.unverified} could not be verified automatically" else "")
        if (run.failures > 0) appendLine("${run.failures} operation(s) failed along the way")
        if (run.proposals.isNotEmpty()) {
            appendLine("${run.proposals.size} change(s) were planned but NOT executed:")
            run.proposals.take(20).forEach { appendLine("  - ${it.summary}") }
        }
        if (run.issues.isNotEmpty()) {
            appendLine("Unresolved:")
            run.issues.forEach { appendLine("  - ${it.note}") }
        }
        stopped?.let { appendLine("Stopped because $it") }
        providerError?.let { appendLine("AI provider problem: ${it.kind}") }
    }.trimEnd()
}
