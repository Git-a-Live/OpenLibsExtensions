package io.libwrap.kv.gson

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * 把 [KType] 还原成 `java.lang.reflect.Type`。
 *
 * 标准做法是 `kotlin.reflect.jvm.javaType`，但它属于 kotlin-reflect（约 3MB），
 * 只为这点用途引入不划算。这里只处理存储场景会遇到的两种形态：裸类型与参数化类型；
 * 通配符与类型变量不在支持范围内（作为存储键值类型没有意义）。
 */
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
