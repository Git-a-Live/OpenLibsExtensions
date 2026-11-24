package io.libwrap.kv

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KType

/**
 * 键值模块共用的 JSON 实例。
 *
 * `ignoreUnknownKeys` 让新增字段后的旧数据仍能读回；`encodeDefaults` 保证带默认值的字段
 * 也写进 JSON，否则默认值改动后旧数据会被解析成新的默认值而不是写入时的值。
 */
internal val kvJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * 默认编解码器：kotlinx.serialization，无反射序列化。
 *
 * 只能处理标注了 `@Serializable` 的类型，查不到序列化器时返回 null（静默跳过）。
 * 需要支持普通类时用 `Kv.useCodec` 换成反射实现。
 */
internal object KotlinxKvCodec : KvCodec {

    override fun encode(value: Any, type: KType): String? {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerOrNull(type) as? KSerializer<Any> ?: return null
        return runCatching { kvJson.encodeToString(serializer, value) }.getOrNull()
    }

    override fun <T : Any> decode(text: String, type: KType): T? {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerOrNull(type) as? KSerializer<T> ?: return null
        return runCatching { kvJson.decodeFromString(serializer, text) }.getOrNull()
    }
}

/** 编解码器是进程级配置，MMKV 与 DataStore 两个后端共用同一份。 */
internal object CodecHolder {

    @Volatile
    var codec: KvCodec = KotlinxKvCodec
}
