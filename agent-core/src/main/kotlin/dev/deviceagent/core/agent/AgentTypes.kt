package dev.deviceagent.core.agent

import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.media.MediaBackend

/** Every task ends in exactly one of these (spec section 12). Computed from facts, not from what the model claims. */
enum class TaskStatus {
    SUCCESS, SUCCESS_AFTER_RECOVERY, PARTIAL_SUCCESS,
    /** Dry run finished: the job was planned, nothing on the device was changed. Never reported as SUCCESS. */
    PLAN_ONLY,
    WAITING_FOR_USER, WAITING_FOR_PERMISSION, WAITING_FOR_RESOURCE,
    BLOCKED, UNSAFE_TO_CONTINUE, FAILED_AFTER_INVESTIGATION, CANCELLED,
}

/** Controls whether file contents may be returned to the cloud model. */
enum class PrivacyMode { NAMES_ONLY, CONTENTS_ALLOWED }

data class AgentConfig(
    val model: String,
    val privacyMode: PrivacyMode = PrivacyMode.NAMES_ONLY,
    /** Hard cap on model requests per task (also bounds AI cost). */
    val maxSteps: Int = 40,
    /** Hard cap on input tokens reported by the provider for this task. */
    val maxInputTokens: Long = 100_000,
    /** Hard cap on output tokens reported by the provider for this task. */
    val maxOutputTokens: Long = 20_000,
    /** Extra tries allowed for the same item after its first failure. */
    val maxRecoveryAttempts: Int = 3,
    /** Stop the whole task after this many failed calls, however they are spread. */
    val maxTotalFailures: Int = 30,
    val maxToolCallsPerTask: Int = 1_000,
    val maxResultChars: Int = 12_000,
    /** Show what would happen; never change anything. */
    val dryRun: Boolean = false,
    val temperature: Double = 0.2,
    /** Route only failed tool-result turns to [recoveryModel]; disabled unless the user opts in. */
    val automaticModelRouting: Boolean = false,
    /** Optional recovery model on the same configured provider. */
    val recoveryModel: String? = null,
    /** Provider-neutral conversation to continue after a process restart. */
    val initialMessages: List<Message> = emptyList(),
    /** Called after each completed model/tool turn so an app can persist an exact checkpoint. */
    val onCheckpoint: ((List<Message>) -> Unit)? = null,
    val mediaBackend: MediaBackend? = null,
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
    val inputTokens: Long,
    val outputTokens: Long,
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
