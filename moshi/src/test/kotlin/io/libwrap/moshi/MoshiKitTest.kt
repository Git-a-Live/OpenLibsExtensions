package io.libwrap.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 序列化与取值验证。
 *
 * Moshi 不为未注册的类型提供反射 adapter，因此自定义类型通过测试内手写的 [JsonAdapter]
 * 验证；内置类型（Map / List / 基础类型）走 Moshi 自带 adapter。
 */
class MoshiKitTest {

    class Point(val x: Int, val y: Int) {
        override fun equals(other: Any?): Boolean =
            other is Point && other.x == x && other.y == y

        override fun hashCode(): Int = 31 * x + y
    }

    private object PointAdapter : JsonAdapter<Point>() {
        override fun fromJson(reader: JsonReader): Point {
            var x = 0
            var y = 0
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "x" -> x = reader.nextInt()
                    "y" -> y = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return Point(x, y)
        }

        override fun toJson(writer: JsonWriter, value: Point?) {
            val point = requireNotNull(value)
            writer.beginObject()
            writer.name("x").value(point.x.toLong())
            writer.name("y").value(point.y.toLong())
            writer.endObject()
        }
    }

    /** 未注册 adapter 的类型，用于验证失败路径。 */
    class Unregistered(val id: Int)

    @Before
    fun setUp() {
        MoshiKit.useMoshi(Moshi.Builder().add(Point::class.java, PointAdapter).build())
    }

    @After
    fun tearDown() {
        MoshiKit.useMoshi(Moshi.Builder().build())
    }

    @Test
    fun `自定义类型依赖已注册的 adapter 往返`() {
        val json = Point(1, 2).toJsonOrNull()

        assertEquals("""{"x":1,"y":2}""", json)
        assertEquals(Point(1, 2), json!!.toObjectOrNull<Point>())
    }

    @Test
    fun `未注册 adapter 的类型返回 null 而不抛异常`() {
        assertNull(Unregistered(1).toJsonOrNull())
        assertNull("""{"id":1}""".toObjectOrNull<Unregistered>())
    }

    @Test
    fun `内置集合类型可往返`() {
        val list = listOf("a", "b")
        assertEquals("""["a","b"]""", list.toJsonOrNull())
        assertEquals(list, """["a","b"]""".toListOrNull<String>())
    }

    @Test
    fun `无类型映射的数字解析为 Double`() {
        val map = """{"count":3,"ratio":0.5,"name":"x"}""".toMapOrNull()

        assertEquals(3.0, map!!["count"])
        assertTrue(map["count"] is Double)
        assertEquals(3, map.intOr("count"))
        assertEquals("x", map.stringOr("name"))
    }

    @Test
    fun `空文本与非法文本返回 null`() {
        assertNull("".toObjectOrNull<Point>())
        assertNull("   ".toObjectOrNull<Point>())
        assertNull("not a json".toObjectOrNull<Point>())
    }

    @Test
    fun `toJsonOrEmpty 在失败时返回空串`() {
        assertEquals("", Unregistered(1).toJsonOrEmpty())
        assertEquals("""{"x":1,"y":2}""", Point(1, 2).toJsonOrEmpty())
    }
}
