package io.libwrap.coil

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi

/**
 * 基于 AGSL 的渐变模糊，在 GPU 上完成，不产生任何位图拷贝。
 *
 * 与 [GradientBlurTransformation] 的 CPU 实现共享同一套几何描述（[BlurBand]），
 * 但采样核不同：这里用**黄金角螺旋**在圆盘内布 16 个采样点，配合径向高斯权重，
 * 再借助纹理线性过滤把等效采样数放大到约 4 倍。相比逐个像素遍历 9×9 网格，
 * 采样次数从 81 次降到 16 次，且在片元着色器里是数据相关的分支无关的常数循环。
 *
 * 需要 API 33（`RuntimeShader`）。低版本请走 [GradientBlurTransformation]。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class GradientBlurEffect(
    private val blurRatio: Float,
    private val top: BlurBand?,
    private val bottom: BlurBand?,
) {

    private val shader = RuntimeShader(SHADER_SOURCE)
    private var layoutListener: View.OnLayoutChangeListener? = null
    private var lastWidth = -1
    private var lastHeight = -1

    /** 已挂载标记，attach 幂等；release 后置回 false，允许视图重新可见时重挂。 */
    private var attached = false

    /** 已排队的挂载任务是否仍然有效，detach 时置 false 以拦截过期任务。 */
    private var attachRequested = false

    /**
     * 把效果挂到 [view] 上。
     *
     * 内部会 post 到下一帧再挂载：RecyclerView 的 onBindViewHolder 阶段视图尚未布局，
     * 此时宽高为 0，直接设置 RenderEffect 会因 uniform 未初始化而失效。
     */
    public fun attach(view: View) {
        if (attached) return
        attached = true
        attachRequested = true
        view.post {
            if (!attachRequested || !view.isAttachedToWindow) return@post
            updateUniforms(view.width, view.height)
            view.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
            val listener = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                updateUniforms(v.width, v.height)
            }
            layoutListener = listener
            view.addOnLayoutChangeListener(listener)
        }
    }

    /** 解除挂载但保留已编译的 shader，重新可见时可直接 [attach]。 */
    public fun detach(view: View) {
        attachRequested = false
        attached = false
        layoutListener?.let { view.removeOnLayoutChangeListener(it) }
        layoutListener = null
    }

    /** 解除挂载并释放 GPU 中间缓冲；shader 实例保留，可用于后续重挂。 */
    public fun release(view: View) {
        detach(view)
        view.setRenderEffect(null)
    }

    /** 参数是否与当前实例一致，用于复用已编译的 shader。 */
    public fun sameParams(blurRatio: Float, top: BlurBand?, bottom: BlurBand?): Boolean =
        this.blurRatio == blurRatio && this.top == top && this.bottom == bottom

    private fun updateUniforms(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (width == lastWidth && height == lastHeight) return
        lastWidth = width
        lastHeight = height

        shader.setFloatUniform("uSize", width.toFloat(), height.toFloat())
        // 半径按高度取比例，保证与 CPU 路径、不同尺寸控件之间的视觉一致
        shader.setFloatUniform("uRadius", (blurRatio * height).coerceAtLeast(1f))
        shader.setFloatUniform("uTopCoverage", top?.coverage ?: 0f)
        shader.setFloatUniform("uTopFade", top?.fade ?: 0f)
        shader.setFloatUniform("uBottomCoverage", bottom?.coverage ?: 0f)
        shader.setFloatUniform("uBottomFade", bottom?.fade ?: 0f)
        shader.setColorUniform("uTopTintStart", top?.tintStart ?: 0)
        shader.setColorUniform("uTopTintEnd", top?.tintEnd ?: 0)
        shader.setColorUniform("uBottomTintStart", bottom?.tintStart ?: 0)
        shader.setColorUniform("uBottomTintEnd", bottom?.tintEnd ?: 0)
    }

    private companion object {

        /**
         * 距离场按"到中心的归一化半径"计算，`2.2` 控制高斯衰减速度：
         * 取 2.2 时边缘权重约为中心的 11%，与三角核的视觉强度接近。
         */
        val SHADER_SOURCE = """
            uniform shader content;
            uniform float2 uSize;
            uniform float uRadius;
            uniform float uTopCoverage;
            uniform float uTopFade;
            uniform float uBottomCoverage;
            uniform float uBottomFade;
            layout(color) uniform half4 uTopTintStart;
            layout(color) uniform half4 uTopTintEnd;
            layout(color) uniform half4 uBottomTintStart;
            layout(color) uniform half4 uBottomTintEnd;

            float3 blurredColor(float2 position) {
                float3 accumulator = float3(0.0);
                float weightSum = 0.0;
                for (int i = 0; i < 16; i++) {
                    float sequence = float(i) + 0.5;
                    float normalized = sqrt(sequence * 0.0625);
                    float angle = sequence * 2.39996323;
                    float2 offset = float2(cos(angle), sin(angle)) * (normalized * uRadius);
                    float2 samplePosition = clamp(position + offset, float2(0.0), uSize - float2(1.0));
                    half4 color = content.eval(samplePosition);
                    float weight = exp(-2.2 * normalized * normalized) * float(color.a);
                    // premultiplied 输入还原为直通色；a 为 0 时 rgb 必为 0，用极小值兜底避免除零
                    float3 straight = float3(color.rgb) / max(float(color.a), 0.0001);
                    accumulator += straight * weight;
                    weightSum += weight;
                }
                if (weightSum <= 0.0) {
                    return float3(0.0);
                }
                return accumulator / weightSum;
            }

            half4 main(float2 fragCoord) {
                half4 source = content.eval(fragCoord);
                float height = max(uSize.y, 1.0);
                float alpha = float(source.a);
                float3 baseColor = alpha > 0.0 ? float3(unpremul(source).rgb) : float3(0.0);

                float topWeight = 0.0;
                if (uTopCoverage > 0.0) {
                    float regionEnd = uTopCoverage * height;
                    float fadeStart = max(regionEnd - uTopFade * height, 0.0);
                    topWeight = 1.0 - smoothstep(fadeStart, max(regionEnd, fadeStart + 1.0), fragCoord.y);
                }

                float bottomWeight = 0.0;
                if (uBottomCoverage > 0.0) {
                    float regionStart = (1.0 - uBottomCoverage) * height;
                    float regionFadeEnd = min(regionStart + uBottomFade * height, height);
                    bottomWeight = smoothstep(min(regionStart, regionFadeEnd - 1.0), regionFadeEnd, fragCoord.y);
                }

                float blurWeight = clamp(topWeight + bottomWeight, 0.0, 1.0);
                float3 color = baseColor;
                if (blurWeight > 0.0) {
                    color = mix(baseColor, blurredColor(fragCoord), blurWeight);
                }

                if (topWeight > 0.0) {
                    float span = max(uTopCoverage * height, 1.0);
                    float4 tint = mix(float4(uTopTintStart), float4(uTopTintEnd),
                        clamp(fragCoord.y / span, 0.0, 1.0));
                    color = mix(color, tint.rgb, tint.a * topWeight);
                }

                if (bottomWeight > 0.0) {
                    float regionStart = (1.0 - uBottomCoverage) * height;
                    float span = max(height - regionStart, 1.0);
                    float4 tint = mix(float4(uBottomTintStart), float4(uBottomTintEnd),
                        clamp((fragCoord.y - regionStart) / span, 0.0, 1.0));
                    color = mix(color, tint.rgb, tint.a * bottomWeight);
                }

                return half4(half3(color * alpha), half(alpha));
            }
        """.trimIndent()
    }
}
