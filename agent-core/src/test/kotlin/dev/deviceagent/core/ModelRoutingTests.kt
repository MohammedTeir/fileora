package dev.deviceagent.core

import dev.deviceagent.core.ai.AIProvider
import dev.deviceagent.core.ai.GenerateRequest
import dev.deviceagent.core.ai.GenerateResponse
import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.ai.ModelCapabilities
import dev.deviceagent.core.ai.ModelInfo
import dev.deviceagent.core.ai.ModelRouter
import dev.deviceagent.core.ai.ModelRoutePhase
import dev.deviceagent.core.ai.ToolResponse

fun registerModelRoutingTests() {
    test("model routing: planning and successful turns retain the selected model") {
        val provider = ScriptedProvider()
        val first = ModelRouter.select(provider, "fast", "strong", true, listOf(Message.User("list files")))
        eq("fast", first.model); eq(ModelRoutePhase.ROUTINE, first.phase)
        val successfulTurn = ModelRouter.select(
            provider, "fast", "strong", true,
            listOf(Message.User("list files"), Message.ToolResults(listOf(ToolResponse("c", "file.list", args("ok" to true))))),
        )
        eq("fast", successfulTurn.model); eq(ModelRoutePhase.ROUTINE, successfulTurn.phase)
    }

    test("model routing: failed latest tool result selects the configured recovery model") {
        val provider = ScriptedProvider()
        val decision = ModelRouter.select(
            provider, "fast", "strong", true,
            listOf(Message.User("copy a file"), Message.ToolResults(listOf(ToolResponse("c", "file.copy", args("ok" to false))))),
        )
        eq("strong", decision.model); eq(ModelRoutePhase.RECOVERY, decision.phase)
    }

    test("model routing: disabled routing and missing recovery choice never change models") {
        val provider = ScriptedProvider()
        val failedTurn = listOf(Message.ToolResults(listOf(ToolResponse("c", "file.copy", args("ok" to false)))))
        eq("fast", ModelRouter.select(provider, "fast", "strong", false, failedTurn).model)
        eq("fast", ModelRouter.select(provider, "fast", null, true, failedTurn).model)
        eq("fast", ModelRouter.select(provider, "fast", "fast", true, failedTurn).model)
    }

    test("model routing: a model without tool-calling capability is rejected") {
        val provider = object : AIProvider {
            override val providerId = "test"
            override suspend fun listModels() = emptyList<ModelInfo>()
            override suspend fun validateCredentials() = true
            override suspend fun generate(request: GenerateRequest) = GenerateResponse("unused", emptyList())
            override fun capabilities(model: String) = ModelCapabilities(toolCalling = model != "unsupported")
        }
        val decision = ModelRouter.select(
            provider, "fast", "unsupported", true,
            listOf(Message.ToolResults(listOf(ToolResponse("c", "file.copy", args("ok" to false))))),
        )
        eq("fast", decision.model); eq(ModelRoutePhase.RECOVERY, decision.phase)
    }

    test("model routing: engine uses recovery model after an actual tool failure") {
        fx { f ->
            f.provider.calls("not.a.real.tool" to args())
            f.provider.says("The failed operation was investigated.")
            f.run(config = dev.deviceagent.core.agent.AgentConfig(
                model = "fast",
                automaticModelRouting = true,
                recoveryModel = "strong",
            ))
            eq(listOf("fast", "strong"), f.provider.requests.map { it.model })
        }
    }

    test("model routing: engine toggle off keeps the selected model after tool failure") {
        fx { f ->
            f.provider.calls("not.a.real.tool" to args())
            f.provider.says("The failure was reported.")
            f.run(config = dev.deviceagent.core.agent.AgentConfig(
                model = "fast",
                automaticModelRouting = false,
                recoveryModel = "strong",
            ))
            eq(listOf("fast", "fast"), f.provider.requests.map { it.model })
        }
    }
}
