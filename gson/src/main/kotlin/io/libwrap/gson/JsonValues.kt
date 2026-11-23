package io.libwrap.gson

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/**
 * [JsonObject] 的"永不抛异常"取值扩展。
 *
 * 三类情况统一回退到默认值：字段不存在、字段值为 `null`、字段类型与期望不符。
 * Gson 原生的 `get("k").asInt` 在类型不符时会抛 `NumberFormatException` /
 * `UnsupportedOperationException`，在解析三方数据的场景里这属于预期情况，不应升级为崩溃。
 */

/** 取字符串；对象/数组字段返回其 JSON 文本，与 JSON 语义保持可逆。 */
public fun JsonObject.stringOr(key: String, fallback: String = ""): String {
    val element = get(key) ?: return fallback
    if (element.isJsonNull) return fallback
    if (element.isJsonPrimitive) {
        val primitive = element.asJsonPrimitive
        return if (primitive.isString) primitive.asString else primitive.toString()
    }
    return element.toString()
}

/** 取字符串；字段缺失或为 `null` 时返回 null。 */
public fun JsonObject.stringOrNull(key: String): String? {
    val element = get(key) ?: return null
    if (element.isJsonNull) return null
    if (element.isJsonPrimitive) {
        val primitive = element.asJsonPrimitive
        return if (primitive.isString) primitive.asString else primitive.toString()
    }
    return element.toString()
}

public fun JsonObject.intOr(key: String, fallback: Int = 0): Int =
    numberOr(key) { it.asInt }?.toInt() ?: fallback

public fun JsonObject.longOr(key: String, fallback: Long = 0L): Long =
    numberOr(key) { it.asLong }?.toLong() ?: fallback

public fun JsonObject.doubleOr(key: String, fallback: Double = 0.0): Double =
    numberOr(key) { it.asDouble }?.toDouble() ?: fallback

public fun JsonObject.floatOr(key: String, fallback: Float = 0f): Float =
    numberOr(key) { it.asFloat }?.toFloat() ?: fallback

/** 取布尔值；Gson 对字符串 `"true"` 也返回 true，其余非法值回退默认值。 */
public fun JsonObject.booleanOr(key: String, fallback: Boolean = false): Boolean {
    val primitive = primitiveOrNull(key) ?: return fallback
    return runCatching { primitive.asBoolean }.getOrDefault(fallback)
}

public fun JsonObject.objectOrNull(key: String): JsonObject? {
    val element = get(key) ?: return null
    return if (element.isJsonObject) element.asJsonObject else null
}

public fun JsonObject.arrayOrNull(key: String): JsonArray? {
    val element = get(key) ?: return null
    return if (element.isJsonArray) element.asJsonArray else null
}

/** 取原始节点，字段缺失时返回 null（字段存在但为 `null` 时返回 `JsonNull`）。 */
public fun JsonObject.elementOrNull(key: String): JsonElement? = get(key)

/** 安全取字符串，`JsonNull` 视为缺失。 */
public fun JsonElement?.textOrNull(): String? = when {
    this == null || isJsonNull -> null
    isJsonPrimitive -> asJsonPrimitive.let { if (it.isString) it.asString else it.toString() }
    else -> toString()
}

public fun JsonElement?.intOrNull(): Int? =
    this?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asInt }.getOrNull() }

public fun JsonElement?.longOrNull(): Long? =
    this?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asLong }.getOrNull() }

public fun JsonElement?.doubleOrNull(): Double? =
    this?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asDouble }.getOrNull() }

public fun JsonElement?.booleanOrNull(): Boolean? =
    this?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asBoolean }.getOrNull() }

private inline fun JsonObject.numberOr(key: String, convert: (JsonPrimitive) -> Number): Number? {
    val primitive = primitiveOrNull(key) ?: return null
    return runCatching { convert(primitive) }.getOrNull()
}

private fun JsonObject.primitiveOrNull(key: String): JsonPrimitive? {
    val element = get(key) ?: return null
    if (!element.isJsonPrimitive) return null
    return element.asJsonPrimitive
}
