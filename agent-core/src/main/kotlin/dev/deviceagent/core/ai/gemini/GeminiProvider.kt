package dev.deviceagent.core.ai.gemini

import dev.deviceagent.core.ai.GenerateRequest
import dev.deviceagent.core.ai.GenerateResponse
import dev.deviceagent.core.ai.AIProvider
import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.ai.ModelCapabilities
import dev.deviceagent.core.ai.ModelInfo
import dev.deviceagent.core.ai.ProviderErrorKind
import dev.deviceagent.core.ai.ProviderException
import dev.deviceagent.core.ai.ToolDeclaration
import dev.deviceagent.core.ai.Usage
import dev.deviceagent.core.tools.ParamType
import dev.deviceagent.core.tools.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection

// ───────────── transport (swappable: tests fake it; Android/JVM use HttpURLConnection) ─────────────

data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String?,
    val timeoutMs: Int = 60_000,
)

data class HttpResponse(val status: Int, val body: String, val headers: Map<String, String> = emptyMap())

interface HttpTransport {
    /** Throws [IOException] on connectivity problems; returns normally for any HTTP status. */
    suspend fun send(request: HttpRequest): HttpResponse
}

class UrlConnectionTransport : HttpTransport {
    override suspend fun send(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val conn = java.net.URI(request.url).toURL().openConnection() as HttpURLConnection
        try {
            conn.requestMethod = request.method
            conn.connectTimeout = request.timeoutMs
            conn.readTimeout = request.timeoutMs
            request.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (request.body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val headers = conn.headerFields.filterKeys { it != null }
                .mapKeys { it.key.lowercase() }.mapValues { it.value.firstOrNull() ?: "" }
            HttpResponse(status, text, headers)
        } finally {
            conn.disconnect()
        }
    }
}

// ───────────── adapter ─────────────

/**
 * Google Gemini adapter (Generative Language API, `generateContent` with function calling).
 *
 * The API key is fetched per request from [apiKey] and sent in the `x-goog-api-key` header —
 * never in the URL, so it cannot end up in logs or proxies' URL records.
 *
 * NOTE: written against the public API documentation and unit-tested against recorded-shape
 * fakes; it has not been run against the live service from the build environment.
 */
class GeminiProvider(
    private val apiKey: () -> String,
    private val transport: HttpTransport = UrlConnectionTransport(),
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
    private val timeoutMs: Int = 60_000,
) : AIProvider {
    override val providerId = "gemini"

    private val json = Json { ignoreUnknownKeys = true }
    private var callCounter = 0

    // Gemini function names: encode '.' so every declared name is a plain identifier.
    private fun encodeName(name: String) = name.replace(".", "__")

    override suspend fun listModels(): List<ModelInfo> {
        val resp = send("GET", "$baseUrl/v1beta/models?pageSize=200", null)
        val root = parse(resp.body)
        return root["models"]?.jsonArray.orEmpty().mapNotNull { m ->
            val o = m.jsonObject
            val methods = o["supportedGenerationMethods"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
            if ("generateContent" !in methods) return@mapNotNull null
            val id = o["name"]?.jsonPrimitive?.contentOrNull?.removePrefix("models/") ?: return@mapNotNull null
            ModelInfo(
                id = id,
                displayName = o["displayName"]?.jsonPrimitive?.contentOrNull ?: id,
                capabilities = ModelCapabilities(
                    // Every generateContent model is assumed able to call tools; the list endpoint
                    // does not say, so this is marked unverified.
                    toolCalling = true,
                    maxInputTokens = o["inputTokenLimit"]?.jsonPrimitive?.intOrNull,
                    maxOutputTokens = o["outputTokenLimit"]?.jsonPrimitive?.intOrNull,
                    verified = false,
                ),
            )
        }
    }

    override suspend fun validateCredentials(): Boolean = try {
        listModels(); true
    } catch (e: ProviderException) {
        if (e.kind == ProviderErrorKind.INVALID_CREDENTIALS) false else throw e
    }

    override fun capabilities(model: String) = ModelCapabilities(toolCalling = true, verified = false)

    override suspend fun generate(request: GenerateRequest): GenerateResponse {
        if (!MODEL_ID.matches(request.model)) {
            throw ProviderException(ProviderErrorKind.BAD_REQUEST, "Invalid model name. Use a name from the model list, such as one returned by List models.")
        }
        val body = buildRequestBody(request)
        val resp = send("POST", "$baseUrl/v1beta/models/${request.model}:generateContent", body.toString())
        return parseResponse(resp.body, request.tools)
    }

    // ── request ──

    internal fun buildRequestBody(req: GenerateRequest): JsonObject = buildJsonObject {
        if (req.system.isNotBlank()) {
            putJsonObject("systemInstruction") { putJsonArray("parts") { add(buildJsonObject { put("text", req.system) }) } }
        }
        put("contents", JsonArray(req.messages.map(::content)))
        if (req.tools.isNotEmpty()) {
            putJsonArray("tools") {
                add(buildJsonObject { put("functionDeclarations", JsonArray(req.tools.map(::declaration))) })
            }
        }
        putJsonObject("generationConfig") {
            req.temperature?.let { put("temperature", it) }
            req.maxOutputTokens?.let { put("maxOutputTokens", it) }
        }
    }

    private fun content(m: Message): JsonObject = when (m) {
        is Message.User -> buildJsonObject {
            put("role", "user")
            putJsonArray("parts") { add(buildJsonObject { put("text", m.text) }) }
        }
        is Message.Assistant -> buildJsonObject {
            put("role", "model")
            putJsonArray("parts") {
                m.text?.takeIf { it.isNotEmpty() }?.let { add(buildJsonObject { put("text", it) }) }
                for (c in m.toolCalls) {
                    // Echo the provider's own part back untouched (keeps any thought signature / id).
                    val raw = c.providerData as? JsonObject
                    add(raw ?: buildJsonObject {
                        putJsonObject("functionCall") { put("name", encodeName(c.name)); put("args", c.args) }
                    })
                }
            }
        }
        is Message.ToolResults -> buildJsonObject {
            put("role", "user")
            putJsonArray("parts") {
                for (r in m.results) add(buildJsonObject {
                    putJsonObject("functionResponse") {
                        put("name", encodeName(r.name))
                        putJsonObject("response") { put("result", r.content) }
                    }
                })
            }
        }
    }

    private fun declaration(d: ToolDeclaration): JsonObject = buildJsonObject {
        put("name", encodeName(d.name))
        put("description", d.description)
        if (d.params.isNotEmpty()) {
            putJsonObject("parameters") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    for (p in d.params) putJsonObject(p.name) {
                        put("type", when (p.type) { ParamType.STRING -> "STRING"; ParamType.INTEGER -> "INTEGER"; ParamType.BOOLEAN -> "BOOLEAN" })
                        put("description", p.description)
                    }
                }
                val required = d.params.filter { it.required }.map { it.name }
                if (required.isNotEmpty()) putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
            }
        }
    }

    // ── response ──

    internal fun parseResponse(body: String, tools: List<ToolDeclaration>): GenerateResponse {
        val root = parse(body)
        val reverse = tools.associateBy { encodeName(it.name) }
        root["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.contentOrNull?.let {
            throw ProviderException(ProviderErrorKind.SAFETY_BLOCKED, "The request was blocked by the provider ($it)")
        }
        val candidate = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw ProviderException(ProviderErrorKind.UNKNOWN, "The provider returned no candidates")
        val text = StringBuilder()
        val calls = ArrayList<ToolCall>()
        for (part in candidate["content"]?.jsonObject?.get("parts")?.jsonArray.orEmpty()) {
            val p = part.jsonObject
            if (p["thought"]?.jsonPrimitive?.contentOrNull == "true") continue
            p["text"]?.jsonPrimitive?.contentOrNull?.let { text.append(it) }
            val fc = p["functionCall"]?.jsonObject ?: continue
            val wire = fc["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val name = reverse[wire]?.name ?: wire.replace("__", ".")
            calls += ToolCall(
                id = fc["id"]?.jsonPrimitive?.contentOrNull ?: "call_${++callCounter}",
                name = name,
                args = fc["args"] as? JsonObject ?: JsonObject(emptyMap()),
                providerData = p,
            )
        }
        val usage = root["usageMetadata"]?.jsonObject?.let {
            Usage(it["promptTokenCount"]?.jsonPrimitive?.intOrNull ?: 0, it["candidatesTokenCount"]?.jsonPrimitive?.intOrNull ?: 0)
        }
        val finish = candidate["finishReason"]?.jsonPrimitive?.contentOrNull
        if (text.isEmpty() && calls.isEmpty() && finish != null && finish != "STOP") {
            throw ProviderException(
                if (finish == "SAFETY" || finish == "PROHIBITED_CONTENT") ProviderErrorKind.SAFETY_BLOCKED else ProviderErrorKind.UNKNOWN,
                "The model returned nothing (finish reason: $finish)",
            )
        }
        return GenerateResponse(text.toString().ifEmpty { null }, calls, usage, finish)
    }

    // ── transport + errors ──

    private suspend fun send(method: String, url: String, body: String?): HttpResponse {
        val resp = try {
            transport.send(
                HttpRequest(method, url, mapOf("x-goog-api-key" to apiKey(), "Content-Type" to "application/json"), body, timeoutMs),
            )
        } catch (e: IOException) {
            throw ProviderException(ProviderErrorKind.NETWORK, "Network error: ${e.javaClass.simpleName}")
        }
        if (resp.status !in 200..299) throw mapError(resp)
        return resp
    }

    private fun parse(body: String): JsonObject = try {
        json.parseToJsonElement(body).jsonObject
    } catch (e: Exception) {
        throw ProviderException(ProviderErrorKind.UNKNOWN, "Unreadable response from the provider")
    }

    internal fun mapError(resp: HttpResponse): ProviderException {
        val err = try { json.parseToJsonElement(resp.body).jsonObject["error"]?.jsonObject } catch (e: Exception) { null }
        val msg = err?.get("message")?.jsonPrimitive?.contentOrNull ?: "HTTP ${resp.status}"
        val lower = msg.lowercase()
        val retryAfter = resp.headers["retry-after"]?.toLongOrNull()?.times(1000) ?: retryDelayMs(err)
        return when {
            resp.status == 429 -> ProviderException(ProviderErrorKind.RATE_LIMITED, msg, retryAfter)
            resp.status == 401 || resp.status == 403 -> ProviderException(ProviderErrorKind.INVALID_CREDENTIALS, msg)
            resp.status == 400 && ("api key" in lower) -> ProviderException(ProviderErrorKind.INVALID_CREDENTIALS, msg)
            resp.status == 400 && ("token count" in lower || "too long" in lower || "exceeds the maximum" in lower || "context" in lower && "exceed" in lower) ->
                ProviderException(ProviderErrorKind.CONTEXT_TOO_LARGE, msg)
            resp.status == 400 -> ProviderException(ProviderErrorKind.BAD_REQUEST, msg)
            resp.status == 404 -> ProviderException(ProviderErrorKind.MODEL_UNAVAILABLE, msg)
            resp.status >= 500 -> ProviderException(ProviderErrorKind.MODEL_UNAVAILABLE, msg, retryAfter)
            else -> ProviderException(ProviderErrorKind.UNKNOWN, msg)
        }
    }

    /** Gemini reports "retryDelay": "12s" inside error.details. */
    private fun retryDelayMs(err: JsonObject?): Long? {
        val details = err?.get("details") as? JsonArray ?: return null
        for (d in details) {
            val s = (d as? JsonObject)?.get("retryDelay")?.jsonPrimitive?.contentOrNull ?: continue
            s.removeSuffix("s").toDoubleOrNull()?.let { return (it * 1000).toLong() }
        }
        return null
    }

    private companion object {
        val MODEL_ID = Regex("[A-Za-z0-9._\\-]+")
    }
}
