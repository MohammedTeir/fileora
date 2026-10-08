package dev.deviceagent.core.util

import kotlinx.serialization.json.*

fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

fun JsonObject.long(key: String): Long? {
    val p = (this[key] as? JsonPrimitive)?.takeIf { !it.isString } ?: return null
    p.longOrNull?.let { return it }
    val d = p.doubleOrNull ?: return null
    return if (d == Math.floor(d) && !d.isInfinite()) d.toLong() else null
}

fun JsonObject.bool(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull

/** Deterministic string form (object keys sorted) used to fingerprint tool calls. */
fun canonicalJson(e: JsonElement): String = when (e) {
    is JsonObject -> e.entries.sortedBy { it.key }
        .joinToString(",", "{", "}") { "\"${it.key}\":${canonicalJson(it.value)}" }
    is JsonArray -> e.joinToString(",", "[", "]") { canonicalJson(it) }
    else -> e.toString()
}

fun jsonObjectOf(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((k, v) in pairs) put(k, toJson(v))
}

fun toJson(v: Any?): JsonElement = when (v) {
    null -> JsonNull
    is JsonElement -> v
    is String -> JsonPrimitive(v)
    is Boolean -> JsonPrimitive(v)
    is Int -> JsonPrimitive(v)
    is Long -> JsonPrimitive(v)
    is Double -> JsonPrimitive(v)
    is Number -> JsonPrimitive(v.toDouble())
    is Map<*, *> -> buildJsonObject { v.forEach { (k, x) -> put(k.toString(), toJson(x)) } }
    is Iterable<*> -> JsonArray(v.map { toJson(it) })
    else -> JsonPrimitive(v.toString())
}
