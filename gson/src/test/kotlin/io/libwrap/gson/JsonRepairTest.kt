package io.libwrap.gson

import io.libwrap.gson.JsonRepair.Fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [JsonRepair] 的行为约定：能修的必须修好且结果可被标准解析器接受，不能修的必须明确返回 null，
 * 不能把"修坏了"的文本交给调用方。
 */
class JsonRepairTest {

    // ==================== 合法输入 ====================

    @Test
    fun `合法 JSON 原样返回且 applied 为空`() {
        val raw = """{"code":200,"data":{"name":"张三"}}"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals(raw, result!!.text)
        assertTrue(result.isUnmodified)
    }

    @Test
    fun `顶层是数组的合法文本同样原样返回`() {
        val raw = """[1,2,{"a":true}]"""
        assertEquals(raw, JsonRepair.repair(raw)?.text)
    }

    // ==================== 非法转义 ====================

    @Test
    fun `修复 Windows 路径里的非法反斜杠转义`() {
        val raw = """{"path":"C:\Users\name"}"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals(listOf(Fix.ESCAPES), result!!.applied)
        assertTrue(GsonKit.gson.fromJson(result.text, Map::class.java).isNotEmpty())
    }

    @Test
    fun `残缺的 unicode 转义按普通反斜杠处理`() {
        val raw = """{"a":"\u12"}"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals("""{"a":"\\u12"}""", result!!.text)
    }

    @Test
    fun `合法转义序列不被改动`() {
        assertNull(JsonRepair.escapeInvalidSequences("""{"a":"line\nbreak\t\"q\"\\"}"""))
    }

    @Test
    fun `无需修复时 escapeInvalidSequences 返回 null`() {
        assertNull(JsonRepair.escapeInvalidSequences("""{"a":"plain"}"""))
    }

    // ==================== 未转义引号 ====================

    @Test
    fun `修复字符串值里的一组未转义引号`() {
        val raw = """{"desc":"他说"你好"就走了","n":1}"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals(listOf(Fix.UNESCAPED_QUOTES), result!!.applied)
        val parsed = GsonKit.gson.fromJson(result.text, Map::class.java)
        assertEquals("他说\"你好\"就走了", parsed["desc"])
    }

    @Test
    fun `修复连续多个未转义引号`() {
        val raw = """{"a":"x"y"z","b":1}"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals("""{"a":"x\"y\"z","b":1}""", result!!.text)
    }

    @Test
    fun `字段名后的冒号不会被误判为字符串内容`() {
        val raw = """{"a":"v","b":"w"}"""
        assertTrue(JsonRepair.repair(raw)!!.isUnmodified)
    }

    // ==================== 尾部截断 ====================

    @Test
    fun `截断在字符串中间时补齐引号与括号`() {
        val raw = """{"a":"hello"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals("""{"a":"hello"}""", result!!.text)
    }

    @Test
    fun `截断在数组中间时补齐方括号`() {
        val raw = """{"a":[1,2"""
        assertEquals("""{"a":[1,2]}""", JsonRepair.repair(raw)?.text)
    }

    @Test
    fun `截断在悬空的冒号后面时丢弃该成员`() {
        val raw = """{"a":1,"b":"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals("""{"a":1}""", result!!.text)
    }

    @Test
    fun `截断在未闭合的键名中间时丢弃该成员`() {
        val raw = """{"a":1,"b"""
        assertEquals("""{"a":1}""", JsonRepair.repair(raw)?.text)
    }

    @Test
    fun `截断在尾随逗号后面时丢弃逗号`() {
        val raw = """{"a":1,"""
        assertEquals("""{"a":1}""", JsonRepair.repair(raw)?.text)
    }

    @Test
    fun `深层嵌套截断时补齐每一层闭合符号`() {
        val raw = """{"a":{"b":{"c":[1,{"d":2"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals("""{"a":{"b":{"c":[1,{"d":2}]}}}""", result!!.text)
    }

    @Test
    fun `已合法的文本不会被 closeTruncated 改动`() {
        assertNull(JsonRepair.closeTruncated("""{"a":1}"""))
        assertNull(JsonRepair.closeTruncated("""[1,2,3]"""))
    }

    // ==================== 组合损伤 ====================

    @Test
    fun `同时存在非法转义与未转义引号时按组合策略修复`() {
        val raw = """{"t":"a\zb"c","n":1}"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals(listOf(Fix.ESCAPES, Fix.UNESCAPED_QUOTES), result!!.applied)
    }

    @Test
    fun `三类损伤同时出现时按三策略组合修复`() {
        val raw = """{"t":"a\zb"c","arr":[1,2"""
        val result = JsonRepair.repair(raw)
        assertNotNull(result)
        assertEquals(listOf(Fix.ESCAPES, Fix.UNESCAPED_QUOTES, Fix.TRUNCATION), result!!.applied)
        assertNotNull(GsonKit.gson.fromJson(result.text, Map::class.java))
    }

    // ==================== 无法修复 ====================

    @Test
    fun `完全不是 JSON 的文本返回 null`() {
        assertNull(JsonRepair.repair("hello world"))
    }

    @Test
    fun `空文本返回 null`() {
        assertNull(JsonRepair.repair(""))
        assertNull(JsonRepair.repair("   "))
    }

    @Test
    fun `结构合法但值非法的文本不会被强行修复`() {
        // 缺少值的对象字面量不属于三类损伤，修复器不应臆造数据
        assertNull(JsonRepair.repair("""{"a":,"b":1}"""))
    }

    @Test
    fun `策略链不会把本身合法的文本改坏`() {
        val samples = listOf(
            """{"a":null}""",
            """{"a":-1.5e10}""",
            """{"a":"\"引号\""}""",
            """[{"a":[]},{"b":{}}]""",
        )
        samples.forEach { raw ->
            val result = JsonRepair.repair(raw)
            assertNotNull("期望原样通过：$raw", result)
            assertEquals(raw, result!!.text)
        }
    }
}
