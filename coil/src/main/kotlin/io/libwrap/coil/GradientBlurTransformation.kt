package io.libwrap.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.roundToInt

/**
 * Coil 的模糊变换：按 [BlurBand] 描述在图片顶部和/或底部做渐变模糊，可选叠加渐变色。
 *
 * 组合顺序刻意设计为"先模糊 → 再叠色 → 最后统一裁切"：
 * 叠色发生在遮罩之前，模糊与颜色共用同一条渐变边界，因此两者会一起淡出，
 * 不会出现"颜色在清晰区边缘突然截断"的接缝。
 *
 * 模糊由 CPU 完成（[BitmapBlur]），适用于没有 RenderEffect 的低版本或需要
 * 一次性产出静态位图的场景（例如分享卡片、通知大图）。视图上实时渲染应优先使用
 * [GradientBlurEffect]。
 */
public class GradientBlurTransformation @JvmOverloads constructor(
    private val blurRatio: Float = 0.02f,
    private val top: BlurBand? = BlurBand(),
    private val bottom: BlurBand? = null,
    private val sampling: Int = 4,
) : Transformation() {

    override val cacheKey: String = buildString {
        append(GradientBlurTransformation::class.java.name)
        append(":blur=").append(blurRatio)
        append(":sampling=").append(sampling)
        append(":top=").append(top?.cacheToken() ?: "none")
        append(":bottom=").append(bottom?.cacheToken() ?: "none")
    }

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        if (width <= 0 || height <= 0) return input
        if (top == null && bottom == null) return input

        // 半径按图片高度取比例，与 GPU 路径的口径一致
        val radius = (blurRatio * height).roundToInt().coerceAtLeast(1)
        val blurred = BitmapBlur.blur(input, radius, sampling)
        val layer = blurred.copy(Bitmap.Config.ARGB_8888, true) ?: return input
        val layerCanvas = Canvas(layer)

        drawTint(layerCanvas, width, height)
        val mask = buildMask(width, height)
        layerCanvas.drawBitmap(
            mask,
            0f,
            0f,
            Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) },
        )
        mask.recycle()

        val output = input.copy(Bitmap.Config.ARGB_8888, true) ?: return input
        Canvas(output).drawBitmap(layer, 0f, 0f, null)

        // 中间位图用完即释放；input 由 Coil 持有，不能回收
        if (blurred !== input) blurred.recycle()
        layer.recycle()
        return output
    }

    /**
     * 模糊的可见区域遮罩：模糊区不透明白色，清晰区全透明，两者之间是线性过渡带。
     * 只有 alpha 参与运算，颜色值取白或黑都不影响 `DST_IN` 结果。
     */
    private fun buildMask(width: Int, height: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        top?.let { band ->
            val regionEnd = (height * band.coverage).coerceIn(0f, height.toFloat())
            val fadeStart = (regionEnd - height * band.fade).coerceAtLeast(0f)
            paint.color = Color.WHITE
            paint.shader = if (fadeStart < regionEnd) {
                LinearGradient(
                    0f, fadeStart, 0f, regionEnd,
                    Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
            } else {
                null
            }
            canvas.drawRect(0f, 0f, width.toFloat(), regionEnd, paint)
            paint.shader = null
        }

        bottom?.let { band ->
            val regionStart = (height * (1f - band.coverage)).coerceIn(0f, height.toFloat())
            val fadeEnd = (regionStart + height * band.fade).coerceAtMost(height.toFloat())
            paint.color = Color.WHITE
            paint.shader = if (regionStart < fadeEnd) {
                LinearGradient(
                    0f, regionStart, 0f, fadeEnd,
                    Color.TRANSPARENT, Color.WHITE, Shader.TileMode.CLAMP,
                )
            } else {
                null
            }
            canvas.drawRect(0f, regionStart, width.toFloat(), height.toFloat(), paint)
            paint.shader = null
        }

        return mask
    }

    /** 在模糊层上叠加颜色；渐变方向与遮罩一致，随后会被同一遮罩裁切。 */
    private fun drawTint(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        top?.takeIf { it.hasTint }?.let { band ->
            val regionEnd = (height * band.coverage).coerceIn(0f, height.toFloat())
            val fadeEnd = regionEnd.coerceAtLeast(1f)
            paint.shader = LinearGradient(
                0f, 0f, 0f, fadeEnd,
                band.tintStart, band.tintEnd, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, width.toFloat(), regionEnd, paint)
        }
        bottom?.takeIf { it.hasTint }?.let { band ->
            val regionStart = (height * (1f - band.coverage)).coerceIn(0f, height.toFloat())
            val span = (height - regionStart).coerceAtLeast(1f)
            paint.shader = LinearGradient(
                0f, regionStart, 0f, regionStart + span,
                band.tintStart, band.tintEnd, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, regionStart, width.toFloat(), height.toFloat(), paint)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GradientBlurTransformation) return false
        return blurRatio == other.blurRatio &&
            sampling == other.sampling &&
            top == other.top &&
            bottom == other.bottom
    }

    override fun hashCode(): Int {
        var result = blurRatio.hashCode()
        result = 31 * result + sampling
        result = 31 * result + (top?.hashCode() ?: 0)
        result = 31 * result + (bottom?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = cacheKey
}
