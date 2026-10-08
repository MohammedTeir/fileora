package dev.deviceagent.core

import dev.deviceagent.core.ai.ContextReducer
import dev.deviceagent.core.ai.GenerateRequest
import dev.deviceagent.core.ai.GenerateResponse
import dev.deviceagent.core.ai.AIProvider
import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.ai.ModelCapabilities
import dev.deviceagent.core.ai.ModelInfo
import dev.deviceagent.core.ai.ProviderErrorKind
import dev.deviceagent.core.ai.ProviderException
import dev.deviceagent.core.ai.ResilientProvider
import dev.deviceagent.core.ai.RetryPolicy
import dev.deviceagent.core.ai.ToolDeclaration
import dev.deviceagent.core.ai.ToolResponse
import dev.deviceagent.core.ai.UsageTracker
import dev.deviceagent.core.ai.gemini.GeminiProvider
import dev.deviceagent.core.ai.gemini.HttpRequest
import dev.deviceagent.core.ai.gemini.HttpResponse
import dev.deviceagent.core.ai.gemini.HttpTransport
import dev.deviceagent.core.ai.claude.ClaudeProvider
import dev.deviceagent.core.ai.openai.OpenAIProvider
import dev.deviceagent.core.history.InMemoryAuditLog
import dev.deviceagent.core.security.Redactor
import dev.deviceagent.core.security.maskSecret
import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ParamType
import dev.deviceagent.core.tools.ToolCall
import dev.deviceagent.core.util.str
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

private val CAPS = ModelCapabilities(true)

/** Fails with the queued exceptions first, then succeeds. Records which model each call used. */
private class FlakyProvider(val id: String, private val failures: MutableList<ProviderException>) : AIProvider {
    override val providerId = id
    val modelsUsed = ArrayList<String>()
    val sizes = ArrayList<Int>()
    override suspend fun listModels() = emptyList<ModelInfo>()
    override suspend fun validateCredentials() = true
    override fun capabilities(model: String) = CAPS
    override suspend fun generate(request: GenerateRequest): GenerateResponse {
        modelsUsed += request.model; sizes += request.messages.size
        failures.removeFirstOrNull()?.let { throw it }
        return GenerateResponse("ok from $id", emptyList())
    }
}

private fun req(model: String = "m1", n: Int = 1) = GenerateRequest(model, "sys", List(n) { Message.User("msg $it") })

private class FakeTransport(private val respond: (HttpRequest) -> HttpResponse) : HttpTransport {
    val sent = ArrayList<HttpRequest>()
    override suspend fun send(request: HttpRequest): HttpResponse { sent += request; return respond(request) }
}

private const val KEY = "AIzaSyFAKE_KEY_FOR_TESTS_0123456789abcdefg"
private val TOOLS = listOf(ToolDeclaration("file.copy", "Copy", listOf(ParamSpec("source", ParamType.STRING, "from"), ParamSpec("limit", ParamType.INTEGER, "n", required = false))))

private fun gemini(t: HttpTransport) = GeminiProvider({ KEY }, t, "https://example.test")
private fun openai(t: HttpTransport) = OpenAIProvider({ KEY }, t, "https://example.test/v1")
private fun claude(t: HttpTransport) = ClaudeProvider({ KEY }, t, "https://example.test/v1")

private fun okBody(parts: String, extra: String = "") =
    HttpResponse(200, """{"candidates":[{"content":{"role":"model","parts":$parts},"finishReason":"STOP"}]$extra}""")

fun registerAiAndSecurityTests() {
    // ───────── ResilientProvider ─────────

    test("resilient: rate limit => waits at least Retry-After, then succeeds") {
        val inner = FlakyProvider("p", mutableListOf(ProviderException(ProviderErrorKind.RATE_LIMITED, "slow down", 5_000), ProviderException(ProviderErrorKind.RATE_LIMITED, "slow down", 5_000)))
        val sleeps = ArrayList<Long>(); val tracker = UsageTracker()
        val r = ResilientProvider(inner, sleep = { sleeps += it }, tracker = tracker).generate(req())
        eq("ok from p", r.text, "result"); eq(listOf(5_000L, 5_000L), sleeps, "sleeps honour Retry-After")
        eq(3, tracker.requestCount, "usage records every attempt"); eq(2, tracker.errorCount, "errors recorded")
    }

    test("resilient: backoff grows exponentially without Retry-After, and gives up after maxRetries") {
        val errs = MutableList(10) { ProviderException(ProviderErrorKind.NETWORK, "offline") }
        val inner = FlakyProvider("p", errs); val sleeps = ArrayList<Long>()
        try { ResilientProvider(inner, policy = RetryPolicy(maxRetries = 3, baseDelayMs = 1_000), sleep = { sleeps += it }).generate(req()); throw TestFailure("should have thrown") }
        catch (e: ProviderException) { eq(ProviderErrorKind.NETWORK, e.kind, "kind") }
        eq(listOf(1_000L, 2_000L, 4_000L), sleeps, "backoff"); eq(4, inner.modelsUsed.size, "1 try + 3 retries")
    }

    test("resilient: invalid credentials / bad request / safety block are NOT retried") {
        for (kind in listOf(ProviderErrorKind.INVALID_CREDENTIALS, ProviderErrorKind.BAD_REQUEST, ProviderErrorKind.SAFETY_BLOCKED)) {
            val inner = FlakyProvider("p", mutableListOf(ProviderException(kind, "no")))
            val sleeps = ArrayList<Long>()
            try { ResilientProvider(inner, sleep = { sleeps += it }).generate(req()); throw TestFailure("should have thrown for $kind") } catch (e: ProviderException) { eq(kind, e.kind) }
            eq(1, inner.modelsUsed.size, "$kind attempts"); check(sleeps.isEmpty()) { "$kind must not wait" }
        }
    }

    test("resilient: model unavailable => retries, then the configured fallback model, only if permitted") {
        val primary = FlakyProvider("primary", MutableList(10) { ProviderException(ProviderErrorKind.MODEL_UNAVAILABLE, "503") })
        val fb = FlakyProvider("fallback", mutableListOf())
        val r = ResilientProvider(primary, fb, "fallback-model", RetryPolicy(maxRetries = 1), sleep = {}).generate(req("primary-model"))
        eq("ok from fallback", r.text, "result"); eq(listOf("fallback-model"), fb.modelsUsed, "fallback model used"); eq(2, primary.modelsUsed.size, "primary tried 1+1")
        val primary2 = FlakyProvider("primary", MutableList(10) { ProviderException(ProviderErrorKind.MODEL_UNAVAILABLE, "503") })
        val fb2 = FlakyProvider("fallback", mutableListOf())
        try { ResilientProvider(primary2, fb2, "x", RetryPolicy(maxRetries = 1, allowFallback = false), sleep = {}).generate(req()); throw TestFailure("should throw") } catch (e: ProviderException) { }
        eq(0, fb2.modelsUsed.size, "fallback must respect the user's choice to disable it")
    }

    test("resilient: context too large => history is shrunk once and retried; still too large => error") {
        val inner = FlakyProvider("p", mutableListOf(ProviderException(ProviderErrorKind.CONTEXT_TOO_LARGE, "too big")))
        ResilientProvider(inner, sleep = {}).generate(req(n = 40))
        eq(40, inner.sizes[0], "first try full"); check(inner.sizes[1] < 40) { "second try must be smaller: ${inner.sizes}" }
        val always = FlakyProvider("p", MutableList(5) { ProviderException(ProviderErrorKind.CONTEXT_TOO_LARGE, "too big") })
        try { ResilientProvider(always, sleep = {}).generate(req(n = 40)); throw TestFailure("should throw") } catch (e: ProviderException) { eq(ProviderErrorKind.CONTEXT_TOO_LARGE, e.kind) }
        eq(2, always.sizes.size, "only one reduction attempt")
    }

    test("context reducer never orphans a tool result from its tool call") {
        val call = ToolCall("c", "file.list", args("path" to "/x"))
        val msgs = mutableListOf<Message>(Message.User("goal"))
        repeat(10) { i -> msgs += Message.Assistant(null, listOf(call)); msgs += Message.ToolResults(listOf(ToolResponse("c", "file.list", args("i" to i)))) }
        val reduced = ContextReducer.reduce(GenerateRequest("m", "s", msgs)).messages
        check(reduced.size < msgs.size); check(reduced.first() is Message.User)
        check(reduced[1] is Message.User && (reduced[1] as Message.User).text.contains("omitted")) { "must tell the model something was dropped" }
        check(reduced[2] !is Message.ToolResults) { "first kept message must not be a dangling tool result" }
    }

    // ───────── Gemini adapter (against a fake transport; NOT the live service) ─────────

    test("gemini: key goes in a header, never in the URL; request has tools, system prompt and encoded names") {
        val t = FakeTransport { okBody("""[{"text":"hi"}]""") }
        gemini(t).generate(GenerateRequest("gemini-test", "SYSTEM", listOf(Message.User("hello")), TOOLS))
        val sent = t.sent.single()
        eq("https://example.test/v1beta/models/gemini-test:generateContent", sent.url, "url")
        check(!sent.url.contains("AIza")) { "key must not be in the URL" }; eq(KEY, sent.headers["x-goog-api-key"], "key header")
        val body = Json.parseToJsonElement(sent.body!!).jsonObject
        eq("SYSTEM", body.sub("systemInstruction")["parts"]!!.jsonArray[0].jsonObject.str("text"), "system prompt")
        val decl = body["tools"]!!.jsonArray[0].jsonObject["functionDeclarations"]!!.jsonArray[0].jsonObject
        eq("file__copy", decl.str("name"), "function name encoded (no dot)")
        eq("source", decl.sub("parameters")["required"]!!.jsonArray.single().jsonPrimitive.content, "required list")
        eq("INTEGER", decl.sub("parameters").sub("properties").sub("limit").str("type"), "param type")
    }

    test("gemini: tool calls come back with real names and the raw part is echoed on the next turn") {
        val part = """{"functionCall":{"name":"file__copy","args":{"source":"/a"}},"thoughtSignature":"SIG123"}"""
        val t = FakeTransport { okBody("[$part]", ""","usageMetadata":{"promptTokenCount":11,"candidatesTokenCount":4}""") }
        val g = gemini(t)
        val r = g.generate(GenerateRequest("m", "s", listOf(Message.User("go")), TOOLS))
        val call = r.toolCalls.single()
        eq("file.copy", call.name, "decoded name"); eq("/a", call.args.str("source"), "args"); eq(11, r.usage!!.inputTokens, "usage"); eq(4, r.usage!!.outputTokens, "usage")
        g.generate(GenerateRequest("m", "s", listOf(Message.User("go"), Message.Assistant(null, r.toolCalls), Message.ToolResults(listOf(ToolResponse(call.id, call.name, args("ok" to true))))), TOOLS))
        val second = t.sent[1].body!!
        check(second.contains("SIG123")) { "provider data (thought signature) must be echoed back untouched" }
        check(second.contains("\"functionResponse\"") && second.contains("file__copy")) { "tool result sent as functionResponse with encoded name" }
    }

    test("gemini: HTTP errors map to the right ProviderErrorKind (with Retry-After)") {
        fun kindOf(status: Int, body: String, headers: Map<String, String> = emptyMap()): ProviderException {
            val g = gemini(FakeTransport { HttpResponse(status, body, headers) })
            try { kotlinx.coroutines.runBlocking { g.generate(req()) }; throw TestFailure("expected failure for $status") } catch (e: ProviderException) { return e }
        }
        eq(ProviderErrorKind.INVALID_CREDENTIALS, kindOf(403, """{"error":{"message":"denied"}}""").kind, "403")
        eq(ProviderErrorKind.INVALID_CREDENTIALS, kindOf(400, """{"error":{"message":"API key not valid. Please pass a valid API key."}}""").kind, "400 bad key")
        val rl = kindOf(429, """{"error":{"message":"quota","details":[{"retryDelay":"12s"}]}}""")
        eq(ProviderErrorKind.RATE_LIMITED, rl.kind, "429"); eq(12_000L, rl.retryAfterMs, "retryDelay parsed")
        eq(3_000L, kindOf(429, """{"error":{"message":"q"}}""", mapOf("retry-after" to "3")).retryAfterMs, "Retry-After header")
        eq(ProviderErrorKind.CONTEXT_TOO_LARGE, kindOf(400, """{"error":{"message":"The input token count exceeds the maximum number of tokens allowed"}}""").kind, "context")
        eq(ProviderErrorKind.BAD_REQUEST, kindOf(400, """{"error":{"message":"Invalid JSON payload"}}""").kind, "400")
        eq(ProviderErrorKind.MODEL_UNAVAILABLE, kindOf(503, "overloaded").kind, "503")
        eq(ProviderErrorKind.MODEL_UNAVAILABLE, kindOf(404, """{"error":{"message":"model not found"}}""").kind, "404")
    }

    test("gemini: network failure => NETWORK (no secrets in the message); blocked prompt => SAFETY_BLOCKED") {
        val down = gemini(FakeTransport { throw IOException("connect failed to host with $KEY") })
        try { down.generate(req()); throw TestFailure("should throw") } catch (e: ProviderException) { eq(ProviderErrorKind.NETWORK, e.kind); check(!e.message!!.contains("AIza")) { "message leaked the key" } }
        val blocked = gemini(FakeTransport { HttpResponse(200, """{"promptFeedback":{"blockReason":"SAFETY"}}""") })
        try { blocked.generate(req()); throw TestFailure("should throw") } catch (e: ProviderException) { eq(ProviderErrorKind.SAFETY_BLOCKED, e.kind) }
    }

    test("gemini: listModels keeps only generateContent models; validateCredentials returns false for a rejected key") {
        val ok = gemini(FakeTransport { HttpResponse(200, """{"models":[{"name":"models/g-a","displayName":"A","supportedGenerationMethods":["generateContent"],"inputTokenLimit":1000},{"name":"models/emb","supportedGenerationMethods":["embedContent"]}]}""") })
        val models = ok.listModels()
        eq(listOf("g-a"), models.map { it.id }, "models"); eq(1000, models[0].capabilities.maxInputTokens, "limit"); check(!models[0].capabilities.verified) { "tool-calling support is assumed, so must be flagged unverified" }
        check(ok.validateCredentials())
        val bad = gemini(FakeTransport { HttpResponse(400, """{"error":{"message":"API key not valid."}}""") })
        check(!bad.validateCredentials()) { "rejected key should give false, not throw" }
    }

    test("gemini: model id is validated so it cannot inject into the URL path") {
        val t = FakeTransport { okBody("""[{"text":"x"}]""") }
        val g = gemini(t)
        try { g.generate(GenerateRequest("evil/../../x?key=1", "s", listOf(Message.User("a")))); throw TestFailure("accepted a malicious model id") }
        catch (e: ProviderException) { eq(ProviderErrorKind.BAD_REQUEST, e.kind, "a bad model name is a provider error, not a crash") }
        check(t.sent.isEmpty()) { "nothing may be sent for an invalid model name" }
    }

    test("openai: request uses bearer auth and maps tool calls and usage") {
        val t = FakeTransport {
            check(it.headers["Authorization"] == "Bearer $KEY")
            check(!it.url.contains(KEY))
            HttpResponse(200, """{"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[{"id":"call_1","type":"function","function":{"name":"file.copy","arguments":"{\"source\":\"/a\"}"}}]},"finish_reason":"tool_calls"}],"usage":{"prompt_tokens":12,"completion_tokens":4}}""")
        }
        val response = openai(t).generate(GenerateRequest("gpt-test", "sys", listOf(Message.User("copy")), TOOLS))
        eq("file.copy", response.toolCalls.single().name, "tool name")
        eq(12, response.usage!!.inputTokens, "input usage")
        check(t.sent.single().url.endsWith("/chat/completions"))
    }

    test("openai: rate limits and invalid credentials map to shared provider errors") {
        val rate = FakeTransport { HttpResponse(429, """{"error":{"message":"slow"}}""", mapOf("retry-after" to "3")) }
        try { openai(rate).generate(req("gpt-test")); throw TestFailure("rate limit should fail") }
        catch (e: ProviderException) { eq(ProviderErrorKind.RATE_LIMITED, e.kind, "rate limit"); eq(3_000L, e.retryAfterMs, "retry delay") }
        val auth = FakeTransport { HttpResponse(401, """{"error":{"message":"bad key"}}""") }
        try { openai(auth).generate(req("gpt-test")); throw TestFailure("auth should fail") }
        catch (e: ProviderException) { eq(ProviderErrorKind.INVALID_CREDENTIALS, e.kind, "auth") }
    }

    test("claude: Messages API maps tool use, usage, and required headers") {
        val t = FakeTransport {
            check(it.headers["x-api-key"] == KEY)
            check(it.headers["anthropic-version"] == "2023-06-01")
            HttpResponse(200, """{"content":[{"type":"text","text":"Inspecting"},{"type":"tool_use","id":"tu_1","name":"file.copy","input":{"source":"/a"}}],"stop_reason":"tool_use","usage":{"input_tokens":8,"output_tokens":3}}""")
        }
        val response = claude(t).generate(GenerateRequest("claude-test", "sys", listOf(Message.User("copy")), TOOLS))
        eq("Inspecting", response.text, "text")
        eq("file.copy", response.toolCalls.single().name, "tool name")
        eq(8, response.usage!!.inputTokens, "input usage")
        check(t.sent.single().body!!.contains("input_schema"))
    }

    // ───────── secrets ─────────

    test("redactor: registered secrets and common key/token shapes are removed, including inside JSON") {
        val r = Redactor(); r.addSecret("hunter2-hunter2")
        val text = "key=hunter2-hunter2 and $KEY and Bearer abcdefghijklmnopqrstuvwx and password: swordfish"
        val out = r.redact(text)
        for (s in listOf("hunter2-hunter2", "AIzaSy", "abcdefghijklmnopqrstuvwx", "swordfish")) check(!out.contains(s)) { "'$s' survived: $out" }
        val json = r.redact(args("a" to "x hunter2-hunter2", "n" to listOf("y $KEY"))) as JsonObject
        check(!json.toString().contains("hunter2") && !json.toString().contains("AIza")) { "nested JSON not redacted: $json" }
    }

    test("audit log redacts before storing; maskSecret never shows more than the last 4 characters") {
        val r = Redactor(); r.addSecret("topsecretvalue99")
        val log = InMemoryAuditLog(r)
        log.record("t", "x", args("note" to "used topsecretvalue99 here"))
        check(!log.entries().single().details.toString().contains("topsecretvalue99"))
        check(maskSecret(KEY).endsWith(KEY.takeLast(4)) && !maskSecret(KEY).contains(KEY.take(12))) { "mask exposes too much: ${maskSecret(KEY)}" }
        eq("••••", maskSecret("short"), "short secrets are fully hidden")
    }
}
