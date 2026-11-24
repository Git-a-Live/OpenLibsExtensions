package io.libwrap.coil

import android.graphics.Bitmap
import android.os.Build
import kotlin.math.max

/**
 * [Bitmap] 级模糊门面，内部委托给 [StackBlur]。
 *
 * 关键点是**先降采样再模糊**：模糊本身是低通滤波，把长边压到原图边长的 1/[sampling] 后
 * 再按同比例缩小半径，视觉结果几乎一致，像素处理量却降到 1/sampling²。
 * 以 1080×1080 的封面、radius=24、sampling=4 为例，参与运算的像素从 117 万降到 7.3 万。
 */
public object BitmapBlur {

    /**
     * 对 [source] 做模糊，返回新的位图；[radius] 非正时原样返回入参。
     *
     * @param radius 模糊半径，单位为位图像素
     * @param sampling 降采样倍数，1 表示不降采样；建议 2~8，取值过大时边缘会出现块状
     */
    @JvmOverloads
    public fun blur(source: Bitmap, radius: Int, sampling: Int = 4): Bitmap {
        if (radius <= 0) return source

        val factor = sampling.coerceAtLeast(1)
        val readable = if (source.isHardwareBitmap()) {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: return source
        } else {
            source
        }

        val targetWidth = max(1, readable.width / factor)
        val targetHeight = max(1, readable.height / factor)
        val working = if (targetWidth != readable.width || targetHeight != readable.height) {
            Bitmap.createScaledBitmap(readable, targetWidth, targetHeight, true)
        } else {
            readable
        }

        val pixels = IntArray(working.width * working.height)
        working.getPixels(pixels, 0, working.width, 0, 0, working.width, working.height)
        val result = StackBlur.blur(
            pixels,
            working.width,
            working.height,
            max(1, radius / factor),
        )

        val blurred = Bitmap.createBitmap(working.width, working.height, Bitmap.Config.ARGB_8888)
        blurred.setPixels(result, 0, working.width, 0, 0, working.width, working.height)

        // 中间位图用完即释放；source 由调用方持有，任何情况下都不回收
        if (working !== readable) working.recycle()
        if (readable !== source) readable.recycle()

        if (blurred.width == source.width && blurred.height == source.height) {
            return blurred
        }
        val restored = Bitmap.createScaledBitmap(blurred, source.width, source.height, true)
        blurred.recycle()
        return restored
    }

    /** `Bitmap.Config.HARDWARE` 在 API 26 才出现，低版本不能直接引用该常量。 */
    private fun Bitmap.isHardwareBitmap(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE
}
