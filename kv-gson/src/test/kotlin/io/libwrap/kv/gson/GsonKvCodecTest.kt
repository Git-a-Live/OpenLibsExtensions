package io.libwrap.kv.gson

import kotlin.reflect.typeOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 编解码器层面的验证：普通类、泛型集合、嵌套泛型都能往返。
 *
 * 不经过存储层，因为这里要验证的是"类型信息能否正确传给 Gson"，
 * 存储分发已在 `:kv` 模块覆盖。
 */
class GsonKvCodecTest {

    private val codec = GsonKvCodec()

    /** 刻意不标注 @Serializable：这正是本模块存在的理由。 */
    data class Plain(val id: Int, val name: String)

    @Test
    fun `普通类可往返`() {
        val source = Plain(1, "a")
        val text = codec.encode(source, typeOf<Plain>())

        assertEquals("""{"id":1,"name":"a"}""", text)
        assertEquals(source, codec.decode<Plain>(text!!, typeOf<Plain>()))
    }

    @Test
    fun `泛型集合可往返`() {
        val source = listOf(Plain(1, "a"), Plain(2, "b"))
        val text = codec.encode(source, typeOf<List<Plain>>())

        assertEquals(source, codec.decode<List<Plain>>(text!!, typeOf<List<Plain>>()))
    }

    @Test
    fun `嵌套泛型可往返`() {
        val source = mapOf("k" to listOf(Plain(1, "a")))
        val text = codec.encode(source, typeOf<Map<String, List<Plain>>>())

        assertEquals(source, codec.decode<Map<String, List<Plain>>>(text!!, typeOf<Map<String, List<Plain>>>()))
    }

    @Test
    fun `无法解析的文本返回 null 而不是抛异常`() {
        assertEquals(null, codec.decode<Plain>("not a json", typeOf<Plain>()))
    }
}
