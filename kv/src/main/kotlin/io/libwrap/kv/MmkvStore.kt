package io.libwrap.kv

import com.tencent.mmkv.MMKV

/**
 * [KeyValueStore] 的 MMKV 实现。
 *
 * 基础类型走 MMKV 的原生编码接口，因此不需要序列化、也不会把 Int 变成字符串。
 * 读取时用 [MMKV.containsKey] 区分"键不存在"与"存了默认值"——直接调用 `decodeInt`
 * 在键缺失时返回 0，会把两种情况混为一谈。
 */
internal class MmkvStore(private val mmkv: MMKV) : KeyValueStore {

    override fun putString(key: String, value: String): Boolean = mmkv.encode(key, value)

    override fun putInt(key: String, value: Int): Boolean = mmkv.encode(key, value)

    override fun putLong(key: String, value: Long): Boolean = mmkv.encode(key, value)

    override fun putFloat(key: String, value: Float): Boolean = mmkv.encode(key, value)

    override fun putDouble(key: String, value: Double): Boolean = mmkv.encode(key, value)

    override fun putBoolean(key: String, value: Boolean): Boolean = mmkv.encode(key, value)

    override fun getString(key: String): String? = mmkv.decodeString(key, null)

    override fun getInt(key: String): Int? =
        if (mmkv.containsKey(key)) mmkv.decodeInt(key) else null

    override fun getLong(key: String): Long? =
        if (mmkv.containsKey(key)) mmkv.decodeLong(key) else null

    override fun getFloat(key: String): Float? =
        if (mmkv.containsKey(key)) mmkv.decodeFloat(key) else null

    override fun getDouble(key: String): Double? =
        if (mmkv.containsKey(key)) mmkv.decodeDouble(key) else null

    override fun getBoolean(key: String): Boolean? =
        if (mmkv.containsKey(key)) mmkv.decodeBool(key) else null

    override fun contains(key: String): Boolean = mmkv.containsKey(key)

    override fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    override fun clear() {
        mmkv.clearAll()
    }
}
