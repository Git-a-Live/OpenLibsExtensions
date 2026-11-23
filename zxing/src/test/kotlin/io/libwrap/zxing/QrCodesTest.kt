package io.libwrap.zxing

import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 只覆盖编码层（码元矩阵），位图绘制部分由 demo 覆盖。
 *
 * 注意 ZXing 的 `QRCodeWriter` 会把码区按整数倍缩放后居中放进请求的边长里、余量留白，
 * 所以不能直接断言 `matrix[0, 0]`；所有几何断言都基于深色像素包围盒与游程长度。
 */
class QrCodesTest {

    private fun options(size: Int = 256, quietZone: Int = 1) =
        QrCodeOptions(sizePx = size, quietZoneModules = quietZone)

    /** 返回深色像素的包围盒 [minX, minY, maxX, maxY]。 */
    private fun darkBounds(matrix: BitMatrix): IntArray {
        var minX = matrix.width
        var minY = matrix.height
        var maxX = -1
        var maxY = -1
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (!matrix[x, y]) continue
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        return intArrayOf(minX, minY, maxX, maxY)
    }

    /** 从序列首端开始游程长度编码。 */
    private fun leadingRuns(values: BooleanArray): List<Int> {
        if (values.isEmpty()) return emptyList()
        val runs = mutableListOf<Int>()
        var current = values[0]
        var count = 0
        for (value in values) {
            if (value == current) {
                count++
            } else {
                runs += count
                current = value
                count = 1
            }
        }
        runs += count
        return runs
    }

    /** 定位图案沿任意方向都是 1:1:3:1:1 的游程。 */

    @Test
    fun `输出尺寸与请求一致且为正方形`() {
        val matrix = QrCodes.encodeMatrix("https://example.com", options(size = 256))
        assertEquals(256, matrix.width)
        assertEquals(256, matrix.height)
    }

    /**
     * 定位图案的"顶边"是 7 个码元连成的实心长条，而 1:1:3:1:1 的特征只出现在过中心的那一行上。
     * 因此先用顶边长度反推码元尺寸，再取中心行验证游程。
     */
    @Test
    fun `三个角上存在标准的 7 乘 7 定位图案`() {
        val matrix = QrCodes.encodeMatrix("hello", options(size = 128, quietZone = 0))
        val bounds = darkBounds(matrix)
        val left = bounds[0]
        val top = bounds[1]
        val right = bounds[2]
        val bottom = bounds[3]

        val topEdge = leadingRuns(BooleanArray(matrix.width - left) { matrix[left + it, top] })
        val finderWidth = topEdge.first()
        assertTrue("定位图案宽度 $finderWidth 应为 7 的整数倍", finderWidth % 7 == 0)
        val moduleSize = finderWidth / 7
        assertTrue("码元尺寸计算结果异常：$moduleSize", moduleSize > 0)

        val expected = listOf(moduleSize, moduleSize, moduleSize * 3, moduleSize, moduleSize)

        val topCenterRow = leadingRuns(
            BooleanArray(matrix.width - left) { matrix[left + it, top + 3 * moduleSize] },
        ).take(5)
        assertEquals("左上角中心行游程异常：$topCenterRow", expected, topCenterRow)

        val topCenterRowReversed = leadingRuns(
            BooleanArray(right + 1) { matrix[right - it, top + 3 * moduleSize] },
        ).take(5)
        assertEquals("右上角中心行游程异常：$topCenterRowReversed", expected, topCenterRowReversed)

        val leftCenterColumnReversed = leadingRuns(
            BooleanArray(bottom + 1) { matrix[left + 3 * moduleSize, bottom - it] },
        ).take(5)
        assertEquals(
            "左下角中心列游程异常：$leftCenterColumnReversed",
            expected,
            leftCenterColumnReversed,
        )
    }

    @Test
    fun `静区越大有效码区越小`() {
        val tight = darkBounds(QrCodes.encodeMatrix("hello", options(size = 96, quietZone = 0)))
        val padded = darkBounds(QrCodes.encodeMatrix("hello", options(size = 96, quietZone = 4)))
        val tightSpan = tight[2] - tight[0] + 1
        val paddedSpan = padded[2] - padded[0] + 1
        assertTrue("静区应压缩有效码区，实际 $tightSpan vs $paddedSpan", paddedSpan < tightSpan)
    }

    @Test
    fun `支持中文内容`() {
        val matrix = QrCodes.encodeMatrix("车载二维码测试", options())
        assertEquals(256, matrix.width)
    }

    @Test
    fun `高纠错等级下长文本仍可编码`() {
        val content = "https://example.com/search?q=car&id=" + "9".repeat(64)
        val matrix = QrCodes.encodeMatrix(
            content,
            QrCodeOptions(sizePx = 320, quietZoneModules = 1, errorCorrection = ErrorCorrectionLevel.H),
        )
        assertEquals(320, matrix.width)
    }

    @Test
    fun `空内容被拒绝`() {
        assertThrows(IllegalArgumentException::class.java) {
            QrCodes.encode("")
        }
    }

    @Test
    fun `非法参数在构造时被拒绝`() {
        assertThrows(IllegalArgumentException::class.java) { QrCodeOptions(sizePx = 0) }
        assertThrows(IllegalArgumentException::class.java) { QrCodeOptions(quietZoneModules = -1) }
    }
}
