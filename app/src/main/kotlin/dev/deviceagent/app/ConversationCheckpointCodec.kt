package dev.deviceagent.app

import dev.deviceagent.core.ai.Message
import dev.deviceagent.core.ai.ToolResponse
import dev.deviceagent.core.tools.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Compact, provider-neutral encoding for the exact conversation needed to resume a task. */
object ConversationCheckpointCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(messages: List<Message>): String = buildJsonArray {
        messages.forEach { message ->
            add(
                when (message) {
                    is Message.User -> buildJsonObject {
                        put("kind", "user")
                        put("text", message.text)
                    }
                    is Message.Assistant -> buildJsonObject {
                        put("kind", "assistant")
                        message.text?.let { put("text", it) } ?: put("text", JsonNull)
                        put("tool_calls", buildJsonArray {
                            message.toolCalls.forEach { call -> add(encodeCall(call)) }
                        })
                    }
                    is Message.ToolResults -> buildJsonObject {
                        put("kind", "tool_results")
                        put("results", buildJsonArray {
                            message.results.forEach { result ->
                                add(buildJsonObject {
                                    put("call_id", result.callId)
                                    put("name", result.name)
                                    put("content", result.content)
                                })
                            }
                        })
                    }
                },
            )
        }
    }.toString()

    fun decode(encoded: String?): List<Message> {
        if (encoded.isNullOrBlank()) return emptyList()
        return runCatching {
            json.parseToJsonElement(encoded).jsonArray.mapNotNull { element ->
                val o = element.jsonObject
                when (o["kind"]?.jsonPrimitive?.content) {
                    "user" -> o["text"]?.jsonPrimitive?.content?.let(Message::User)
                    "assistant" -> Message.Assistant(
                        text = o["text"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                        toolCalls = o["tool_calls"]?.jsonArray?.mapNotNull(::decodeCall).orEmpty(),
                    )
                    "tool_results" -> Message.ToolResults(
                        o["results"]?.jsonArray?.mapNotNull { result ->
                            val r = result.jsonObject
                            val callId = r["call_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            val name = r["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            val content = r["content"]?.jsonObject ?: return@mapNotNull null
                            ToolResponse(callId, name, content)
                        }.orEmpty(),
                    )
                    else -> null
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeCall(call: ToolCall) = buildJsonObject {
        put("id", call.id)
        put("name", call.name)
        put("args", call.args)
        call.providerData?.let { put("provider_data", it) }
    }

    private fun decodeCall(element: JsonElement): ToolCall? = runCatching {
        val o = element.jsonObject
        ToolCall(
            id = o["id"]!!.jsonPrimitive.content,
            name = o["name"]!!.jsonPrimitive.content,
            args = o["args"]!!.jsonObject,
            providerData = o["provider_data"],
        )
    }.getOrNull()
}
