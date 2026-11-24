package io.libwrap.moshi

/**
 * 无类型树模型（`Map<String, Any?>`）的安全取值。
 *
 * Moshi 默认将所有数字解析为 `Double`，因此数值取值的实现必须兼容 `Double`、`Long`、
 * `Int`、`String` 四种来源，不能只做类型判断。缺失、`null` 与类型不符一律回退默认值。
 */
public fun Map<String, Any?>.stringOr(key: String, fallback: String = ""): String =
    when (val value = this[key]) {
        is String -> value
        is Number, is Boolean -> value.toString()
        else -> fallback
    }

public fun Map<String, Any?>.stringOrNull(key: String): String? =
    when (val value = this[key]) {
        is String -> value
        is Number, is Boolean -> value.toString()
        else -> null
    }

public fun Map<String, Any?>.intOr(key: String, fallback: Int = 0): Int =
    when (val value = this[key]) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: fallback
        else -> fallback
    }

public fun Map<String, Any?>.longOr(key: String, fallback: Long = 0L): Long =
    when (val value = this[key]) {
        is Long -> value
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: fallback
        else -> fallback
    }

public fun Map<String, Any?>.doubleOr(key: String, fallback: Double = 0.0): Double =
    when (val value = this[key]) {
        is Double -> value
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: fallback
        else -> fallback
    }

public fun Map<String, Any?>.floatOr(key: String, fallback: Float = 0f): Float =
    doubleOr(key, fallback.toDouble()).toFloat()

public fun Map<String, Any?>.booleanOr(key: String, fallback: Boolean = false): Boolean =
    when (val value = this[key]) {
        is Boolean -> value
        is String -> when (value.lowercase()) {
            "true" -> true
            "false" -> false
            else -> fallback
        }
        else -> fallback
    }

public fun Map<String, Any?>.mapOrNull(key: String): Map<String, Any?>? {
    @Suppress("UNCHECKED_CAST")
    return this[key] as? Map<String, Any?>
}

public fun Map<String, Any?>.listOrNull(key: String): List<Any?>? {
    @Suppress("UNCHECKED_CAST")
    return this[key] as? List<Any?>
}

/** 值缺失或为显式 `null` 时返回 true。 */
public fun Map<String, Any?>.isNullOrAbsent(key: String): Boolean = this[key] == null
