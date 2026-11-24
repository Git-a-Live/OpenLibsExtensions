package io.libwrap.kv.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.libwrap.kv.Kv
import io.libwrap.kv.StoredValue
import io.libwrap.kv.isNativeType
import io.libwrap.kv.toStoredValue
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlinx.coroutines.flow.first

/**
 * 与 [io.libwrap.kv.Kv] 共用同一套类型分发规则与编解码器，但后端换成 AndroidX DataStore。
 *
 * 语法保持一致（`42 toKey "age"`、`getKV<Int>("age")`），差别只在**函数是挂起的**：
 * DataStore 的读取是 `Flow<Preferences>`、写入是 `edit` 挂起事务，没有同步读的能力。
 * 强行做成同步只能靠内存快照，那会带来"刚写完立刻读拿到旧值"的错觉，因此这里不做同步封装。
 *
 * 导入本包的 `toKey` / `getKV` 即切换到 DataStore 后端，两个后端的函数名相同、签名不同
 * （挂起与否），置于不同包避免冲突。编解码器通过 [Kv.useCodec] 统一设置。
 *
 * 约定与 MMKV 后端一致：同一个 key 必须用同一类型读写。
 */
public object DataStoreKv {

    @Volatile
    private var store: DataStore<Preferences>? = null

    public fun init(dataStore: DataStore<Preferences>) {
        store = dataStore
    }

    @PublishedApi
    internal fun store(): DataStore<Preferences> =
        store ?: error("DataStoreKv.init(dataStore) 尚未调用")
}

/** 把 [this] 写入 [key]，基础类型走原生编码，其余类型走编解码器。 */
public suspend inline infix fun <reified T : Any> T.toKey(key: String) {
    val store = DataStoreKv.store()
    if (putNativePreferences(store, key, this)) return
    Kv.codec().encode(this, typeOf<T>())?.let { writeText(store, key, it) }
}

/** 按 [T] 的类型读出 [key] 的值；键不存在或解码失败时返回 null。 */
public suspend inline fun <reified T : Any> getKV(key: String): T? {
    val store = DataStoreKv.store()
    val preferences = store.data.first()
    return if (isNativeType(T::class)) {
        readNativePreferences(preferences, key, T::class)
    } else {
        preferences[stringPreferencesKey(key)]?.let { Kv.codec().decode<T>(it, typeOf<T>()) }
    }
}

/** 显式指定 key 与值的写法；[value] 为 null 时删除该键。 */
public suspend inline fun <reified T : Any> putKV(key: String, value: T?) {
    if (value == null) {
        removeValue(key)
    } else {
        value toKey key
    }
}

/** 删除 [this] 对应的键。 */
public suspend fun String.removeKV() {
    removeValue(this)
}

/** 清空全部键值。 */
public suspend fun clearKV() {
    DataStoreKv.store().edit { preferences ->
        preferences.clear()
    }
}

/** [this] 对应的任意类型键是否存在。 */
public suspend fun String.containsValue(): Boolean {
    val key = this
    return DataStoreKv.store().data.first().asMap().keys.any { it.name == key }
}

/** 基础类型直接落库，返回是否已处理；返回 false 表示需要交给编解码器。 */
@PublishedApi
internal suspend fun putNativePreferences(
    store: DataStore<Preferences>,
    key: String,
    value: Any,
): Boolean {
    val stored = value.toStoredValue() ?: return false
    store.edit { preferences ->
        when (stored) {
            is StoredValue.Text -> preferences[stringPreferencesKey(key)] = stored.value
            is StoredValue.IntValue -> preferences[intPreferencesKey(key)] = stored.value
            is StoredValue.LongValue -> preferences[longPreferencesKey(key)] = stored.value
            is StoredValue.FloatValue -> preferences[floatPreferencesKey(key)] = stored.value
            is StoredValue.DoubleValue -> preferences[doublePreferencesKey(key)] = stored.value
            is StoredValue.BooleanValue -> preferences[booleanPreferencesKey(key)] = stored.value
        }
    }
    return true
}

/** 基础类型的读取；非基础类型返回 null（调用方应先判 [isNativeType]）。 */
@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> readNativePreferences(
    preferences: Preferences,
    key: String,
    type: KClass<T>,
): T? = when (type) {
    String::class -> preferences[stringPreferencesKey(key)]
    Int::class -> preferences[intPreferencesKey(key)]
    Long::class -> preferences[longPreferencesKey(key)]
    Float::class -> preferences[floatPreferencesKey(key)]
    Double::class -> preferences[doublePreferencesKey(key)]
    Boolean::class -> preferences[booleanPreferencesKey(key)]
    else -> null
} as T?

@PublishedApi
internal suspend fun writeText(store: DataStore<Preferences>, key: String, value: String) {
    store.edit { preferences ->
        preferences[stringPreferencesKey(key)] = value
    }
}

@PublishedApi
internal suspend fun removeValue(key: String) {
    DataStoreKv.store().edit { preferences ->
        preferences.remove(stringPreferencesKey(key))
        preferences.remove(intPreferencesKey(key))
        preferences.remove(longPreferencesKey(key))
        preferences.remove(floatPreferencesKey(key))
        preferences.remove(doublePreferencesKey(key))
        preferences.remove(booleanPreferencesKey(key))
    }
}
