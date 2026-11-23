package io.libwrap.coil

import android.graphics.Color

/**
 * 一条模糊带：描述视图某一边有多大比例参与模糊、过渡带有多宽，以及可选的颜色叠加。
 *
 * 参数全部以"视图高度的比例"表达，与像素密度和控件尺寸无关，同一份配置可以直接用在
 * 不同尺寸的封面上。模糊强度与过渡带的形状由 [GradientBlurTransformation]（CPU）和
 * [GradientBlurEffect]（GPU）共同遵守，两条路径的视觉结果保持一致。
 */
public data class BlurBand(
    /** 模糊区占视图高度的比例，取值 (0, 1]。1 表示整张图都模糊。 */
    public val coverage: Float = 0.4f,
    /** 过渡带占视图高度的比例，取值 [0, 1]。0 表示模糊区与清晰区之间是硬边界。 */
    public val fade: Float = 0.12f,
    /** 过渡带起始侧（远离开放边）的叠加色，默认全透明即不叠加。 */
    public val tintStart: Int = Color.TRANSPARENT,
    /** 过渡带结束侧的叠加色，默认全透明即不叠加。 */
    public val tintEnd: Int = Color.TRANSPARENT,
) {

    init {
        require(coverage > 0f && coverage <= 1f) { "coverage must be in (0, 1], but was $coverage" }
        require(fade >= 0f && fade <= 1f) { "fade must be in [0, 1], but was $fade" }
    }

    /** 是否配置了颜色叠加。 */
    public val hasTint: Boolean
        get() = Color.alpha(tintStart) > 0 || Color.alpha(tintEnd) > 0

    /** 用于参与缓存键的稳定描述。 */
    internal fun cacheToken(): String =
        "$coverage,$fade,${Integer.toHexString(tintStart)},${Integer.toHexString(tintEnd)}"
}
