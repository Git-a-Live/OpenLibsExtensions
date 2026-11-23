package io.libwrap.coil

import kotlin.math.abs

/**
 * Stack Blur：线性时间的可分离近似高斯模糊。
 *
 * 权重核为三角形 `w(d) = radius + 1 - |d|`，归一化因子为 `(radius + 1)^2`。
 * 三角核在视觉上足够接近高斯，但可以借助滑动窗口把复杂度从 `O(r^2)` 降到 `O(1)`：
 * 水平、垂直各扫一遍，每步只做常数次加减。
 *
 * 边界按"钳制取边"（clamp-to-edge）处理，越界采样取最近的边缘像素，因此不会出现暗边。
 *
 * 全流程只操作 `IntArray`，不依赖 `android.graphics`，可直接在 JVM 单元测试中验证。
 * 像素格式为 `ARGB_8888`，alpha 通道原样保留——模糊只作用于颜色，透明区域不会被"糊"进邻居。
 */
public object StackBlur {

    /** 三角核半径上限。更大的半径在实践中没有意义，且会显著放大中间数组的内存占用。 */
    public const val MAX_RADIUS: Int = 254

    /**
     * 对 [source] 做模糊，返回新数组，不修改入参。
     *
     * @param source 像素数组，长度至少为 `width * height`，格式 `ARGB_8888`
     * @param radius 模糊半径（像素），0 表示不处理，超出 [MAX_RADIUS] 会被钳制
     * @throws IllegalArgumentException 当尺寸非正或数组长度不足时
     */
    public fun blur(source: IntArray, width: Int, height: Int, radius: Int): IntArray {
        require(width > 0 && height > 0) { "width and height must be positive, got ${width}x$height" }
        require(source.size >= width * height) {
            "pixel array is smaller than width * height: ${source.size} < ${width * height}"
        }

        val normalizedRadius = radius.coerceIn(0, MAX_RADIUS)
        if (normalizedRadius == 0) return source.copyOf(width * height)

        val pixels = source.copyOf(width * height)
        val diameter = normalizedRadius * 2 + 1
        val halfWidth = (diameter + 1) / 2
        val normalizer = halfWidth * halfWidth

        val channelR = IntArray(pixels.size)
        val channelG = IntArray(pixels.size)
        val channelB = IntArray(pixels.size)

        blurRows(pixels, width, height, normalizedRadius, normalizer, channelR, channelG, channelB)
        blurColumns(pixels, width, height, normalizedRadius, normalizer, channelR, channelG, channelB)

        return pixels
    }

    private fun blurRows(
        pixels: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        normalizer: Int,
        outR: IntArray,
        outG: IntArray,
        outB: IntArray,
    ) {
        val diameter = radius * 2 + 1
        val ringR = IntArray(diameter)
        val ringG = IntArray(diameter)
        val ringB = IntArray(diameter)
        // 每列的采样位置只依赖 x，算一次复用 h 次
        val sampleX = IntArray(width)
        var rowStart = 0
        var index = 0

        for (y in 0 until height) {
            var sumInR = 0
            var sumInG = 0
            var sumInB = 0
            var sumOutR = 0
            var sumOutG = 0
            var sumOutB = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0

            for (offset in -radius..radius) {
                val sample = pixels[rowStart + offset.coerceIn(0, width - 1)]
                val slot = offset + radius
                val r = (sample ushr 16) and 0xFF
                val g = (sample ushr 8) and 0xFF
                val b = sample and 0xFF
                ringR[slot] = r
                ringG[slot] = g
                ringB[slot] = b
                val weight = radius + 1 - abs(offset)
                sumR += r * weight
                sumG += g * weight
                sumB += b * weight
                if (offset > 0) {
                    sumInR += r
                    sumInG += g
                    sumInB += b
                } else {
                    sumOutR += r
                    sumOutG += g
                    sumOutB += b
                }
            }

            var cursor = radius
            for (x in 0 until width) {
                outR[index] = sumR / normalizer
                outG[index] = sumG / normalizer
                outB[index] = sumB / normalizer

                sumR -= sumOutR
                sumG -= sumOutG
                sumB -= sumOutB

                // 即将滑出窗口的槽位，同时用来存放新进入窗口的像素
                val slot = (cursor - radius + diameter) % diameter
                sumOutR -= ringR[slot]
                sumOutG -= ringG[slot]
                sumOutB -= ringB[slot]

                if (y == 0) sampleX[x] = minOf(x + radius + 1, width - 1)
                val entering = pixels[rowStart + sampleX[x]]
                val r = (entering ushr 16) and 0xFF
                val g = (entering ushr 8) and 0xFF
                val b = entering and 0xFF
                ringR[slot] = r
                ringG[slot] = g
                ringB[slot] = b
                sumInR += r
                sumInG += g
                sumInB += b
                sumR += sumInR
                sumG += sumInG
                sumB += sumInB

                cursor = (cursor + 1) % diameter
                sumOutR += ringR[cursor]
                sumOutG += ringG[cursor]
                sumOutB += ringB[cursor]
                sumInR -= ringR[cursor]
                sumInG -= ringG[cursor]
                sumInB -= ringB[cursor]

                index++
            }
            rowStart += width
        }
    }

    private fun blurColumns(
        pixels: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        normalizer: Int,
        inR: IntArray,
        inG: IntArray,
        inB: IntArray,
    ) {
        val diameter = radius * 2 + 1
        val ringR = IntArray(diameter)
        val ringG = IntArray(diameter)
        val ringB = IntArray(diameter)
        val sampleOffset = IntArray(height)

        for (x in 0 until width) {
            var sumInR = 0
            var sumInG = 0
            var sumInB = 0
            var sumOutR = 0
            var sumOutG = 0
            var sumOutB = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0

            // 首行上方按边缘钳制，等价于从第 0 行开始逐行下移
            var sampleRow = -radius * width
            for (offset in -radius..radius) {
                val position = maxOf(0, sampleRow) + x
                val slot = offset + radius
                val r = inR[position]
                val g = inG[position]
                val b = inB[position]
                ringR[slot] = r
                ringG[slot] = g
                ringB[slot] = b
                val weight = radius + 1 - abs(offset)
                sumR += r * weight
                sumG += g * weight
                sumB += b * weight
                if (offset > 0) {
                    sumInR += r
                    sumInG += g
                    sumInB += b
                } else {
                    sumOutR += r
                    sumOutG += g
                    sumOutB += b
                }
                if (offset < height - 1) sampleRow += width
            }

            var index = x
            var cursor = radius
            for (y in 0 until height) {
                // alpha 沿用原像素，模糊只改颜色
                pixels[index] = (pixels[index] and 0xFF000000.toInt()) or
                    ((sumR / normalizer) shl 16) or
                    ((sumG / normalizer) shl 8) or
                    (sumB / normalizer)

                sumR -= sumOutR
                sumG -= sumOutG
                sumB -= sumOutB

                val slot = (cursor - radius + diameter) % diameter
                sumOutR -= ringR[slot]
                sumOutG -= ringG[slot]
                sumOutB -= ringB[slot]

                if (x == 0) sampleOffset[y] = minOf(y + radius + 1, height - 1) * width
                val position = x + sampleOffset[y]
                val r = inR[position]
                val g = inG[position]
                val b = inB[position]
                ringR[slot] = r
                ringG[slot] = g
                ringB[slot] = b
                sumInR += r
                sumInG += g
                sumInB += b
                sumR += sumInR
                sumG += sumInG
                sumB += sumInB

                cursor = (cursor + 1) % diameter
                sumOutR += ringR[cursor]
                sumOutG += ringG[cursor]
                sumOutB += ringB[cursor]
                sumInR -= ringR[cursor]
                sumInG -= ringG[cursor]
                sumInB -= ringB[cursor]

                index += width
            }
        }
    }
}
