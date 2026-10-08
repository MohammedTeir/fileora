package dev.deviceagent.core.ai

import kotlinx.serialization.json.JsonPrimitive

/** The kind of work represented by an upcoming model request. */
enum class ModelRoutePhase { ROUTINE, RECOVERY }

data class ModelRoute(
    val model: String,
    val phase: ModelRoutePhase,
    val reason: String,
)

/**
 * Conservative, provider-neutral routing policy.
 *
 * Routing never invents model IDs or changes providers. The default model remains in use for
 * planning and ordinary turns. A separately selected recovery model is used only immediately
 * after a failed tool-result turn, and only when that model advertises tool-calling support.
 */
object ModelRouter {
    fun select(
        provider: AIProvider,
        defaultModel: String,
        recoveryModel: String?,
        enabled: Boolean,
        messages: List<Message>,
    ): ModelRoute {
        if (!enabled) return ModelRoute(defaultModel, ModelRoutePhase.ROUTINE, "automatic routing is disabled")
        val latestResults = (messages.lastOrNull() as? Message.ToolResults)?.results
            ?: return ModelRoute(defaultModel, ModelRoutePhase.ROUTINE, "no completed tool turn requires recovery")
        if (latestResults.none { it.content["ok"] == JsonPrimitive(false) }) {
            return ModelRoute(defaultModel, ModelRoutePhase.ROUTINE, "the latest tool turn did not fail")
        }
        val candidate = recoveryModel?.trim()?.takeIf { it.isNotEmpty() }
            ?: return ModelRoute(defaultModel, ModelRoutePhase.RECOVERY, "no recovery model is configured; using the selected model")
        if (candidate == defaultModel) {
            return ModelRoute(defaultModel, ModelRoutePhase.RECOVERY, "the recovery model matches the selected model")
        }
        val supportsToolCalling = runCatching { provider.capabilities(candidate).toolCalling }.getOrDefault(false)
        if (!supportsToolCalling) {
            return ModelRoute(defaultModel, ModelRoutePhase.RECOVERY, "the recovery model is not known to support tool calling")
        }
        return ModelRoute(candidate, ModelRoutePhase.RECOVERY, "the latest tool turn failed")
    }
}
