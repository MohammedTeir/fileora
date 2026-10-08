package dev.deviceagent.core.policy

import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.RiskLevel
import dev.deviceagent.core.tools.ToolSpec

enum class InteractionMode {
    /** Ask for anything above [PolicyConfig.autoAllowMaxRisk]. */
    NORMAL,

    /** Tools in [PolicyConfig.autonomousAllowedTools] never ask; destructive/privileged still always ask. */
    AUTONOMOUS,

    /** Read-only tools run; every change is recorded as a proposal and never executed. */
    MANUAL,
}

data class PolicyConfig(
    val mode: InteractionMode = InteractionMode.NORMAL,
    val autoAllowMaxRisk: RiskLevel = RiskLevel.LOW,
    val autonomousAllowedTools: Set<String> = emptySet(),
    val maxFilesPerTask: Int = 5_000,
    val maxBytesPerTask: Long = Long.MAX_VALUE,
    /** Writes/deletes under these prefixes are always refused, whatever the user confirms. */
    val protectedPrefixes: List<String> = listOf("/system", "/proc", "/dev", "/sys", "/data/data", "/data/system", "/vendor"),
)

enum class ConfirmationResponse { ALLOW_ONCE, ALLOW_FOR_TASK, DENY, CANCEL_TASK }

data class ConfirmationRequest(
    val tool: String,
    val risk: RiskLevel,
    val summary: String,
    val affectedSample: List<String>,
    val fileCount: Int,
    val bytes: Long,
    val reversible: Boolean,
    /** Non-null when the user may grant "always allow for this task" for this scope. */
    val grantScope: String?,
)

sealed interface PolicyDecision {
    data class Allow(val reason: String) : PolicyDecision
    data class Confirm(val request: ConfirmationRequest) : PolicyDecision

    /** [proposal] is true in MANUAL mode: not a refusal of the idea, just "do not execute". */
    data class Deny(val reason: String, val proposal: Boolean = false, val unsafe: Boolean = false) : PolicyDecision
}

/** What the engine knows about the running task that policy needs. */
data class PolicyView(val filesTouched: Int, val bytesTouched: Long, val grants: Set<String>)

/**
 * The AI proposes, the policy engine authorizes, the executor performs. This class has no
 * dependency on the model: it only sees the tool's declared risk and the computed [Plan].
 */
class PolicyEngine(val config: PolicyConfig = PolicyConfig()) {

    fun evaluate(spec: ToolSpec, plan: Plan, view: PolicyView): PolicyDecision {
        if (spec.mutating) {
            val touched = plan.writes + plan.deletes
            val protectedHit = touched.firstOrNull { p -> config.protectedPrefixes.any { p == it || p.startsWith("$it/") } }
            if (protectedHit != null) return PolicyDecision.Deny("'$protectedHit' is a protected system location", unsafe = true)
            if (view.filesTouched + plan.fileCount > config.maxFilesPerTask) {
                return PolicyDecision.Deny("File limit for this task would be exceeded (${config.maxFilesPerTask})")
            }
            if (view.bytesTouched + plan.bytes > config.maxBytesPerTask) {
                return PolicyDecision.Deny("Data limit for this task would be exceeded")
            }
        }
        if (spec.risk == RiskLevel.CRITICAL) return PolicyDecision.Deny("Critical operations are not permitted", unsafe = true)
        if (spec.risk == RiskLevel.READ) return PolicyDecision.Allow("read-only")
        if (config.mode == InteractionMode.MANUAL) {
            return PolicyDecision.Deny("Manual mode: recorded as a proposal, not executed", proposal = true)
        }
        // Never grantable for the whole task: always ask.
        if (spec.risk.level >= RiskLevel.DESTRUCTIVE.level) return PolicyDecision.Confirm(request(spec, plan, grantScope = null))

        val scope = spec.name
        return when (config.mode) {
            InteractionMode.AUTONOMOUS ->
                if (scope in config.autonomousAllowedTools || scope in view.grants) PolicyDecision.Allow("pre-authorized")
                else PolicyDecision.Confirm(request(spec, plan, scope))
            else ->
                if (spec.risk.level <= config.autoAllowMaxRisk.level) PolicyDecision.Allow("low risk")
                else if (scope in view.grants) PolicyDecision.Allow("allowed for this task")
                else PolicyDecision.Confirm(request(spec, plan, scope))
        }
    }

    private fun request(spec: ToolSpec, plan: Plan, grantScope: String?) = ConfirmationRequest(
        tool = spec.name,
        risk = spec.risk,
        summary = plan.summary,
        affectedSample = (plan.writes + plan.deletes + plan.reads).distinct().take(10),
        fileCount = plan.fileCount,
        bytes = plan.bytes,
        reversible = plan.reversible,
        grantScope = grantScope,
    )
}

/** The UI side: confirmations and questions. Implemented by the app; faked in tests. */
interface UserChannel {
    suspend fun confirm(request: ConfirmationRequest): ConfirmationResponse

    /** Returns the user's answer, or null if nobody can answer right now. */
    suspend fun ask(question: String): String?
}
