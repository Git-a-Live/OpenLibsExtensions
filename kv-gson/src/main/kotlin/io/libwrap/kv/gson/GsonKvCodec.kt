package io.libwrap.kv.gson

import com.google.gson.Gson
import io.libwrap.kv.KvCodec
import kotlin.reflect.KType

/**
 * 基于 Gson 反射的编解码器，用于支持未标注 `@Serializable` 的普通类。
 *
 * 默认实现（kotlinx.serialization）只认 `@Serializable`，遇到普通类会静默跳过；
 * 换成这个实现后任意类都能读写，代价是：
 *
 * 1. 序列化走反射，需要宿主在 R8 配置里保留数据类的无参构造与字段名
 *    （Gson 自带的 `gson.pro` 只覆盖库内部路径，不覆盖业务类）；
 * 2. 反序列化依赖无参构造，没有默认值的 `val` 属性无法被填充。
 *
 * ```kotlin
 * Kv.useCodec(GsonKvCodec())
 * 42 toKey "age"            // 基础类型仍走原生编码，不经过 Gson
 * Plain(1, "a") toKey "p"   // 普通类也能存
 * ```
 */
public class GsonKvCodec @JvmOverloads constructor(
    private val gson: Gson = Gson(),
) : KvCodec {

    override fun encode(value: Any, type: KType): String? =
        runCatching { gson.toJson(value) }.getOrNull()

    override fun <T : Any> decode(text: String, type: KType): T? =
        runCatching { gson.fromJson<T>(text, type.toJavaType()) }.getOrNull()
}
