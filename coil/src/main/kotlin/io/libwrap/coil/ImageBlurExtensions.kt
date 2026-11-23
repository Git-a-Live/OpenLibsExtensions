package io.libwrap.coil

import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil3.load
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import coil3.request.transformations

/**
 * 渐变模糊的一组参数。
 *
 * [blurRatio] 是模糊半径占图片高度的比例而不是绝对像素值：同一份配置用在 200px 的缩略图
 * 和 1080px 的大图上，视觉强度一致，也保证 CPU 与 GPU 两条路径取到同一个半径口径。
 */
public class GradientBlurOptions @JvmOverloads constructor(
    /** 模糊半径占高度的比例，建议 0.01~0.05。 */
    public val blurRatio: Float = 0.02f,
    /** 顶部模糊带，null 表示顶部不模糊。 */
    public val top: BlurBand? = BlurBand(),
    /** 底部模糊带，null 表示底部不模糊。 */
    public val bottom: BlurBand? = null,
    /** CPU 路径的降采样倍数，仅 [preferGpu] 为 false 或系统版本不足时生效。 */
    public val sampling: Int = 4,
    /** 圆角半径（px），大于 0 时用 outline 裁剪，避免 RenderEffect 丢失圆角。 */
    public val cornerRadiusPx: Float = 0f,
    /** 是否优先走 GPU 路径；仅在 API 33 及以上生效，低版本自动回退 CPU。 */
    public val preferGpu: Boolean = true,
) {

    init {
        require(blurRatio > 0f) { "blurRatio must be positive, but was $blurRatio" }
        require(sampling >= 1) { "sampling must be >= 1, but was $sampling" }
        require(top != null || bottom != null) { "at least one of top/bottom must be set" }
    }
}

/**
 * 加载图片并施加渐变模糊。
 *
 * API 33 及以上默认走 GPU（[GradientBlurEffect]，零位图拷贝），低版本走 CPU
 * （[GradientBlurTransformation]）。两条路径共享同一套 [BlurBand] 几何参数，视觉结果一致。
 *
 * @param skipIfUnchanged 同一 [ImageView] 上数据未变化时跳过本次加载，避免列表因状态刷新
 *   整行重绑时封面"闪一下"
 */
@JvmOverloads
public fun ImageView.loadWithGradientBlur(
    data: Any?,
    options: GradientBlurOptions = GradientBlurOptions(),
    @DrawableRes placeholderRes: Int = 0,
    @DrawableRes errorRes: Int = placeholderRes,
    skipIfUnchanged: Boolean = false,
    builder: ImageRequest.Builder.() -> Unit = {},
) {
    if (skipIfUnchanged && !markDataLoaded(data)) return

    val useGpu = options.preferGpu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    load(data) {
        if (placeholderRes != 0) placeholder(placeholderRes)
        if (errorRes != 0) error(errorRes)
        builder()
        if (!useGpu) {
            transformations(
                GradientBlurTransformation(
                    blurRatio = options.blurRatio,
                    top = options.top,
                    bottom = options.bottom,
                    sampling = options.sampling,
                ),
            )
        }
    }
    if (!useGpu) return

    if (options.cornerRadiusPx > 0f) clipToRoundCorners(options.cornerRadiusPx)
    applyGradientBlurEffect(options)
}

/**
 * 释放当前视图上的模糊效果占用的 GPU 中间缓冲。
 *
 * RenderEffect 的中间缓冲按视图尺寸分配、依赖 finalizer 释放，列表快速滚动时会累积，
 * 建议在 `onViewRecycled` 或视图变为 GONE 时调用。效果实例与已编译的 shader 会保留，
 * 重新可见时调用 [restoreGradientBlur] 即可恢复，不必重新编译。
 */
public fun ImageView.releaseGradientBlur() {
    (getTag(R.id.libwrap_gradient_blur_effect) as? GradientBlurEffect)?.release(this)
}

/** 视图重新可见但未重新加载图片时，恢复此前挂载的模糊效果。 */
public fun ImageView.restoreGradientBlur() {
    (getTag(R.id.libwrap_gradient_blur_effect) as? GradientBlurEffect)?.attach(this)
}

private fun ImageView.applyGradientBlurEffect(options: GradientBlurOptions) {
    val existing = getTag(R.id.libwrap_gradient_blur_effect) as? GradientBlurEffect
    if (existing != null && existing.sameParams(options.blurRatio, options.top, options.bottom)) {
        // 参数未变：attach 幂等，重复调用不会重复注册监听
        existing.attach(this)
        return
    }
    existing?.detach(this)
    val effect = GradientBlurEffect(options.blurRatio, options.top, options.bottom)
    setTag(R.id.libwrap_gradient_blur_effect, effect)
    effect.attach(this)
}

/** 圆角用 outline 裁剪：与 RenderEffect 同属 RenderNode 层级，组合使用不会互相抵消。 */
private fun ImageView.clipToRoundCorners(radiusPx: Float) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
        }
    }
    clipToOutline = true
}

/** 记录本次要加载的数据标识，返回是否需要加载。 */
private fun ImageView.markDataLoaded(data: Any?): Boolean {
    val key = data?.toString().orEmpty()
    if (getTag(R.id.libwrap_loaded_data_key) == key) return false
    setTag(R.id.libwrap_loaded_data_key, key)
    return true
}
