package io.libwrap.moshi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 无类型树模型的安全取值。 */
class MoshiValuesTest {

    private val map: Map<String, Any?> = mapOf(
        "name" to "x",
        "count" to 3.0,
        "big" to 9007199254740993.0,
        "ratio" to 0.25,
        "flag" to true,
        "flagText" to "TRUE",
        "nested" to mapOf("k" to "v"),
        "list" to listOf(1.0, 2.0),
        "explicitNull" to null,
        "number" to 42,
        "numericText" to "7",
    )

    @Test
    fun `字符串取值`() {
        assertEquals("x", map.stringOr("name"))
        assertEquals("absent", map.stringOr("absent", "absent"))
        assertEquals("", map.stringOr("absent"))
        assertNull(map.stringOrNull("absent"))
        assertNull(map.stringOrNull("nested"))
        assertEquals("x", map.stringOrNull("name"))
    }

    @Test
    fun `数值取值兼容 Double Int 与字符串`() {
        assertEquals(3, map.intOr("count"))
        assertEquals(42, map.intOr("number"))
        assertEquals(7, map.intOr("numericText"))
        assertEquals(0, map.intOr("absent"))
        assertEquals(0.25, map.doubleOr("ratio"), 1e-9)
        assertEquals(0.25f, map.floatOr("ratio"), 1e-6f)
        assertEquals(3L, map.longOr("count"))
    }

    @Test
    fun `布尔取值兼容字符串`() {
        assertTrue(map.booleanOr("flag"))
        assertTrue(map.booleanOr("flagText"))
        assertFalse(map.booleanOr("absent"))
        assertTrue(map.booleanOr("absent", true))
        assertFalse(map.booleanOr("name"))
    }

    @Test
    fun `结构取值`() {
        assertEquals("v", map.mapOrNull("nested")?.stringOr("k"))
        assertEquals(2, map.listOrNull("list")?.size)
        assertNull(map.mapOrNull("list"))
        assertNull(map.listOrNull("nested"))
    }

    @Test
    fun `缺失与显式 null 均视为空`() {
        assertTrue(map.isNullOrAbsent("absent"))
        assertTrue(map.isNullOrAbsent("explicitNull"))
        assertFalse(map.isNullOrAbsent("count"))
    }
}
