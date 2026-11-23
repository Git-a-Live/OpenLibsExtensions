package io.libwrap.coil

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [StackBlur] 的正确性验证。
 *
 * 两遍可分离实现在中间结果上做了整数截断，结果与"单遍二维三角核"存在 1~2 级的舍入差异，
 * 因此用暴力参考实现对比时采用容差断言；而均匀色、alpha 保持这类不变量必须是精确相等。
 */
class StackBlurTest {

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun channel(pixel: Int, shift: Int): Int = (pixel ushr shift) and 0xFF

    /** 单遍二维三角核参考实现，只用于小尺寸对比。 */
    private fun reference(src: IntArray, width: Int, height: Int, radius: Int): IntArray {
        // 单轴权重和为 (radius+1)^2，二维核的归一化因此是它的平方
        val axisSum = radius + 1
        val normalizer = axisSum * axisSum * axisSum * axisSum
        val out = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                for (dy in -radius..radius) {
                    val sy = (y + dy).coerceIn(0, height - 1)
                    for (dx in -radius..radius) {
                        val sx = (x + dx).coerceIn(0, width - 1)
                        val pixel = src[sy * width + sx]
                        val weight = (radius + 1 - abs(dx)) * (radius + 1 - abs(dy))
                        sumR += channel(pixel, 16) * weight
                        sumG += channel(pixel, 8) * weight
                        sumB += channel(pixel, 0) * weight
                    }
                }
                out[y * width + x] = argb(255, sumR / normalizer, sumG / normalizer, sumB / normalizer)
            }
        }
        return out
    }

    @Test
    fun `半径为 0 时返回内容相同的新数组`() {
        val source = intArrayOf(argb(255, 1, 2, 3), argb(255, 4, 5, 6))
        val result = StackBlur.blur(source, 2, 1, 0)
        assertArrayEquals(source, result)
        assertTrue("必须是新数组，不能与入参同一引用", source !== result)
    }

    @Test
    fun `均匀色图像模糊后颜色不变`() {
        val color = argb(255, 90, 140, 200)
        val source = IntArray(12 * 9) { color }
        val result = StackBlur.blur(source, 12, 9, 4)
        result.forEach { assertEquals(color, it) }
    }

    @Test
    fun `alpha 通道原样保留`() {
        val source = IntArray(10 * 10) { index ->
            argb(index % 256, index % 255, (index * 3) % 255, (index * 7) % 255)
        }
        val result = StackBlur.blur(source, 10, 10, 3)
        for (i in source.indices) {
            assertEquals("第 $i 个像素的 alpha 被改动", channel(source[i], 24), channel(result[i], 24))
        }
    }

    @Test
    fun `不修改入参数组`() {
        val source = IntArray(8 * 8) { argb(255, it, 255 - it, it / 2) }
        val snapshot = source.copyOf()
        StackBlur.blur(source, 8, 8, 2)
        assertArrayEquals(snapshot, source)
    }

    @Test
    fun `结果与暴力三角核参考实现一致`() {
        val width = 9
        val height = 7
        val random = Random(42)
        val source = IntArray(width * height) {
            argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }

        for (radius in 1..3) {
            val actual = StackBlur.blur(source, width, height, radius)
            val expected = reference(source, width, height, radius)
            var maxDiff = 0
            var totalDiff = 0
            for (i in actual.indices) {
                for (shift in intArrayOf(16, 8, 0)) {
                    val diff = abs(channel(actual[i], shift) - channel(expected[i], shift))
                    maxDiff = maxOf(maxDiff, diff)
                    totalDiff += diff
                }
            }
            val average = totalDiff.toDouble() / (actual.size * 3)
            assertTrue("radius=$radius 最大偏差 $maxDiff 超出可接受范围", maxDiff <= 3)
            assertTrue("radius=$radius 平均偏差 $average 偏大", average <= 1.0)
        }
    }

    @Test
    fun `强边缘会被平滑到中间值`() {
        val width = 9
        val height = 1
        val source = IntArray(width) { x -> if (x < 4) argb(255, 0, 0, 0) else argb(255, 255, 255, 255) }
        val result = StackBlur.blur(source, width, height, 2)
        val edgeR = channel(result[4], 16)
        assertTrue("边缘像素应当处于黑白之间，实际 $edgeR", edgeR in 1..254)
    }

    @Test
    fun `超过上限的半径被钳制而不是抛异常`() {
        val source = IntArray(4 * 4) { argb(255, 10, 20, 30) }
        val result = StackBlur.blur(source, 4, 4, 1000)
        result.forEach { assertEquals(argb(255, 10, 20, 30), it) }
    }

    @Test
    fun `尺寸或数组长度非法时抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            StackBlur.blur(IntArray(4), 0, 1, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StackBlur.blur(IntArray(3), 2, 2, 1)
        }
    }

    @Test
    fun `非方形图像不会被转置`() {
        val width = 6
        val height = 2
        // 上边界全红、下边界全蓝，转置会让结果颜色分布错位
        val source = IntArray(width * height) { index ->
            if (index < width) argb(255, 255, 0, 0) else argb(255, 0, 0, 255)
        }
        val result = StackBlur.blur(source, width, height, 1)
        assertTrue("首行应保留下红调", channel(result[0], 16) > channel(result[0], 0))
        assertTrue("末行应保留下蓝调", channel(result[width * height - 1], 0) > channel(result[width * height - 1], 16))
    }
}
