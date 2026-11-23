package io.libwrap.lottie

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.RawRes
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lottie 动画的共享缓存，面向"同一份动画在列表里出现几十次"的场景。
 *
 * 直接给每个 item 的 `LottieAnimationView` 配置 `lottie_rawRes` 时，每个实例都会各自解析
 * 一份 [LottieComposition]（关键帧、路径、插值器对象），回收后也不会释放，长列表反复滚动
 * 会让这部分对象持续堆积；同时 `lottie_autoPlay` 会让 GONE 状态的 item 也在跑
 * ValueAnimator，Choreographer 每帧回调都在分配临时对象。
 *
 * 本类的做法是：以 `资源 id + 日夜模式` 为键全局只解析一次，命中后把同一份 Composition
 * 挂给所有视图；只在需要播放时播放，视图回收时显式释放引用。
 *
 * 使用约定：布局里不要再写 `app:lottie_rawRes` 与 `app:lottie_autoPlay`，
 * 改由 [play] / [stop] / [release] 控制。
 */
public object LottieCache {

    /**
     * 缓存键包含日夜模式：`raw/` 与 `raw-night/` 下同名资源共用同一个 resId，
     * 仅靠 resId 作键会在切换深浅色后命中另一模式的 Composition，导致颜色错误。
     */
    private data class CacheKey(@RawRes val resId: Int, val nightMode: Boolean)

    private val compositions = ConcurrentHashMap<CacheKey, LottieComposition>()

    /** 解析完成前登记等待播放的视图；解析是异步的，不能阻塞主线程。 */
    private val pending = ConcurrentHashMap<CacheKey, CopyOnWriteArrayList<LottieAnimationView>>()

    /**
     * 播放 [resId] 对应的动画。
     *
     * 首次调用会异步解析，解析完成后统一给期间登记的所有视图挂载并播放；
     * 解析失败时静默放弃，不抛异常——动画属于装饰，失败不应影响主流程。
     */
    public fun play(view: LottieAnimationView?, @RawRes resId: Int) {
        view ?: return
        val key = CacheKey(resId, view.context.isNightMode())
        compositions[key]?.let {
            attach(view, it)
            return
        }

        pending.getOrPut(key) { CopyOnWriteArrayList() }.add(view)
        LottieCompositionFactory.fromRawRes(view.context, resId)
            .addListener { composition ->
                compositions[key] = composition
                pending.remove(key)?.forEach { attach(it, composition) }
            }
            .addFailureListener {
                pending.remove(key)?.clear()
            }
    }

    /** 停止播放但保留 Composition 引用，适用于临时不可见。 */
    public fun stop(view: LottieAnimationView?) {
        view?.cancelAnimation()
    }

    /**
     * 释放视图对 Composition 的引用，供 `onViewRecycled` / `onDetachedFromWindow` 调用。
     * 共享缓存不受影响，其他视图仍在用它。
     */
    public fun release(view: LottieAnimationView?) {
        view ?: return
        view.cancelAnimation()
        (view.drawable as? LottieDrawable)?.clearComposition()
    }

    /** 清空共享缓存，建议在 `onTrimMemory(TRIM_MEMORY_COMPLETE)` 时调用。 */
    public fun clear() {
        compositions.clear()
        pending.clear()
    }

    private fun attach(view: LottieAnimationView, composition: LottieComposition) {
        val drawable = view.drawable as? LottieDrawable
        if (drawable?.composition !== composition) {
            view.setComposition(composition)
        }
        // 替代 XML 的 lottie_loop：列表里的播放指示器都需要循环
        view.repeatCount = LottieDrawable.INFINITE
        // 按 Composition 自身帧率渲染而非屏幕刷新率：30fps 的素材在 60Hz 屏上
        // 原本会被插值成 60 次更新，逐帧字符串拼接的分配量翻倍
        view.setUseCompositionFrameRate(true)
        if (!view.isAnimating) {
            view.playAnimation()
        }
    }

    private fun Context.isNightMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
}
