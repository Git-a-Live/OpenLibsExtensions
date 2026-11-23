package io.libwrap.gson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [GsonKit] 的容错约定：失败返回 null，不向调用方抛解析异常。
 */
class GsonKitTest {

    data class Payload(val code: Int, val name: String)

    @Test
    fun `对象与 JSON 文本可双向转换`() {
        val payload = Payload(200, "ok")
        val json = payload.toJsonOrNull()
        assertEquals("""{"code":200,"name":"ok"}""", json)
        assertEquals(payload, json.toObjectOrNull<Payload>())
    }

    @Test
    fun `null 入参返回 null 而不是字符串 null`() {
        val nothing: Payload? = null
        assertNull(nothing.toJsonOrNull())
    }

    @Test
    fun `非法 JSON 反序列化返回 null`() {
        assertNull("""{"code":}""".toObjectOrNull<Payload>())
        assertNull("".toObjectOrNull<Payload>())
        assertNull("   ".toObjectOrNull<Payload>())
    }

    @Test
    fun `反序列化到列表`() {
        val list = """[{"code":1,"name":"a"},{"code":2,"name":"b"}]""".toListOrNull<Payload>()
        assertEquals(2, list?.size)
        assertEquals("b", list?.get(1)?.name)
    }

    @Test
    fun `类型不匹配的列表返回 null 而不是空列表`() {
        assertNull("""{"code":1}""".toListOrNull<Payload>())
    }

    @Test
    fun `toJsonObjectOrNull 对数组返回 null`() {
        assertNull("""[1,2]""".toJsonObjectOrNull())
        assertEquals(2, """{"a":1,"b":2}""".toJsonObjectOrNull()?.size())
    }

    @Test
    fun `toJsonArrayOrNull 对对象返回 null`() {
        assertNull("""{"a":1}""".toJsonArrayOrNull())
        assertEquals(2, """[1,2]""".toJsonArrayOrNull()?.size())
    }

    @Test
    fun `默认配置不转义 HTML 字符`() {
        val json = """{"url":"https://example.com?a=1&b=2"}""".toJsonObjectOrNull()
        val text = json.toJsonOrEmpty()
        assertTrue(text.contains("&"))
        assertTrue(!text.contains("\\u0026"))
    }

    @Test
    fun `JsonElement 转对象`() {
        val element = """{"code":1,"name":"a"}""".toJsonObjectOrNull()
        assertEquals(1, element.toObjectOrNull<Payload>()?.code)
        assertNull("""{"code":"x"}""".toJsonObjectOrNull().toObjectOrNull<Payload>())
    }
}
