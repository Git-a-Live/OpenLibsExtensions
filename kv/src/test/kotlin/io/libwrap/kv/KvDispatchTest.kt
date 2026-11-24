package io.libwrap.kv

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 分发逻辑的单元测试。
 *
 * 用内存实现替代 MMKV，因为 MMKV 依赖 native 库，只能在设备上跑；
 * 这里要验证的是"按什么类型落库、按什么类型取回"这一层，与具体存储无关。
 */
class KvDispatchTest {

    /** 记录写入时的原始类型，用来断言分发是否走对了分支。 */
    private class FakeStore : KeyValueStore {
        val values = mutableMapOf<String, Any>()

        override fun putString(key: String, value: String) = values.put(key, value) == null
        override fun putInt(key: String, value: Int) = values.put(key, value) == null
        override fun putLong(key: String, value: Long) = values.put(key, value) == null
        override fun putFloat(key: String, value: Float) = values.put(key, value) == null
        override fun putDouble(key: String, value: Double) = values.put(key, value) == null
        override fun putBoolean(key: String, value: Boolean) = values.put(key, value) == null

        override fun getString(key: String): String? = values[key] as? String
        override fun getInt(key: String): Int? = values[key] as? Int
        override fun getLong(key: String): Long? = values[key] as? Long
        override fun getFloat(key: String): Float? = values[key] as? Float
        override fun getDouble(key: String): Double? = values[key] as? Double
        override fun getBoolean(key: String): Boolean? = values[key] as? Boolean

        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun remove(key: String) {
            values.remove(key)
        }

        override fun clear() {
            values.clear()
        }
    }

    private lateinit var fake: FakeStore

    @Before
    fun setUp() {
        fake = FakeStore()
        Kv.useStore(fake)
    }

    @Test
    fun `基础类型按原生类型落库`() {
        "text" toKey "string"
        42 toKey "int"
        42L toKey "long"
        1.5f toKey "float"
        2.5 toKey "double"
        true toKey "boolean"

        assertEquals("text", fake.values["string"])
        assertEquals(42, fake.values["int"])
        assertEquals(42L, fake.values["long"])
        assertEquals(1.5f, fake.values["float"])
        assertEquals(2.5, fake.values["double"])
        assertEquals(true, fake.values["boolean"])
    }

    @Test
    fun `声明为 Any 的值仍按运行时类型落库`() {
        val erased: Any = 42
        erased toKey "erasedInt"

        assertEquals("声明为 Any 的 Int 不应退化成 JSON", 42, fake.values["erasedInt"])
    }

    @Test
    fun `自定义类型序列化为 JSON 字符串`() {
        Payload("张三", 18) toKey "payload"

        val stored = fake.values["payload"]
        assertTrue("应为 String，实际 ${stored?.javaClass?.simpleName}", stored is String)
        assertTrue((stored as String).contains("\"name\""))
    }

    @Test
    fun `按类型读回各基础类型`() {
        "text" toKey "string"
        42 toKey "int"
        false toKey "boolean"

        assertEquals("text", getKV<String>("string"))
        assertEquals(42, getKV<Int>("int"))
        assertEquals(false, getKV<Boolean>("boolean"))
    }

    @Test
    fun `自定义类型可 JSON 往返`() {
        val payload = Payload("李四", 20)
        payload toKey "payload"

        assertEquals(payload, getKV<Payload>("payload"))
    }

    @Test
    fun `泛型集合可往返`() {
        val items = listOf(Payload("a", 1), Payload("b", 2))
        items toKey "list"

        assertEquals(items, getKV<List<Payload>>("list"))
    }

    @Test
    fun `未标注 Serializable 的类不写入也不抛异常`() {
        Plain(1) toKey "plain"

        assertTrue("不应写入存储", !fake.values.containsKey("plain"))
        assertNull(getKV<Plain>("plain"))
    }

    @Test
    fun `键不存在时返回 null 而不是默认值`() {
        assertNull(getKV<Int>("absent"))
        assertNull(getKV<String>("absent"))
        assertNull(getKV<Boolean>("absent"))
        assertNull(getKV<Payload>("absent"))
    }

    @Test
    fun `存了 0 与键不存在可以区分`() {
        0 toKey "zero"

        assertEquals(0, getKV<Int>("zero"))
        assertNull(getKV<Int>("neverWritten"))
    }

    @Test
    fun `putKV 写入与删除`() {
        putKV("count", 7)
        assertEquals(7, getKV<Int>("count"))

        putKV<Int>("count", null)
        assertNull(getKV<Int>("count"))
        assertTrue("写 null 应删除键", !"count".containsValue())
    }

    @Test
    fun `fromKey 语法读取`() {
        99 toKey "age"

        assertEquals(99, Int::class fromKey "age")
        assertEquals("fallback", String::class fromKey "missing" ?: "fallback")
    }

    @Test
    fun `删除与清空`() {
        "a" toKey "k1"
        "b" toKey "k2"
        assertTrue("k1".containsValue())

        "k1".removeKV()
        assertTrue("k1 应被删除", !"k1".containsValue())
        assertTrue("k2 不应受影响", "k2".containsValue())

        clearKV()
        assertTrue(fake.values.isEmpty())
    }

    class Plain(val id: Int)

    @Serializable
    data class Payload(val name: String, val age: Int)
}
