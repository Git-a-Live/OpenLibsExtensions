package io.libwrap.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

/**
 * 共享的 [Gson] 实例。
 *
 * 配置为关闭 HTML 转义（`<` `>` `&` `=` `'` 原样输出），避免 JSON 里的 URL 或富文本
 * 被写成 `\u003c` 一类不可读形式。需要自定义日期格式、字段命名策略时，直接用自己
 * 的 [Gson] 实例配合本文件里的扩展函数即可。
 */
public object GsonKit {

    @JvmField
    public val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()
}

/** 对象转 JSON 文本；入参为 null 或序列化失败时返回 null。 */
public fun Any?.toJsonOrNull(): String? {
    if (this == null) return null
    return runCatching { GsonKit.gson.toJson(this) }.getOrNull()
}

/** 对象转 JSON 文本；失败时返回空串，便于直接拼进字符串模板。 */
public fun Any?.toJsonOrEmpty(): String = toJsonOrNull().orEmpty()

/** 对象转 JsonElement；失败时返回 null。 */
public fun Any?.toJsonTreeOrNull(): JsonElement? {
    if (this == null) return null
    return runCatching { GsonKit.gson.toJsonTree(this) }.getOrNull()
}

/** JSON 文本反序列化；文本为空或格式非法时返回 null。 */
public fun <T> String?.toObjectOrNull(clazz: Class<T>): T? {
    if (isNullOrBlank()) return null
    return runCatching { GsonKit.gson.fromJson(this, clazz) }.getOrNull()
}

public inline fun <reified T> String?.toObjectOrNull(): T? = toObjectOrNull(T::class.java)

/** 反序列化为 List；失败或文本为空返回 null，与"空列表"语义区分开。 */
public inline fun <reified T> String?.toListOrNull(): List<T>? {
    if (isNullOrBlank()) return null
    val type = object : TypeToken<List<T>>() {}.type
    return runCatching { GsonKit.gson.fromJson<List<T>>(this, type) }.getOrNull()
}

/** 解析为 JsonObject；文本不是 JSON 对象时返回 null。 */
public fun String?.toJsonObjectOrNull(): JsonObject? {
    if (isNullOrBlank()) return null
    return runCatching {
        val element = GsonKit.gson.fromJson(this, JsonElement::class.java)
        if (element != null && element.isJsonObject) element.asJsonObject else null
    }.getOrNull()
}

/** 解析为 JsonArray；文本不是 JSON 数组时返回 null。 */
public fun String?.toJsonArrayOrNull(): JsonArray? {
    if (isNullOrBlank()) return null
    return runCatching {
        val element = GsonKit.gson.fromJson(this, JsonElement::class.java)
        if (element != null && element.isJsonArray) element.asJsonArray else null
    }.getOrNull()
}

/** JsonElement 转指定类型；类型不符或解析失败返回 null。 */
public fun <T> JsonElement?.toObjectOrNull(clazz: Class<T>): T? {
    if (this == null) return null
    return runCatching { GsonKit.gson.fromJson(this, clazz) }.getOrNull()
}

public inline fun <reified T> JsonElement?.toObjectOrNull(): T? = toObjectOrNull(T::class.java)
