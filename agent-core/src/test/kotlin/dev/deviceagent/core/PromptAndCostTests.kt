package dev.deviceagent.core

import dev.deviceagent.core.agent.AgentConfig
import dev.deviceagent.core.agent.SystemPrompt
import dev.deviceagent.core.agent.TaskStatus
import dev.deviceagent.core.ai.GenerateResponse
import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.ai.Usage
import dev.deviceagent.core.policy.ConfirmationResponse
import dev.deviceagent.core.policy.InteractionMode
import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ParamType
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.RiskLevel
import dev.deviceagent.core.tools.Tool
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolResult
import dev.deviceagent.core.tools.ToolSpec
import dev.deviceagent.core.tools.system.DefaultTools
import kotlinx.serialization.json.JsonObject

/** A tool that exists only in tests: proves the prompt is generated from whatever is registered. */
private class DemoTool(name: String, mutating: Boolean) : Tool {
    override val spec = ToolSpec(
        name, "Demo capability $name.", listOf(ParamSpec("x", ParamType.STRING, "anything", required = false)),
        if (mutating) RiskLevel.LOW else RiskLevel.READ,
    )
    override suspend fun plan(args: JsonObject, ctx: ToolContext) = Plan("demo")
    override suspend fun execute(args: JsonObject, ctx: ToolContext) = ToolResult.success()
}

private val TOOL_NAME_PATTERN = Regex("""\b(?:file|system|media|agent|demo)\.[a-z_]+\b""")

fun registerPromptAndCostTests() {
    // ───────── PLAN_ONLY ─────────

    test("dry run with only read-only calls is PLAN_ONLY, not SUCCESS") {
        fx { f ->
            f.file("a.txt")
            f.provider.calls("file.list" to args("path" to f.root.toString())).says("Nothing to change.")
            val out = f.run(config = AgentConfig("m", dryRun = true))
            eq(TaskStatus.PLAN_ONLY, out.status, "status")
            eq(0, out.proposals.size, "proposals")
        }
    }

    test("a real run is never PLAN_ONLY") {
        fx { f ->
            f.file("a.txt")
            f.provider.calls("file.list" to args("path" to f.root.toString())).says("ok")
            eq(TaskStatus.SUCCESS, f.run().status, "status")
        }
    }

    test("request budget stops the task with BLOCKED, not a false success or plan") {
        fx { f ->
            f.provider
                .calls("file.list" to args("path" to f.root.toString()))
                .calls("file.list" to args("path" to f.root.toString()))
            val out = f.run(config = AgentConfig("m", dryRun = true, maxSteps = 2))
            check(out.status != TaskStatus.PLAN_ONLY) { "an interrupted dry run is not a complete plan, got ${out.status}" }
            eq(TaskStatus.BLOCKED, out.status, "status")
            check(out.stoppedReason!!.contains("AI requests")) { "request budget reason missing" }
        }
    }

    test("input token budget stops before another provider request and reports usage") {
        fx { f ->
            f.provider.then {
                GenerateResponse(
                    text = null,
                    toolCalls = listOf(dev.deviceagent.core.tools.ToolCall("c1", "file.list", args("path" to f.root.toString()))),
                    usage = Usage(inputTokens = 10, outputTokens = 3),
                )
            }
            val out = f.run(config = AgentConfig("m", maxInputTokens = 10, maxOutputTokens = 100))
            eq(TaskStatus.BLOCKED, out.status, "status")
            eq(10L, out.inputTokens, "input tokens")
            eq(3L, out.outputTokens, "output tokens")
            eq(1, f.provider.requests.size, "provider requests")
            check(out.report.contains("input-token limit")) { "token budget reason missing from report" }
        }
    }

    test("checkpoint: engine persists each tool-result turn and resumes from supplied messages") {
        fx { f ->
            f.provider.calls("file.list" to args("path" to f.root.toString())).says("done")
            val snapshots = mutableListOf<List<Message>>()
            val first = f.run(config = AgentConfig("m", onCheckpoint = { snapshots += it }))
            eq(TaskStatus.SUCCESS, first.status, "first status")
            check(snapshots.size >= 2) { "expected initial and post-tool checkpoints" }
            check(snapshots.last().last() is Message.ToolResults) { "last checkpoint must include tool results" }

            f.provider.says("resumed")
            val restored = listOf(Message.User("original goal"), snapshots.last().last())
            f.run(config = AgentConfig("m", initialMessages = restored))
            eq(restored, f.provider.requests.last().messages, "restored conversation")
        }
    }

    // ───────── exact tool list in the system prompt ─────────

    test("prompt lists every registered tool and ask_user, and no other tool name") {
        val specs = DefaultTools.all().map { it.spec }
        val prompt = SystemPrompt.build(listOf("/phone"), false, InteractionMode.NORMAL, specs)
        val mentioned = TOOL_NAME_PATTERN.findAll(prompt).map { it.value }.toSet()
        val expected = specs.map { it.name }.toSet() + "agent.ask_user"
        eq(expected, mentioned, "tool names in the prompt")
    }

    test("prompt follows the registry: a custom registry changes the capability list") {
        val specs = listOf(DemoTool("demo.look", mutating = false).spec, DemoTool("demo.poke", mutating = true).spec)
        val prompt = SystemPrompt.build(listOf("/phone"), false, InteractionMode.NORMAL, specs)
        val mentioned = TOOL_NAME_PATTERN.findAll(prompt).map { it.value }.toSet()
        eq(setOf("demo.look", "demo.poke", "agent.ask_user"), mentioned, "tool names in the prompt")
        check(!prompt.contains("file.copy") && !prompt.contains("file.list")) { "prompt must not mention tools that are not registered" }
        check(prompt.contains("demo.look [read-only]")) { "read-only tool must be labelled" }
        check(prompt.contains("demo.poke [changes the device]")) { "mutating tool must be labelled" }
    }

    test("the prompt the engine really sends contains every registered tool") {
        fx { f ->
            f.provider.says("hi")
            f.run()
            val system = f.provider.requests[0].system
            for (t in DefaultTools.all()) check(system.contains(t.spec.name)) { "missing ${t.spec.name} in system prompt" }
            check(system.contains("COMPLETE list")) { "prompt must say the list is complete" }
        }
    }

    // ───────── request count ─────────

    test("prompt tells the model to batch independent calls into one reply") {
        val prompt = SystemPrompt.build(listOf("/phone"), false, InteractionMode.NORMAL, DefaultTools.all().map { it.spec })
        check(prompt.contains("ONE reply")) { "batching guidance missing" }
        check(prompt.contains("create them separately")) { "guidance about redundant mkdir missing" }
        check(prompt.contains("file.move_matching")) { "bulk move guidance missing" }
        check(prompt.contains("never use more than 10 individual file.move calls")) { "bulk move limit missing" }
    }

    test("engine: create folder + move file in a batched reply takes 3 AI requests (inspect, act, summarize)") {
        fx { f ->
            f.user.onConfirm = { ConfirmationResponse.ALLOW_ONCE }
            f.file("Download/lesson1.mp4", "video")
            f.provider
                .calls("file.list" to args("path" to f.p("Download")))
                .calls(
                    "file.mkdir" to args("path" to f.p("Music")),
                    "file.move" to args("source" to f.p("Download/lesson1.mp4"), "destination" to f.p("Music/lesson1.mp4")),
                )
                .says("Created Music and moved the file.")
            val out = f.run("Create a folder Music and move lesson1.mp4 into it")
            eq(3, out.steps, "steps")
            eq(3, f.provider.requests.size, "requests sent to the AI")
            eq(TaskStatus.SUCCESS, out.status, "status")
            check(f.exists("Music/lesson1.mp4") && !f.exists("Download/lesson1.mp4")) { "file must be moved" }
        }
    }

    test("engine: the same job as a dry run takes 3 requests and ends PLAN_ONLY with 2 proposals") {
        fx { f ->
            f.file("Download/lesson1.mp4", "video")
            f.provider
                .calls("file.list" to args("path" to f.p("Download")))
                .calls(
                    "file.mkdir" to args("path" to f.p("Music")),
                    "file.move" to args("source" to f.p("Download/lesson1.mp4"), "destination" to f.p("Music/lesson1.mp4")),
                )
                .says("Plan: create Music, move the file.")
            val out = f.run("Create a folder Music and move lesson1.mp4 into it", AgentConfig("m", dryRun = true))
            eq(3, f.provider.requests.size, "requests sent to the AI")
            eq(TaskStatus.PLAN_ONLY, out.status, "status")
            eq(2, out.proposals.size, "proposals")
            check(f.exists("Download/lesson1.mp4") && !f.exists("Music")) { "dry run must not change anything" }
        }
    }
}
