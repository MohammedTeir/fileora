package dev.deviceagent.core.ai

import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ToolCall
import kotlinx.serialization.json.JsonObject

// ───────────── conversation model (provider-neutral) ─────────────

sealed interface Message {
    data class User(val text: String) : Message
    data class Assistant(val text: String?, val toolCalls: List<ToolCall> = emptyList()) : Message
    data class ToolResults(val results: List<ToolResponse>) : Message
}

data class ToolResponse(val callId: String, val name: String, val content: JsonObject)

data class ToolDeclaration(val name: String, val description: String, val params: List<ParamSpec>)

data class GenerateRequest(
    val model: String,
    val system: String,
    val messages: List<Message>,
    val tools: List<ToolDeclaration> = emptyList(),
    val temperature: Double? = 0.2,
    val maxOutputTokens: Int? = null,
)

data class Usage(val inputTokens: Int, val outputTokens: Int)

data class GenerateResponse(
    val text: String?,
    val toolCalls: List<ToolCall>,
    val usage: Usage? = null,
    val finishReason: String? = null,
)

// ───────────── capabilities ─────────────

/**
 * [verified] is false when the numbers are defaults rather than values the provider reported,
 * so the core never treats a guess as a guarantee.
 */
data class ModelCapabilities(
    val toolCalling: Boolean,
    val structuredOutput: Boolean = false,
    val vision: Boolean = false,
    val maxInputTokens: Int? = null,
    val maxOutputTokens: Int? = null,
    val verified: Boolean = false,
)

data class ModelInfo(val id: String, val displayName: String, val capabilities: ModelCapabilities)

// ───────────── errors ─────────────

enum class ProviderErrorKind {
    INVALID_CREDENTIALS, RATE_LIMITED, MODEL_UNAVAILABLE, CONTEXT_TOO_LARGE, BAD_REQUEST, SAFETY_BLOCKED, NETWORK, UNKNOWN,
}

/** An AI provider failure. Deliberately separate from device-tool failures. */
class ProviderException(
    val kind: ProviderErrorKind,
    message: String,
    val retryAfterMs: Long? = null,
) : Exception(message)

// ───────────── the abstraction the agent core depends on ─────────────

/**
 * The agent core only ever sees this interface. Provider specifics (wire format, auth header,
 * function-name rules, signatures) stay inside the adapter.
 */
interface AIProvider {
    val providerId: String
    suspend fun listModels(): List<ModelInfo>

    /** True if the credentials work, false if they are rejected; throws on network trouble. */
    suspend fun validateCredentials(): Boolean

    /** Text generation; when [GenerateRequest.tools] is non-empty the model may return tool calls. */
    suspend fun generate(request: GenerateRequest): GenerateResponse

    fun capabilities(model: String): ModelCapabilities
}
