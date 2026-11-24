package io.libwrap.kv

import kotlin.reflect.KType

/**
 * 值经过类型分发后的中间形态。
 *
 * 存在的意义是让"把任意对象归类成哪几种存储类型"只写一遍：MMKV 与 DataStore 的
 * 写入分支形状不同（前者调 `encode`，后者构造 `Preferences.Key`），但归类规则必须一致，
 * 否则两个后端会写出不一样的数据。
 */
internal sealed interface StoredValue {

    @JvmInline
    value class Text(val value: String) : StoredValue

    @JvmInline
    value class IntValue(val value: Int) : StoredValue

    @JvmInline
    value class LongValue(val value: Long) : StoredValue

    @JvmInline
    value class FloatValue(val value: Float) : StoredValue

    @JvmInline
    value class DoubleValue(val value: Double) : StoredValue

    @JvmInline
    value class BooleanValue(val value: Boolean) : StoredValue
}

/** 按运行时类型归类到存储原生类型；其余类型返回 null，交由编解码器处理。 */
internal fun Any.toStoredValue(): StoredValue? = when (this) {
    is String -> StoredValue.Text(this)
    is Int -> StoredValue.IntValue(this)
    is Long -> StoredValue.LongValue(this)
    is Float -> StoredValue.FloatValue(this)
    is Double -> StoredValue.DoubleValue(this)
    is Boolean -> StoredValue.BooleanValue(this)
    else -> null
}

/** 存储层直接支持的类型，读写都不经过编解码器。 */
@PublishedApi
internal fun isNativeType(type: kotlin.reflect.KClass<*>): Boolean =
    type == String::class || type == Int::class || type == Long::class ||
        type == Float::class || type == Double::class || type == Boolean::class

/**
 * 复合类型的编解码协议。
 *
 * 基础类型不走这里，只有 String/Int/Long/Float/Double/Boolean 之外的值才需要。
 * 默认实现是 kotlinx.serialization（[KotlinxKvCodec]），宿主可换成基于反射的
 * 实现（例如 Gson）以支持未标注 `@Serializable` 的普通类——见 `:kv-gson` 模块。
 *
 * [KType] 而不是 `Class`：泛型集合（`List<Item>`）的类型参数必须一路传到底，
 * 否则查不到元素序列化器。
 */
public interface KvCodec {

    /** 编码失败或类型不支持时返回 null，调用方跳过写入。 */
    public fun encode(value: Any, type: KType): String?

    /** 解码失败时返回 null。 */
    public fun <T : Any> decode(text: String, type: KType): T?
}
