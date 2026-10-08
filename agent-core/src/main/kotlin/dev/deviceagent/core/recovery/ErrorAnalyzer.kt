package dev.deviceagent.core.recovery

import dev.deviceagent.core.tools.ErrorKind
import dev.deviceagent.core.tools.ToolError

/** The next-step classes from the spec (section 4) that apply to the file tools today. */
enum class RecoveryAction {
    RETRY_WITH_ADAPTATION, USE_ALTERNATIVE_TOOL, REQUEST_PERMISSION, ASK_USER,
    WAIT_FOR_RESOURCE, BLOCK_UNSAFE_OPERATION, FAIL_WITH_DIAGNOSIS,
}

data class Diagnosis(
    val category: String,
    val explanation: String,
    val suggestions: List<String>,
    val action: RecoveryAction,
)

/**
 * Turns a failure into a structured diagnosis the model can act on. It never decides to apply a
 * fix itself: fixes are normal tool calls and go through the policy engine like anything else.
 */
object ErrorAnalyzer {
    fun analyze(error: ToolError, roots: List<String>): Diagnosis {
        val where = error.path?.let { " ($it)" } ?: ""
        return when (error.kind) {
            ErrorKind.NOT_FOUND -> Diagnosis(
                "wrong_path", "The path does not exist$where.",
                listOf(
                    "List the parent folder with file.list to see what is actually there.",
                    "Search for the real name with file.search (names may differ in case, spaces or extension).",
                    "Retry with the corrected path once you have confirmed it exists.",
                ),
                RecoveryAction.RETRY_WITH_ADAPTATION,
            )
            ErrorKind.ALREADY_EXISTS -> Diagnosis(
                "destination_exists", "Something already exists at the destination$where. Nothing was overwritten.",
                listOf(
                    "Compare both with file.stat and file.hash. If they are identical, the work may already be done: skip it.",
                    "Otherwise choose a different, non-clashing destination name.",
                    "Do not delete the existing item to make room unless the user explicitly asked for that.",
                ),
                RecoveryAction.RETRY_WITH_ADAPTATION,
            )
            ErrorKind.PERMISSION_DENIED -> Diagnosis(
                "no_permission", "The device refused access$where.",
                listOf(
                    "Choose a destination inside an accessible folder: ${roots.joinToString()}.",
                    "If the user needs this exact location, ask them to grant access to that folder. Do not try to bypass the restriction.",
                ),
                RecoveryAction.REQUEST_PERMISSION,
            )
            ErrorKind.OUTSIDE_SANDBOX -> Diagnosis(
                "outside_accessible_folders", "That location is not one the agent may access$where.",
                listOf("Accessible folders are: ${roots.joinToString()}.", "Use a path inside them, or ask the user to add the folder."),
                RecoveryAction.REQUEST_PERMISSION,
            )
            ErrorKind.NO_SPACE -> Diagnosis(
                "storage_full", "Not enough free space$where.",
                listOf(
                    "Check free space per folder with system.storage.",
                    "Pick another destination with more room, or work in smaller batches.",
                    "Ask the user before freeing space by deleting anything.",
                ),
                RecoveryAction.WAIT_FOR_RESOURCE,
            )
            ErrorKind.INVALID_ARGUMENTS -> Diagnosis(
                "bad_arguments", "The arguments were not valid: ${error.message}.",
                listOf("Fix the arguments to match the tool's schema and call it again."),
                RecoveryAction.RETRY_WITH_ADAPTATION,
            )
            ErrorKind.VERIFICATION_FAILED -> Diagnosis(
                "failed_verification",
                "The tool reported success but the result on disk did not check out: ${error.message}. " +
                    "Any change from that call has been rolled back where possible.",
                listOf(
                    "Inspect the current state with file.stat / file.list before trying again.",
                    "Retry once; if it fails the same way, try a different approach or report the problem.",
                ),
                RecoveryAction.RETRY_WITH_ADAPTATION,
            )
            ErrorKind.REPEATED_STRATEGY -> Diagnosis(
                "repeated_strategy", error.message,
                listOf("Change something that matters (path, name, destination) or investigate first with read-only tools."),
                RecoveryAction.RETRY_WITH_ADAPTATION,
            )
            ErrorKind.BUDGET_EXHAUSTED -> Diagnosis(
                "recovery_limit_reached", error.message,
                listOf("Stop retrying this item. Explain to the user what was tried and what is needed."),
                RecoveryAction.FAIL_WITH_DIAGNOSIS,
            )
            ErrorKind.UNKNOWN_TOOL -> Diagnosis(
                "unknown_tool", error.message, listOf("Use only the tools you were given."), RecoveryAction.RETRY_WITH_ADAPTATION,
            )
            ErrorKind.IO, ErrorKind.UNKNOWN -> Diagnosis(
                "io_error", "The operation failed: ${error.message}$where.",
                listOf("Inspect the current state with read-only tools.", "If the cause is unclear, say so honestly instead of guessing."),
                RecoveryAction.FAIL_WITH_DIAGNOSIS,
            )
        }
    }
}
