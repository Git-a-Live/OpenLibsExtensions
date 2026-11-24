package io.libwrap.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import kotlin.reflect.typeOf

/**
 * Moshi 的序列化入口。
 *
 * 与 Gson 不同，Moshi 不对任意对象做反射建模：每个类型都需要一个 `JsonAdapter`，
 * 由 Moshi 代码生成（`@JsonClass(generateAdapter = true)`）或由调用方注册提供。
 * 因此序列化入口要求类型在调用点可推导，无法对 `Any` 做通用处理。
 *
 * ```kotlin
 * @JsonClass(generateAdapter = true)
 * data class User(val name: String, val age: Int)
 *
 * User("n", 1).toJsonOrNull()          // 需要 codegen 生成的 adapter
 * """{"name":"n"}""".toObjectOrNull<User>()
 * ```
 *
 * 未使用代码生成时，可通过 [useMoshi] 注入添加了 `KotlinJsonAdapterFactory` 的实例；
 * 后者依赖 kotlin-reflect，本模块不引入。
 */
public object MoshiKit {

    @Volatile
    private var instance: Moshi = Moshi.Builder().build()

    public val moshi: Moshi
        get() = instance

    /** 替换全局实例，例如注入添加了自定义 adapter 或 `KotlinJsonAdapterFactory` 的 Moshi。 */
    public fun useMoshi(moshi: Moshi) {
        instance = moshi
    }
}

/** 序列化为 JSON；类型缺少 adapter 或序列化失败时返回 null。 */
public inline fun <reified T : Any> T.toJsonOrNull(): String? =
    toJsonOrNull(MoshiKit.moshi, typeOf<T>().toJavaType())

/** 序列化为 JSON；失败时返回空字符串。 */
public inline fun <reified T : Any> T.toJsonOrEmpty(): String = toJsonOrNull() ?: ""

/** 按 [type] 序列化，用于类型无法在调用点推导的场景。 */
public fun <T : Any> T.toJsonOrNull(moshi: Moshi, type: Type): String? = runCatching {
    moshi.adapter<T>(type).toJson(this)
}.getOrNull()

/** 反序列化；解析失败或缺少 adapter 时返回 null。 */
public inline fun <reified T : Any> String?.toObjectOrNull(): T? =
    toObjectOrNull(MoshiKit.moshi, typeOf<T>().toJavaType())

/** 按 [clazz] 反序列化，适用于非泛型类型。 */
public fun <T : Any> String?.toObjectOrNull(clazz: Class<T>): T? =
    toObjectOrNull(MoshiKit.moshi, clazz as Type)

/** 按 [type] 反序列化，适用于泛型类型。 */
public fun <T : Any> String?.toObjectOrNull(moshi: Moshi, type: Type): T? {
    if (this.isNullOrBlank()) return null
    return runCatching { moshi.adapter<T>(type).fromJson(this) }.getOrNull()
}

/** 反序列化为 `List<T>`。 */
public inline fun <reified T : Any> String?.toListOrNull(): List<T>? {
    val elementType = typeOf<T>().toJavaType()
    return toObjectOrNull(MoshiKit.moshi, Types.newParameterizedType(List::class.java, elementType))
}

/** 反序列化为无类型映射，等价于 Moshi 的默认树模型。 */
public fun String?.toMapOrNull(): Map<String, Any?>? =
    toObjectOrNull(
        MoshiKit.moshi,
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )
