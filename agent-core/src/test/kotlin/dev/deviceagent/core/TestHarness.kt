package dev.deviceagent.core

import dev.deviceagent.core.agent.AgentConfig
import dev.deviceagent.core.agent.AgentEngine
import dev.deviceagent.core.agent.TaskOutcome
import dev.deviceagent.core.ai.AIProvider
import dev.deviceagent.core.ai.GenerateRequest
import dev.deviceagent.core.ai.GenerateResponse
import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.ai.ModelCapabilities
import dev.deviceagent.core.ai.ModelInfo
import dev.deviceagent.core.ai.ProviderException
import dev.deviceagent.core.ai.ToolResponse
import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.fs.JvmFileSystem
import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.policy.ConfirmationResponse
import dev.deviceagent.core.policy.InteractionMode
import dev.deviceagent.core.policy.PolicyConfig
import dev.deviceagent.core.policy.PolicyEngine
import dev.deviceagent.core.policy.UserChannel
import dev.deviceagent.core.security.Redactor
import dev.deviceagent.core.tools.ToolCall
import dev.deviceagent.core.tools.ToolRegistry
import dev.deviceagent.core.tools.system.DefaultTools
import dev.deviceagent.core.util.jsonObjectOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Path

// ───────────── tiny test harness (no JUnit, so it runs identically everywhere) ─────────────

class TestFailure(message: String) : Exception(message)

private val registered = ArrayList<Pair<String, suspend () -> Unit>>()

fun test(name: String, body: suspend () -> Unit) { registered += name to body }

fun check(condition: Boolean, message: () -> String = { "condition was false" }) {
    if (!condition) throw TestFailure(message())
}

fun <T> eq(expected: T, actual: T, label: String = "value") {
    if (expected != actual) throw TestFailure("$label: expected <$expected> but was <$actual>")
}

fun runAllRegistered(): Int {
    var failed = 0
    for ((name, body) in registered) {
        try {
            runBlocking { body() }
            println("PASS  $name")
        } catch (t: Throwable) {
            failed++
            println("FAIL  $name\n      ${t.javaClass.simpleName}: ${t.message}")
            t.stackTrace.firstOrNull { it.className.startsWith("dev.deviceagent") }?.let { println("      at $it") }
        }
    }
    println("\n${registered.size - failed} passed, $failed failed, ${registered.size} total")
    return failed
}

// ───────────── fakes ─────────────

class ScriptedProvider : AIProvider {
    override val providerId = "scripted"
    private val script = ArrayDeque<(GenerateRequest) -> GenerateResponse>()
    val requests = ArrayList<GenerateRequest>()
    private var ids = 0

    fun then(f: (GenerateRequest) -> GenerateResponse) = apply { script.addLast(f) }
    fun says(text: String) = then { GenerateResponse(text, emptyList()) }
    fun calls(vararg c: Pair<String, JsonObject>) = then {
        GenerateResponse(null, c.map { (n, a) -> ToolCall("c${++ids}", n, a) })
    }
    fun fails(e: ProviderException) = then { throw e }

    override suspend fun listModels() = listOf(ModelInfo("m", "m", ModelCapabilities(true)))
    override suspend fun validateCredentials() = true
    override fun capabilities(model: String) = ModelCapabilities(true)
    override suspend fun generate(request: GenerateRequest): GenerateResponse {
        requests += request
        val f = script.removeFirstOrNull() ?: throw TestFailure("model script exhausted after ${requests.size - 1} requests")
        return f(request)
    }

    /** Tool results that the model saw at the START of request [index]. */
    fun resultsAt(index: Int): List<ToolResponse> = (requests[index].messages.last() as Message.ToolResults).results
    fun lastResults(): List<ToolResponse> = resultsAt(requests.size - 1)
}

class FakeUser(
    var onConfirm: (ConfirmationRequest) -> ConfirmationResponse = { ConfirmationResponse.DENY },
    var onAsk: (String) -> String? = { null },
) : UserChannel {
    val confirmations = ArrayList<ConfirmationRequest>()
    val questions = ArrayList<String>()
    override suspend fun confirm(request: ConfirmationRequest): ConfirmationResponse {
        confirmations += request
        return onConfirm(request)
    }
    override suspend fun ask(question: String): String? { questions += question; return onAsk(question) }
}

/** A temp "phone": /phone is the only accessible root; /outside exists to test sandbox escapes. */
class Fx(
    mode: InteractionMode = InteractionMode.NORMAL,
    policyConfig: PolicyConfig? = null,
    policyFor: ((Path) -> PolicyConfig)? = null,
    wrapFs: ((FileSystem) -> FileSystem)? = null,
) {
    val base: Path = Files.createTempDirectory("agent-test").toRealPath()
    val root: Path = Files.createDirectories(base.resolve("phone"))
    val outside: Path = Files.createDirectories(base.resolve("outside"))
    private val jfs = JvmFileSystem(listOf(root.toString()))
    val fs: FileSystem = wrapFs?.invoke(jfs) ?: jfs
    val provider = ScriptedProvider()
    val user = FakeUser()
    val policy = PolicyEngine(policyFor?.invoke(root) ?: policyConfig ?: PolicyConfig(mode = mode))
    val redactor = Redactor()
    val engine: AgentEngine by lazy {
        AgentEngine(provider, ToolRegistry(DefaultTools.all()), fs, policy, user, redactor = redactor, newTaskId = { "t1" })
    }

    fun p(rel: String) = root.resolve(rel).toString()
    fun file(rel: String, content: String = "hello"): String {
        val f = root.resolve(rel)
        Files.createDirectories(f.parent)
        Files.writeString(f, content)
        return f.toString()
    }
    fun dir(rel: String): String = Files.createDirectories(root.resolve(rel)).toString()
    fun exists(rel: String) = Files.exists(root.resolve(rel))
    fun read(rel: String) = Files.readString(root.resolve(rel))
    suspend fun run(goal: String = "do it", config: AgentConfig = AgentConfig("test-model")): TaskOutcome = engine.run(goal, config)
    fun cleanup() { base.toFile().deleteRecursively() }
}

fun fx(
    mode: InteractionMode = InteractionMode.NORMAL,
    policyConfig: PolicyConfig? = null,
    policyFor: ((Path) -> PolicyConfig)? = null,
    wrapFs: ((FileSystem) -> FileSystem)? = null,
    body: suspend (Fx) -> Unit,
) = runBlocking {
    val f = Fx(mode, policyConfig, policyFor, wrapFs)
    try { body(f) } finally { f.cleanup() }
}

fun args(vararg pairs: Pair<String, Any?>): JsonObject = jsonObjectOf(*pairs)

fun JsonObject.sub(key: String): JsonObject = this[key]?.jsonObject ?: throw TestFailure("no object '$key' in $this")
