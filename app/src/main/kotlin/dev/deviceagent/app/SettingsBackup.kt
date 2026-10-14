package dev.deviceagent.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A small, portable copy of the user's AI choices, for moving to a new phone.
 *
 * What is deliberately NOT in it:
 * - API keys (they never leave the Android Keystore),
 * - folder grants (Android ties those to this install),
 * - the "send file contents" privacy switch (a file someone handed you must never be able to turn it on).
 *
 * This file has no Android imports so it can be tested on a plain JVM.
 */
data class SettingsBackup(
    val provider: String,
    val models: Map<String, String>,
    val recoveryModels: Map<String, String>,
    val automaticModelRouting: Boolean,
)

object SettingsBackupCodec {
    const val VERSION = 1
    const val MAX_BYTES = 64 * 1024
    private const val MAX_MODEL_CHARS = 200

    fun encode(b: SettingsBackup): String = JsonObject(
        mapOf(
            "app" to JsonPrimitive("synapse"),
            "version" to JsonPrimitive(VERSION),
            "provider" to JsonPrimitive(b.provider),
            "models" to JsonObject(b.models.mapValues { JsonPrimitive(it.value) }),
            "recoveryModels" to JsonObject(b.recoveryModels.mapValues { JsonPrimitive(it.value) }),
            "automaticModelRouting" to JsonPrimitive(b.automaticModelRouting),
        ),
    ).toString()

    /**
     * Returns null for anything that is not a valid Synapse settings file. Unknown providers and
     * unexpected fields are dropped, never trusted, and model names must be short plain text.
     */
    fun decode(text: String, knownProviders: Set<String>): SettingsBackup? {
        if (text.length > MAX_BYTES) return null
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
        // Accept backups exported before the Synapse rename.
        val app = root["app"]?.jsonPrimitiveOrNull()?.contentOrNull
        if (app != "synapse" && app != "fileora") return null
        if (root["version"]?.jsonPrimitiveOrNull()?.contentOrNull?.toIntOrNull() != VERSION) return null
        val provider = root["provider"]?.jsonPrimitiveOrNull()?.contentOrNull?.takeIf { it in knownProviders } ?: return null
        val routing = root["automaticModelRouting"]?.jsonPrimitiveOrNull()?.contentOrNull?.toBooleanStrictOrNull() ?: false
        return SettingsBackup(
            provider = provider,
            models = cleanModels(root["models"], knownProviders),
            recoveryModels = cleanModels(root["recoveryModels"], knownProviders),
            automaticModelRouting = routing,
        )
    }

    private fun cleanModels(e: JsonElement?, known: Set<String>): Map<String, String> {
        val obj = e as? JsonObject ?: return emptyMap()
        return obj.entries.mapNotNull { (k, v) ->
            val name = v.jsonPrimitiveOrNull()?.takeIf { it.isString }?.content?.trim() ?: return@mapNotNull null
            if (k !in known || name.isEmpty() || name.length > MAX_MODEL_CHARS || name.any { it.isISOControl() }) null else k to name
        }.toMap()
    }

    private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive
}
