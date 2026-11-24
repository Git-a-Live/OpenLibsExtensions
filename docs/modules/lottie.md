# lottie：列表共享缓存

[English](lottie_EN.md) | 中文

模块 `:lottie`，依赖 Lottie。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

```kotlin
// 布局中不配置 app:lottie_rawRes 与 app:lottie_autoPlay
override fun onBindViewHolder(holder: Holder, position: Int) {
    LottieCache.play(holder.lottie, R.raw.spinner)
}

override fun onViewRecycled(holder: Holder) {
    LottieCache.release(holder.lottie)
}
```

- 以 `资源 id + 日夜模式` 为键全局只解析一次。缓存键包含日夜模式，是因为 `raw/` 与 `raw-night/` 下同名资源共用同一个 `resId`，仅按 id 缓存会在切换深浅色后命中另一模式的 Composition
- 解析异步执行，期间登记的视图在解析完成后统一挂载，不阻塞主线程
- 按 Composition 自身帧率渲染（`setUseCompositionFrameRate`），30fps 素材在 60Hz 屏幕上不做两倍更新
- `onTrimMemory(TRIM_MEMORY_COMPLETE)` 时可调用 `LottieCache.clear()`

---

[返回根目录 README](../../README.md)
