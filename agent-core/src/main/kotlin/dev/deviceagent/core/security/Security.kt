package dev.deviceagent.core.security

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Removes secrets from anything that is logged or sent onward. Known secrets (the user's API
 * key) are registered at runtime; common key/token shapes are also caught by pattern.
 */
class Redactor {
    private val secrets = CopyOnWriteArrayList<String>()

    fun addSecret(secret: String) { if (secret.length >= 8 && secret !in secrets) secrets.add(secret) }
    fun clearSecrets() = secrets.clear()

    fun redact(text: String): String {
        var out = text
        for (s in secrets) out = out.replace(s, "[REDACTED]")
        for ((rx, repl) in PATTERNS) out = rx.replace(out, repl)
        return out
    }

    fun redact(e: JsonElement): JsonElement = when (e) {
        is JsonObject -> JsonObject(e.mapValues { redact(it.value) })
        is JsonArray -> JsonArray(e.map { redact(it) })
        is JsonPrimitive -> if (e.isString) JsonPrimitive(redact(e.content)) else e
        else -> e
    }

    private companion object {
        val PATTERNS = listOf(
            Regex("AIza[0-9A-Za-z_\\-]{35}") to "[REDACTED_KEY]",
            Regex("(?i)bearer\\s+[A-Za-z0-9._\\-]{16,}") to "Bearer [REDACTED]",
            Regex("(?i)((?:api[_-]?key|secret|password|token)\\s*[=:]\\s*)\\S+") to "$1[REDACTED]",
        )
    }
}

/** Secure storage for credentials. Android: Keystore-backed implementation in the app module. */
interface SecretStore {
    fun put(name: String, value: String)
    fun get(name: String): String?
    fun remove(name: String)
}

class InMemorySecretStore : SecretStore {
    private val map = HashMap<String, String>()
    override fun put(name: String, value: String) { map[name] = value }
    override fun get(name: String) = map[name]
    override fun remove(name: String) { map.remove(name) }
}

/** Shows only the last 4 characters; the full key is never displayed again after entry. */
fun maskSecret(secret: String): String =
    if (secret.length <= 8) "••••" else "••••••••" + secret.takeLast(4)
