package dev.deviceagent.core.agent

import dev.deviceagent.core.tools.Plan

/** Every task ends in exactly one of these (spec section 12). Computed from facts, not from what the model claims. */
enum class TaskStatus {
    SUCCESS, SUCCESS_AFTER_RECOVERY, PARTIAL_SUCCESS,
    WAITING_FOR_USER, WAITING_FOR_PERMISSION, WAITING_FOR_RESOURCE,
    BLOCKED, UNSAFE_TO_CONTINUE, FAILED_AFTER_INVESTIGATION, CANCELLED,
}

data class AgentConfig(
    val model: String,
    /** Hard cap on model requests per task (also bounds AI cost). */
    val maxSteps: Int = 40,
    /** Extra tries allowed for the same item after its first failure. */
    val maxRecoveryAttempts: Int = 3,
    /** Stop the whole task after this many failed calls, however they are spread. */
    val maxTotalFailures: Int = 30,
    val maxToolCallsPerTask: Int = 1_000,
    val maxResultChars: Int = 12_000,
    /** Show what would happen; never change anything. */
    val dryRun: Boolean = false,
    val temperature: Double = 0.2,
)

/** One failed try, kept for the audit trail and the final report. */
data class AttemptRecord(
    val step: Int,
    val tool: String,
    val subject: String,
    val errorKind: String,
    val message: String,
    val diagnosis: String,
    val attemptNumber: Int,
)

data class TaskOutcome(
    val taskId: String,
    val status: TaskStatus,
    /** The model's closing message, if it gave one. */
    val modelMessage: String?,
    /** Facts computed by the engine (what actually happened). Show this, not just the model's words. */
    val report: String,
    val steps: Int,
    val toolCalls: Int,
    val failures: Int,
    val attempts: List<AttemptRecord>,
    /** Changes that were planned but deliberately not executed (dry run / manual mode). */
    val proposals: List<Plan>,
    val canUndo: Boolean,
    val unverifiedChanges: Int,
    val stoppedReason: String?,
    val providerError: String?,
)
