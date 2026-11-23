package io.libwrap.gson

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [JsonValues] 的取值约定：字段缺失、值为 null、类型不符一律回退默认值，任何输入都不抛异常。
 */
class JsonValuesTest {

    private fun obj(text: String) = JsonParser.parseString(text).asJsonObject

    @Test
    fun `stringOr 字段缺失与 null 均回退默认值`() {
        val json = obj("""{"absent":null}""")
        assertEquals("fallback", json.stringOr("missing", "fallback"))
        assertEquals("fallback", json.stringOr("absent", "fallback"))
    }

    @Test
    fun `stringOr 数字与布尔取其字面量文本`() {
        val json = obj("""{"n":42,"b":true}""")
        assertEquals("42", json.stringOr("n"))
        assertEquals("true", json.stringOr("b"))
    }

    @Test
    fun `stringOr 对象与数组返回其 JSON 文本`() {
        val json = obj("""{"o":{"a":1},"arr":[1,2]}""")
        assertEquals("""{"a":1}""", json.stringOr("o"))
        assertEquals("[1,2]", json.stringOr("arr"))
    }

    @Test
    fun `stringOrNull 对 null 值返回 null 而不是空串`() {
        val json = obj("""{"a":null,"b":"x"}""")
        assertNull(json.stringOrNull("a"))
        assertNull(json.stringOrNull("missing"))
        assertEquals("x", json.stringOrNull("b"))
    }

    @Test
    fun `intOr 正常值与默认值`() {
        val json = obj("""{"k":7}""")
        assertEquals(7, json.intOr("k"))
        assertEquals(-1, json.intOr("missing", -1))
    }

    @Test
    fun `intOr 遇到非数字字符串回退默认值而不抛异常`() {
        assertEquals(0, obj("""{"k":"abc"}""").intOr("k"))
    }

    @Test
    fun `intOr 遇到 null 与对象回退默认值`() {
        val json = obj("""{"a":null,"b":{"c":1}}""")
        assertEquals(5, json.intOr("a", 5))
        assertEquals(5, json.intOr("b", 5))
    }

    @Test
    fun `longOr 保留超出 int 范围的精度`() {
        val json = obj("""{"k":9007199254740993}""")
        assertEquals(9007199254740993L, json.longOr("k"))
        assertEquals(99L, json.longOr("missing", 99L))
    }

    @Test
    fun `doubleOr 与 floatOr 支持小数与科学计数法`() {
        val json = obj("""{"a":1.5,"b":-2.5e3}""")
        assertEquals(1.5, json.doubleOr("a"), 0.0)
        assertEquals(-2500.0, json.doubleOr("b"), 0.0)
        assertEquals(1.5f, json.floatOr("a"), 0f)
    }

    @Test
    fun `booleanOr 支持布尔与字符串形式`() {
        val json = obj("""{"t":true,"f":false,"s":"true"}""")
        assertTrue(json.booleanOr("t"))
        assertEquals(false, json.booleanOr("f", true))
        assertTrue(json.booleanOr("s"))
        assertEquals(true, json.booleanOr("missing", true))
    }

    @Test
    fun `objectOrNull 与 arrayOrNull 严格校验类型`() {
        val json = obj("""{"o":{"a":1},"arr":[1],"s":"x"}""")
        assertEquals(1, json.objectOrNull("o")?.intOr("a"))
        assertEquals(1, json.arrayOrNull("arr")?.size())
        assertNull(json.objectOrNull("arr"))
        assertNull(json.arrayOrNull("o"))
        assertNull(json.objectOrNull("s"))
    }

    @Test
    fun `elementOrNull 区分字段缺失与显式 null`() {
        val json = obj("""{"a":null}""")
        assertNull(json.elementOrNull("missing"))
        assertEquals(true, json.elementOrNull("a")?.isJsonNull)
    }

    @Test
    fun `JsonElement 的空安全取值`() {
        val json = obj("""{"a":"12","b":null}""")
        assertEquals("12", json.elementOrNull("a").textOrNull())
        assertEquals(12, json.elementOrNull("a").intOrNull())
        assertEquals(12L, json.elementOrNull("a").longOrNull())
        assertNull(json.elementOrNull("b").textOrNull())
        assertNull(json.elementOrNull("missing").intOrNull())
    }
}
