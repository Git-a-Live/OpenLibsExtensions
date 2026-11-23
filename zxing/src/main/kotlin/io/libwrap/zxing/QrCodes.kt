package io.libwrap.zxing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * 二维码输出参数。
 *
 * [errorCorrection] 默认取最高等级 H（约可容忍 30% 面积损坏），这是叠加中心图标的必要前提：
 * 中心图标会遮挡一部分码元，纠错等级不足时会出现"看着正常但扫不出来"的情况。
 */
public class QrCodeOptions @JvmOverloads constructor(
    /** 输出位图的边长（像素），二维码是正方形。 */
    public val sizePx: Int = 512,
    /** 静区宽度（码元数），0 表示贴边。多数场景建议保留 1。 */
    public val quietZoneModules: Int = 1,
    public val foreground: Int = Color.BLACK,
    public val background: Int = Color.WHITE,
    public val errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.H,
) {

    init {
        require(sizePx > 0) { "sizePx must be positive, but was $sizePx" }
        require(quietZoneModules >= 0) { "quietZoneModules must be >= 0, but was $quietZoneModules" }
    }
}

/**
 * 二维码生成。
 *
 * 相比直接调用 ZXing 的 `MultiFormatWriter`，这里额外处理两件实际会遇到的事：
 * 1. 用 `setPixels` 一次性写入像素数组，而不是逐点 `drawPoint`，大尺寸下快一个量级；
 * 2. 中心图标带一圈背景留白再绘制，避免图标与相邻码元贴边导致识别率骤降。
 */
public object QrCodes {

    /** 中心图标占整幅二维码边长的比例上限，超过 25% 时 H 级纠错也可能无法识别。 */
    private const val MAX_ICON_RATIO = 0.25f

    /**
     * 生成二维码位图。
     *
     * @param centerIcon 中心图标，null 表示不叠加
     * @param iconRatio 中心图标边长占比，取值会被钳制到 [0.1, 0.25]
     */
    @JvmOverloads
    public fun encode(
        content: String,
        options: QrCodeOptions = QrCodeOptions(),
        centerIcon: Bitmap? = null,
        iconRatio: Float = 0.18f,
    ): Bitmap {
        require(content.isNotEmpty()) { "content must not be empty" }

        val matrix = encodeMatrix(content, options)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                pixels[rowOffset + x] = if (matrix[x, y]) options.foreground else options.background
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        centerIcon?.let { drawCenterIcon(bitmap, it, iconRatio.coerceIn(0.1f, MAX_ICON_RATIO), options.background) }
        return bitmap
    }

    /** 只做编码，产出码元矩阵；与位图绘制解耦，便于在 JVM 单元测试中验证编码参数。 */
    internal fun encodeMatrix(content: String, options: QrCodeOptions): BitMatrix {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
            EncodeHintType.ERROR_CORRECTION to options.errorCorrection,
            EncodeHintType.MARGIN to options.quietZoneModules,
        )
        return MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            options.sizePx,
            options.sizePx,
            hints,
        )
    }

    private fun drawCenterIcon(bitmap: Bitmap, icon: Bitmap, ratio: Float, backgroundColor: Int) {
        val side = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val left = (bitmap.width - side) / 2f
        val top = (bitmap.height - side) / 2f
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val padding = side * 0.1f
        val corner = side * 0.22f
        paint.color = backgroundColor
        canvas.drawRoundRect(
            RectF(left - padding, top - padding, left + side + padding, top + side + padding),
            corner,
            corner,
            paint,
        )

        paint.isFilterBitmap = true
        canvas.drawBitmap(
            icon,
            null as Rect?,
            RectF(left, top, left + side, top + side),
            paint,
        )
    }
}
