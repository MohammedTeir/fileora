package dev.deviceagent.core.ai.claude

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

/** Anthropic Claude Messages API adapter. */
class ClaudeProvider(
    private val apiKey: () -> String,
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val baseUrl: String = "https://api.anthropic.com/v1",
    private val timeoutMs: Int = 60_000,
) : AIProvider {
    override val providerId = "claude"
    private val json = Json { ignoreUnknownKeys = true }
    private var callCounter = 0

    override suspend fun listModels(): List<ModelInfo> {
        val root = parse(send("GET", "$baseUrl/models", null).body)
        return root["data"]?.jsonArray.orEmpty().mapNotNull { item ->
            val o = item.jsonObject; val id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            ModelInfo(id, o["display_name"]?.jsonPrimitive?.contentOrNull ?: id, ModelCapabilities(true, verified = false))
        }
    }

    override suspend fun validateCredentials(): Boolean = try { listModels(); true } catch (e: ProviderException) { if (e.kind == ProviderErrorKind.INVALID_CREDENTIALS) false else throw e }
    override fun capabilities(model: String) = ModelCapabilities(true, verified = false)

    override suspend fun generate(request: GenerateRequest): GenerateResponse {
        if (!MODEL_ID.matches(request.model)) throw ProviderException(ProviderErrorKind.BAD_REQUEST, "Invalid Claude model name")
        return parseResponse(send("POST", "$baseUrl/messages", buildRequestBody(request).toString()).body)
    }

    internal fun buildRequestBody(req: GenerateRequest): JsonObject = buildJsonObject {
        put("model", req.model); put("max_tokens", req.maxOutputTokens ?: 4096)
        req.temperature?.let { put("temperature", it) }
        if (req.system.isNotBlank()) put("system", req.system)
        putJsonArray("messages") {
            req.messages.forEach { m -> when (m) {
                is Message.User -> add(buildJsonObject { put("role", "user"); put("content", m.text) })
                is Message.Assistant -> add(buildJsonObject { put("role", "assistant"); put("content", m.text ?: ""); if (m.toolCalls.isNotEmpty()) putJsonArray("content") { m.toolCalls.forEach { c -> add(c.providerData ?: buildJsonObject { put("type", "tool_use"); put("id", c.id); put("name", c.name); put("input", c.args) }) } } })
                is Message.ToolResults -> add(buildJsonObject { put("role", "user"); putJsonArray("content") { m.results.forEach { r -> add(buildJsonObject { put("type", "tool_result"); put("tool_use_id", r.callId); put("content", r.content.toString()) }) } } })
            } }
        }
        if (req.tools.isNotEmpty()) putJsonArray("tools") { req.tools.forEach { d -> add(tool(d)) } }
    }

    private fun tool(d: ToolDeclaration) = buildJsonObject {
        put("name", d.name); put("description", d.description); putJsonObject("input_schema") { put("type", "object"); putJsonObject("properties") { d.params.forEach { p -> putJsonObject(p.name) { put("type", when (p.type) { ParamType.STRING -> "string"; ParamType.INTEGER -> "integer"; ParamType.BOOLEAN -> "boolean" }); put("description", p.description) } } } }
    }

    internal fun parseResponse(body: String): GenerateResponse {
        val root = parse(body); val calls = ArrayList<ToolCall>(); val text = StringBuilder()
        root["content"]?.jsonArray.orEmpty().forEach { block ->
            val o = block.jsonObject
            o["text"]?.jsonPrimitive?.contentOrNull?.let(text::append)
            if (o["type"]?.jsonPrimitive?.contentOrNull == "tool_use") calls += ToolCall(o["id"]?.jsonPrimitive?.contentOrNull ?: "call_${++callCounter}", o["name"]?.jsonPrimitive?.contentOrNull ?: "", o["input"]?.jsonObject ?: JsonObject(emptyMap()), o)
        }
        val usage = root["usage"]?.jsonObject?.let { Usage(it["input_tokens"]?.jsonPrimitive?.intOrNull ?: 0, it["output_tokens"]?.jsonPrimitive?.intOrNull ?: 0) }
        return GenerateResponse(text.toString().ifEmpty { null }, calls, usage, root["stop_reason"]?.jsonPrimitive?.contentOrNull)
    }

    private suspend fun send(method: String, url: String, body: String?): dev.deviceagent.core.ai.gemini.HttpResponse {
        val r = try { transport.send(HttpRequest(method, url, mapOf("x-api-key" to apiKey(), "anthropic-version" to "2023-06-01", "Content-Type" to "application/json"), body, timeoutMs)) } catch (_: IOException) { throw ProviderException(ProviderErrorKind.NETWORK, "Network error") }
        if (r.status !in 200..299) { val msg = runCatching { parse(r.body)["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull }.getOrNull() ?: "HTTP ${r.status}"; throw when { r.status == 401 || r.status == 403 -> ProviderException(ProviderErrorKind.INVALID_CREDENTIALS, msg); r.status == 429 -> ProviderException(ProviderErrorKind.RATE_LIMITED, msg, r.headers["retry-after"]?.toLongOrNull()?.times(1000)); r.status == 404 -> ProviderException(ProviderErrorKind.MODEL_UNAVAILABLE, msg); r.status == 400 && msg.contains("context", true) -> ProviderException(ProviderErrorKind.CONTEXT_TOO_LARGE, msg); r.status == 400 -> ProviderException(ProviderErrorKind.BAD_REQUEST, msg); r.status >= 500 -> ProviderException(ProviderErrorKind.MODEL_UNAVAILABLE, msg); else -> ProviderException(ProviderErrorKind.UNKNOWN, msg) } }
        return r
    }
    private fun parse(body: String): JsonObject = runCatching { json.parseToJsonElement(body).jsonObject }.getOrElse { throw ProviderException(ProviderErrorKind.UNKNOWN, "Unreadable response from Claude") }
    private companion object { val MODEL_ID = Regex("[A-Za-z0-9._:-]+") }
}
