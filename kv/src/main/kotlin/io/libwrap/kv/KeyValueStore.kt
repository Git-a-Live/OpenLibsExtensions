package io.libwrap.kv

/**
 * 键值存储的最小抽象。
 *
 * 存在的意义是把"按类型分发"的逻辑与具体存储实现解耦：分发逻辑可以在 JVM 单元测试里
 * 用一个内存实现验证，不必依赖 MMKV 的 native 库与真机。
 *
 * 读取方法一律以 `null` 表示"键不存在"，实现方必须区分"缺省值"与"存了默认值"。
 */
public interface KeyValueStore {

    public fun putString(key: String, value: String): Boolean

    public fun putInt(key: String, value: Int): Boolean

    public fun putLong(key: String, value: Long): Boolean

    public fun putFloat(key: String, value: Float): Boolean

    public fun putDouble(key: String, value: Double): Boolean

    public fun putBoolean(key: String, value: Boolean): Boolean

    public fun getString(key: String): String?

    public fun getInt(key: String): Int?

    public fun getLong(key: String): Long?

    public fun getFloat(key: String): Float?

    public fun getDouble(key: String): Double?

    public fun getBoolean(key: String): Boolean?

    public fun contains(key: String): Boolean

    public fun remove(key: String)

    public fun clear()
}
