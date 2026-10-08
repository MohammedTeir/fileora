package dev.deviceagent.core.ai.openai

import dev.deviceagent.core.ai.AIProvider
import dev.deviceagent.core.ai.GenerateRequest
import dev.deviceagent.core.ai.GenerateResponse
import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.ai.ModelCapabilities
import dev.deviceagent.core.ai.ModelInfo
import dev.deviceagent.core.ai.ProviderErrorKind
import dev.deviceagent.core.ai.ProviderException
import dev.deviceagent.core.ai.ToolDeclaration
import dev.deviceagent.core.ai.Usage
import dev.deviceagent.core.ai.gemini.HttpRequest
import dev.deviceagent.core.ai.gemini.HttpTransport
import dev.deviceagent.core.ai.gemini.UrlConnectionTransport
import dev.deviceagent.core.tools.ParamType
import dev.deviceagent.core.tools.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException

/** OpenAI Chat Completions-compatible adapter, also usable with compatible gateways. */
class OpenAIProvider(
    private val apiKey: () -> String,
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val baseUrl: String = "https://api.openai.com/v1",
    private val timeoutMs: Int = 60_000,
) : AIProvider {
    override val providerId = "openai"
    private val json = Json { ignoreUnknownKeys = true }
    private var callCounter = 0

    override suspend fun listModels(): List<ModelInfo> {
        val root = parse(send("GET", "$baseUrl/models", null).body)
        return root["data"]?.jsonArray.orEmpty().mapNotNull { item ->
            val id = item.jsonObject["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            ModelInfo(id, id, ModelCapabilities(toolCalling = true, verified = false))
        }
    }

    override suspend fun validateCredentials(): Boolean = try {
        listModels(); true
    } catch (e: ProviderException) {
        if (e.kind == ProviderErrorKind.INVALID_CREDENTIALS) false else throw e
    }

    override fun capabilities(model: String) = ModelCapabilities(toolCalling = true, verified = false)

    override suspend fun generate(request: GenerateRequest): GenerateResponse {
        if (!MODEL_ID.matches(request.model)) throw ProviderException(ProviderErrorKind.BAD_REQUEST, "Invalid model name")
        return parseResponse(send("POST", "$baseUrl/chat/completions", buildRequestBody(request).toString()).body, request.tools)
    }

    internal fun buildRequestBody(req: GenerateRequest): JsonObject = buildJsonObject {
        put("model", req.model)
        putJsonArray("messages") {
            if (req.system.isNotBlank()) add(buildJsonObject { put("role", "system"); put("content", req.system) })
            req.messages.forEach { m ->
                if (m is Message.ToolResults) m.results.forEach { r ->
                    add(buildJsonObject { put("role", "tool"); put("tool_call_id", r.callId); put("content", r.content.toString()) })
                } else add(message(m))
            }
        }
        req.temperature?.let { put("temperature", it) }
        req.maxOutputTokens?.let { put("max_tokens", it) }
        if (req.tools.isNotEmpty()) putJsonArray("tools") { req.tools.forEach { add(tool(it)) } }
    }

    private fun message(m: Message): JsonObject = when (m) {
        is Message.User -> buildJsonObject { put("role", "user"); put("content", m.text) }
        is Message.Assistant -> buildJsonObject {
            put("role", "assistant"); m.text?.let { put("content", it) }
            if (m.toolCalls.isNotEmpty()) putJsonArray("tool_calls") { m.toolCalls.forEach { c ->
                val raw = c.providerData as? JsonObject
                add(raw ?: buildJsonObject { put("id", c.id); put("type", "function"); putJsonObject("function") { put("name", c.name); put("arguments", c.args.toString()) } })
            } }
        }
        is Message.ToolResults -> error("Tool results are flattened by buildRequestBody")
    }

    private fun tool(d: ToolDeclaration) = buildJsonObject {
        put("type", "function")
        putJsonObject("function") {
            put("name", d.name); put("description", d.description)
            putJsonObject("parameters") { put("type", "object"); putJsonObject("properties") { d.params.forEach { p -> putJsonObject(p.name) { put("type", when (p.type) { ParamType.STRING -> "string"; ParamType.INTEGER -> "integer"; ParamType.BOOLEAN -> "boolean" }); put("description", p.description) } } } }
        }
    }

    internal fun parseResponse(body: String, tools: List<ToolDeclaration>): GenerateResponse {
        val root = parse(body)
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: throw ProviderException(ProviderErrorKind.UNKNOWN, "The provider returned no choices")
        val msg = choice["message"]?.jsonObject ?: throw ProviderException(ProviderErrorKind.UNKNOWN, "The provider returned no message")
        val calls = msg["tool_calls"]?.jsonArray.orEmpty().mapNotNull { element ->
            val o = element.jsonObject; val fn = o["function"]?.jsonObject ?: return@mapNotNull null
            ToolCall(o["id"]?.jsonPrimitive?.contentOrNull ?: "call_${++callCounter}", fn["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null, runCatching { json.parseToJsonElement(fn["arguments"]?.jsonPrimitive?.contentOrNull ?: "{}").jsonObject }.getOrDefault(JsonObject(emptyMap())), o)
        }
        val usage = root["usage"]?.jsonObject?.let { Usage(it["prompt_tokens"]?.jsonPrimitive?.intOrNull ?: 0, it["completion_tokens"]?.jsonPrimitive?.intOrNull ?: 0) }
        return GenerateResponse(msg["content"]?.jsonPrimitive?.contentOrNull, calls, usage, choice["finish_reason"]?.jsonPrimitive?.contentOrNull)
    }

    private suspend fun send(method: String, url: String, body: String?): dev.deviceagent.core.ai.gemini.HttpResponse {
        val response = try { transport.send(HttpRequest(method, url, mapOf("Authorization" to "Bearer ${apiKey()}", "Content-Type" to "application/json"), body, timeoutMs)) }
        catch (_: IOException) { throw ProviderException(ProviderErrorKind.NETWORK, "Network error") }
        if (response.status !in 200..299) {
            val message = runCatching { parse(response.body)["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull }.getOrNull() ?: "HTTP ${response.status}"
            throw when {
                response.status == 401 || response.status == 403 -> ProviderException(ProviderErrorKind.INVALID_CREDENTIALS, message)
                response.status == 429 -> ProviderException(ProviderErrorKind.RATE_LIMITED, message, response.headers["retry-after"]?.toLongOrNull()?.times(1000))
                response.status == 404 -> ProviderException(ProviderErrorKind.MODEL_UNAVAILABLE, message)
                response.status == 400 && message.contains("context", true) -> ProviderException(ProviderErrorKind.CONTEXT_TOO_LARGE, message)
                response.status == 400 -> ProviderException(ProviderErrorKind.BAD_REQUEST, message)
                response.status >= 500 -> ProviderException(ProviderErrorKind.MODEL_UNAVAILABLE, message)
                else -> ProviderException(ProviderErrorKind.UNKNOWN, message)
            }
        }
        return response
    }

    private fun parse(body: String): JsonObject = runCatching { json.parseToJsonElement(body).jsonObject }.getOrElse { throw ProviderException(ProviderErrorKind.UNKNOWN, "Unreadable response from provider") }
    private companion object { val MODEL_ID = Regex("[A-Za-z0-9._:-]+") }
}
