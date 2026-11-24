# lottie: shared composition cache

English | [中文](lottie.md)

Module `:lottie`, depends on Lottie.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

```kotlin
// Do not set app:lottie_rawRes or app:lottie_autoPlay in the layout
override fun onBindViewHolder(holder: Holder, position: Int) {
    LottieCache.play(holder.lottie, R.raw.spinner)
}

override fun onViewRecycled(holder: Holder) {
    LottieCache.release(holder.lottie)
}
```

- Compositions are parsed once per process, keyed by `resource id + night mode`. The night mode is part of the key because resources with the same name under `raw/` and `raw-night/` share one `resId`; keying on the id alone would resolve to the composition of the other mode after a light/dark switch.
- Parsing runs asynchronously; views registered meanwhile are attached together once parsing completes, so the main thread is never blocked.
- Rendering follows the composition's own frame rate (`setUseCompositionFrameRate`), so 30 fps assets do not update twice per frame on a 60 Hz display.
- `LottieCache.clear()` can be called from `onTrimMemory(TRIM_MEMORY_COMPLETE)`.

---

[Back to the root README](../../README_EN.md)
