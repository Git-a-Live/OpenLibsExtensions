package io.libwrap.kv.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * DataStore 后端的往返验证。
 *
 * 用内存实现的 [DataStore] 而不是真实文件：真正需要验证的是"按运行时类型构造对应的
 * `Preferences.Key`、按请求类型取回"这一层，文件读写属于 DataStore 自己的职责。
 * 顺带绕开了 Android 变体的原子文件操作在 JVM 单测里不可用的问题。
 */
class DataStoreKvTest {

    /** 只实现 updateData，`edit` 扩展正是建立在它之上的。 */
    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    @Before
    fun setUp() {
        DataStoreKv.init(InMemoryDataStore())
    }

    @Test
    fun `基础类型按声明类型往返`() = runBlocking {
        "text" toKey "string"
        42 toKey "int"
        42L toKey "long"
        1.5f toKey "float"
        2.5 toKey "double"
        true toKey "boolean"

        assertEquals("text", getKV<String>("string"))
        assertEquals(42, getKV<Int>("int"))
        assertEquals(42L, getKV<Long>("long"))
        assertEquals(1.5f, getKV<Float>("float"))
        assertEquals(2.5, getKV<Double>("double"))
        assertEquals(true, getKV<Boolean>("boolean"))
    }

    @Test
    fun `声明为 Any 的值仍按运行时类型落库`() = runBlocking {
        val erased: Any = 42
        erased toKey "erased"

        assertEquals(42, getKV<Int>("erased"))
    }

    @Test
    fun `自定义类型 JSON 往返`() = runBlocking {
        Payload("张三", 18) toKey "payload"

        assertEquals(Payload("张三", 18), getKV<Payload>("payload"))
        assertEquals("张三", getKV<Payload>("payload")?.name)
    }

    @Test
    fun `键不存在返回 null`() = runBlocking {
        assertNull(getKV<Int>("absent"))
        assertNull(getKV<String>("absent"))
        assertNull(getKV<Payload>("absent"))
    }

    @Test
    fun `putKV 写 null 会删除键`() = runBlocking {
        putKV("count", 7)
        assertEquals(7, getKV<Int>("count"))

        putKV<Int>("count", null)
        assertNull(getKV<Int>("count"))
        assertTrue("count 应被删除", !"count".containsValue())
    }

    @Test
    fun `删除与清空`() = runBlocking {
        "a" toKey "k1"
        "b" toKey "k2"
        assertTrue("k1".containsValue())

        "k1".removeKV()
        assertTrue("k1 应被删除", !"k1".containsValue())
        assertTrue("k2 不应受影响", "k2".containsValue())

        clearKV()
        assertTrue("清空后 k2 不应存在", !"k2".containsValue())
    }

    @Serializable
    data class Payload(val name: String, val age: Int)
}
