package io.libwrap.lottie

import androidx.annotation.RawRes
import com.airbnb.lottie.LottieAnimationView

/** 播放动画的语法糖，等价于 `LottieCache.play(this, resId)`。 */
public fun LottieAnimationView.playCached(@RawRes resId: Int) {
    LottieCache.play(this, resId)
}

/** 释放动画引用的语法糖，等价于 `LottieCache.release(this)`。 */
public fun LottieAnimationView.releaseCached() {
    LottieCache.release(this)
}
