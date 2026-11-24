package io.libwrap.moshi

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * 把 [KType] 还原成 `java.lang.reflect.Type`，供 `Moshi.adapter(Type)` 使用。
 *
 * 标准做法 `kotlin.reflect.jvm.javaType` 属于 kotlin-reflect（约 3MB），
 * 本模块不为这一处用途引入。仅处理裸类型与参数化类型，通配符与类型变量不在支持范围。
 */
@PublishedApi
internal fun KType.toJavaType(): Type {
    val raw = (classifier as? KClass<*>)?.java
        ?: throw IllegalArgumentException("unsupported type: $this")
    if (arguments.isEmpty()) return raw
    val typeArguments = arguments.map { it.type?.toJavaType() ?: Any::class.java }.toTypedArray()
    return ParameterizedTypeImpl(raw, typeArguments)
}

private class ParameterizedTypeImpl(
    private val raw: Class<*>,
    private val typeArguments: Array<Type>,
) : ParameterizedType {

    override fun getActualTypeArguments(): Array<Type> = typeArguments

    override fun getRawType(): Type = raw

    override fun getOwnerType(): Type? = null
}
