package io.libwrap.kv

import android.content.Context
import com.tencent.mmkv.MMKV
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 * MMKV 的键值访问入口，提供"按值类型自适应"的中缀语法。
 *
 * ```kotlin
 * 42 toKey "age"                       // Int 走 MMKV 原生整型编码
 * "北京" toKey "city"                  // String 原样存储
 * User("n", 1) toKey "user"            // 其余类型交给编解码器
 *
 * val age: Int? = getKV("age")
 * val user: User? = getKV("user")
 * val list: List<User>? = getKV("users")
 * ```
 *
 * 基础类型按**运行时类型**分发：把值声明成 `Any` 再写入，依然按真实的 Int / String 落库。
 * 复合类型交给 [KvCodec]，默认实现是 kotlinx.serialization，只认 `@Serializable`；
 * 需要支持普通类时用 [useCodec] 换成反射实现。
 *
 * 约定：同一个 key 必须用同一类型读写。存储层不记录值的类型，用错类型取出的结果是未定义的。
 */
public object Kv {

    @Volatile
    private var store: KeyValueStore? = null

    /**
     * 初始化 MMKV。
     *
     * @param rootDir 自定义存储根目录，null 表示使用 MMKV 默认目录
     * @param id 多实例标识，例如按登录用户隔离时传 userId；null 表示使用默认实例
     */
    @JvmOverloads
    public fun init(context: Context, rootDir: String? = null, id: String? = null) {
        if (rootDir == null) {
            MMKV.initialize(context)
        } else {
            MMKV.initialize(context, rootDir)
        }
        store = MmkvStore(if (id == null) MMKV.defaultMMKV() else MMKV.mmkvWithID(id))
    }

    /**
     * 替换复合类型的编解码器。
     *
     * 编解码器是进程级配置，MMKV 与 DataStore 两个后端共用同一份。
     * 换成反射实现后，未标注 `@Serializable` 的普通类也能读写。
     */
    public fun useCodec(codec: KvCodec) {
        CodecHolder.codec = codec
    }

    @PublishedApi
    internal fun store(): KeyValueStore =
        store ?: error("Kv.init(context) 尚未调用")

    @PublishedApi
    internal fun codec(): KvCodec = CodecHolder.codec

    /** 替换底层实现，仅用于单元测试。 */
    internal fun useStore(replacement: KeyValueStore) {
        store = replacement
    }
}

/** 把 [this] 写入 [key]，基础类型走原生编码，其余类型走编解码器。 */
public inline infix fun <reified T : Any> T.toKey(key: String) {
    val store = Kv.store()
    if (putNative(store, key, this)) return
    Kv.codec().encode(this, typeOf<T>())?.let { store.putString(key, it) }
}

/** 按 [T] 的类型读出 [key] 的值；键不存在或解码失败时返回 null。 */
public inline fun <reified T : Any> getKV(key: String): T? {
    val store = Kv.store()
    return if (isNativeType(T::class)) {
        readNative(store, key, T::class)
    } else {
        store.getString(key)?.let { Kv.codec().decode<T>(it, typeOf<T>()) }
    }
}

/** [getKV] 的另一种写法，保留 `String::class fromKey "key"` 的语法。 */
public inline infix fun <reified T : Any> KClass<T>.fromKey(key: String): T? = getKV<T>(key)

/** 显式指定 key 与值的写法；[value] 为 null 时删除该键。 */
public inline fun <reified T : Any> putKV(key: String, value: T?) {
    if (value == null) {
        Kv.store().remove(key)
    } else {
        value toKey key
    }
}

/** 删除 [this] 对应的键。 */
public fun String.removeKV() {
    Kv.store().remove(this)
}

/** 清空全部键值。 */
public fun clearKV() {
    Kv.store().clear()
}

/** [this] 对应的键是否存在。 */
public fun String.containsValue(): Boolean = Kv.store().contains(this)

/** 基础类型直接落库，返回是否已处理；返回 false 表示需要交给编解码器。 */
@PublishedApi
internal fun putNative(store: KeyValueStore, key: String, value: Any): Boolean {
    when (val stored = value.toStoredValue()) {
        null -> return false
        is StoredValue.Text -> store.putString(key, stored.value)
        is StoredValue.IntValue -> store.putInt(key, stored.value)
        is StoredValue.LongValue -> store.putLong(key, stored.value)
        is StoredValue.FloatValue -> store.putFloat(key, stored.value)
        is StoredValue.DoubleValue -> store.putDouble(key, stored.value)
        is StoredValue.BooleanValue -> store.putBoolean(key, stored.value)
    }
    return true
}

/** 基础类型的读取；非基础类型返回 null（调用方应先判 [isNativeType]）。 */
@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> readNative(store: KeyValueStore, key: String, type: KClass<T>): T? = when (type) {
    String::class -> store.getString(key)
    Int::class -> store.getInt(key)
    Long::class -> store.getLong(key)
    Float::class -> store.getFloat(key)
    Double::class -> store.getDouble(key)
    Boolean::class -> store.getBoolean(key)
    else -> null
} as T?
