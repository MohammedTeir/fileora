package dev.deviceagent.app

import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.policy.ConfirmationResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/** True while the app's screen is visible; pending approval notifications are silenced while this is true. */
object AppVisibility {
    @Volatile var inForeground: Boolean = false
}

/**
 * The agent runs inside [AgentTaskService], the screen lives in the activity. This is the one place
 * where the two meet: the service posts an approval request or a question, the screen (or a
 * notification button) answers it. Before this existed the service waited on its own private
 * channel, so the in-app approval and question dialogs could never appear.
 */
object TaskBridge {
    private val _confirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val confirmation: StateFlow<ConfirmationRequest?> = _confirmation.asStateFlow()

    private val _question = MutableStateFlow<String?>(null)
    val question: StateFlow<String?> = _question.asStateFlow()

    @Volatile private var confirmWaiter: CompletableDeferred<ConfirmationResponse>? = null
    @Volatile private var questionWaiter: CompletableDeferred<String?>? = null

    /** Suspends until the user answers (in the app or from the notification). */
    suspend fun requestConfirmation(request: ConfirmationRequest): ConfirmationResponse {
        val waiter = CompletableDeferred<ConfirmationResponse>()
        confirmWaiter = waiter
        _confirmation.value = request
        try {
            return waiter.await()
        } finally {
            if (confirmWaiter === waiter) confirmWaiter = null
            _confirmation.value = null
        }
    }

    fun answerConfirmation(response: ConfirmationResponse) {
        confirmWaiter?.complete(response)
        _confirmation.value = null
    }

    /** Returns the typed answer, or null if the user skipped or nobody answered in [timeoutMs]. */
    suspend fun askQuestion(question: String, timeoutMs: Long): String? {
        val waiter = CompletableDeferred<String?>()
        questionWaiter = waiter
        _question.value = question
        try {
            return withTimeoutOrNull(timeoutMs) { waiter.await() }
        } finally {
            if (questionWaiter === waiter) questionWaiter = null
            _question.value = null
        }
    }

    fun answerQuestion(answer: String?) {
        questionWaiter?.complete(answer)
        _question.value = null
    }
}
