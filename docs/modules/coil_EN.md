# coil: gradient blur

English | [中文](coil.md)

Module `:coil`, depends on Coil 3.x.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

### Usage

```kotlin
imageView.loadWithGradientBlur(
    data = coverUrl,
    options = GradientBlurOptions(
        blurRatio = 0.035f,                              // blur radius as a fraction of image height
        top = BlurBand(coverage = 0.45f, fade = 0.18f),  // top 45% blurred, 18% fade band
        bottom = BlurBand(
            coverage = 0.55f,
            fade = 0.25f,
            tintStart = 0x00FFFFFF,
            tintEnd = 0xCC1B2A4A.toInt(),                // gradient overlay
        ),
        cornerRadiusPx = 28f,
    ),
)
```

In a list, release the GPU buffer on recycle and restore it when the view becomes visible again without reloading the image:

```kotlin
override fun onViewRecycled(holder: Holder) {
    holder.image.releaseGradientBlur()
}

imageView.restoreGradientBlur()
```

### API

| Call | Description |
|---|---|
| `ImageView.loadWithGradientBlur(data, options, ...)` | Loads and applies the gradient blur, choosing GPU or CPU automatically |
| `ImageView.releaseGradientBlur()` / `restoreGradientBlur()` | Releases the GPU buffer / reattaches the effect |
| `BitmapBlur.blur(bitmap, radius, sampling)` | Bitmap blur, returns a new bitmap |
| `StackBlur.blur(pixels, width, height, radius)` | Pixel-level algorithm with no `android.graphics` dependency, unit-testable |
| `GradientBlurTransformation(...)` | Coil `Transformation`, usable with `AsyncImage` |

### Two paths

| Path | Implementation | Requirement | Use |
|---|---|---|---|
| GPU | `GradientBlurEffect` (AGSL + `RenderEffect`) | API 33+ | Real-time rendering such as lists and player backgrounds |
| CPU | `GradientBlurTransformation` (Coil `Transformation`) | none | Fallback on older levels, static bitmap export |

With `GradientBlurOptions.preferGpu = true` (the default), API 33+ uses the GPU path and older levels fall back to CPU without branching at the call site.

### Implementation notes

- The CPU path is a separable triangle-kernel Stack Blur with `O(1)` cost per pixel. The bitmap is downsampled by `sampling` before blurring and scaled back afterwards: a 1080x1080 cover processed with `sampling = 4` drops from 1.17 million pixels to 73 thousand.
- The GPU path places 16 sample points on a golden-angle spiral inside a disc, weighted by a radial Gaussian; compared with multi-layer grid sampling this lowers the sample count while texture linear filtering multiplies the effective sampling.
- Composition order is blur, then tint, then a single clip. The tint is applied before the mask so blur and colour share one gradient boundary and fade out together, leaving no colour seam at the edge of the sharp region.
- `RenderEffect` intermediate buffers are sized per view and released by finalizers, accumulating during fast scrolling, hence the explicit `release` / `restore`. Instances are cached on the view tag by parameter set, so an unchanged configuration reuses the compiled shader.

### Limitations

- The GPU path has not been compared pixel by pixel on a device; it is verified at compile time and by logic only. After integration, compare `preferGpu = true` with `false`.
- `RuntimeShader` compiles at runtime and some GPU drivers reject complex shaders. Where determinism matters, fall back to the CPU path per device.

## FAQ

### How is this different from writing a Coil `BlurTransformation`

Most implementations blur the whole image. This module targets requirements that carry a geometric description, such as blurring only the top region while keeping a gradient transition into the sharp area, and keeps the radius definition identical across the GPU and CPU paths.

### Do the GPU and CPU paths produce identical pixels

No, and pixel-level equality is not the goal. The GPU path uses 16-point spiral sampling while the CPU path uses a separable triangle kernel, so the kernels differ. What they share is the geometric description (band ratios, fade position, tint direction), which is why they look the same. Where reproducible pixels are required, use the CPU path exclusively.

### Does the image disappear after `releaseGradientBlur()`

No. The method only releases the GPU intermediate buffer; once the `RenderEffect` is removed the view returns to its uneffected state and the bitmap remains. Call `restoreGradientBlur()` when the view becomes visible again; no re-decode is needed.

### How is minSdk 21 reconciled with the API 33 requirement

With `preferGpu = true`, older levels use `GradientBlurTransformation` on the CPU with no interface change. To force a single path, set `preferGpu = false`.

---

[Back to the root README](../../README_EN.md)
