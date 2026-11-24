# coil：渐变模糊

[English](coil_EN.md) | 中文

模块 `:coil`，依赖 Coil 3.x。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

### 用法

```kotlin
imageView.loadWithGradientBlur(
    data = coverUrl,
    options = GradientBlurOptions(
        blurRatio = 0.035f,                              // 模糊半径占图片高度的比例
        top = BlurBand(coverage = 0.45f, fade = 0.18f),  // 顶部 45% 模糊，18% 过渡带
        bottom = BlurBand(
            coverage = 0.55f,
            fade = 0.25f,
            tintStart = 0x00FFFFFF,
            tintEnd = 0xCC1B2A4A.toInt(),                // 渐变色叠加
        ),
        cornerRadiusPx = 28f,
    ),
)
```

列表场景在回收时释放 GPU 缓冲，重新可见且未重新加载图片时恢复：

```kotlin
override fun onViewRecycled(holder: Holder) {
    holder.image.releaseGradientBlur()
}

imageView.restoreGradientBlur()
```

### API

| 调用 | 说明 |
|---|---|
| `ImageView.loadWithGradientBlur(data, options, ...)` | 加载并施加渐变模糊，自动选择 GPU / CPU |
| `ImageView.releaseGradientBlur()` / `restoreGradientBlur()` | 释放 GPU 缓冲 / 重新挂载效果 |
| `BitmapBlur.blur(bitmap, radius, sampling)` | 位图模糊，返回新位图 |
| `StackBlur.blur(pixels, width, height, radius)` | 像素级算法，不依赖 `android.graphics`，可直接单测 |
| `GradientBlurTransformation(...)` | Coil `Transformation`，可用于 `AsyncImage` |

### 两条路径

| 路径 | 实现 | 要求 | 适用场景 |
|---|---|---|---|
| GPU | `GradientBlurEffect`（AGSL + `RenderEffect`） | API 33+ | 列表、播放页背景等实时渲染 |
| CPU | `GradientBlurTransformation`（Coil `Transformation`） | 无 | 低版本兜底、导出静态位图 |

`GradientBlurOptions.preferGpu = true`（默认）时，API 33+ 走 GPU，低版本回退 CPU，调用方无需分支。

### 实现说明

- CPU 侧为三角核可分离 Stack Blur，复杂度 `O(1)/像素`；按 `sampling` 降采样后模糊，再放大回原尺寸。1080×1080 的封面在 `sampling = 4` 时参与运算的像素由 117 万降至 7.3 万。
- GPU 侧以黄金角螺旋在圆盘内布置 16 个采样点，配径向高斯权重；相较多层网格采样，采样次数降低并借助纹理线性过滤放大等效采样数。
- 组合顺序为「模糊 → 叠色 → 统一裁切」。叠色发生在遮罩之前，模糊与颜色共用同一条渐变边界并一同淡出，清晰区边缘不出现颜色截断。
- `RenderEffect` 的中间缓冲按视图尺寸分配、依赖 finalizer 释放，列表快速滚动时会累积，因此提供显式 `release` / `restore`。实例按参数缓存在 view tag 上，参数未变时复用已编译的 shader。

### 限制

- GPU 路径的视觉效果未经真机逐像素比对，仅有编译期与逻辑验证；接入后应对比 `preferGpu = true / false` 两条路径。
- `RuntimeShader` 在运行时编译，个别 GPU 驱动可能拒绝复杂着色器。需要确定性时可按机型降级至 CPU 路径。

## 常见问题

### 与自研 Coil `BlurTransformation` 的差异

多数实现只做整图模糊。本模块面向「仅模糊顶部区间、并与清晰区之间保留渐变过渡」这类带几何描述的需求，同时保证 GPU 与 CPU 两条路径的半径口径一致。

### GPU 与 CPU 的像素结果是否一致

不一致，也不以逐像素一致为目标。GPU 为 16 点螺旋采样，CPU 为三角核可分离卷积，核函数不同；两者一致的是几何描述（分区比例、过渡带位置、叠色方向），因此观感一致。需要可复现的逐像素结果时固定使用 CPU 路径。

### `releaseGradientBlur()` 之后图片是否消失

不消失。该方法只卸载 GPU 中间缓冲，`RenderEffect` 移除后视图回到无效果状态，图片仍在。重新可见时调用 `restoreGradientBlur()` 恢复，无需重新解码。

### minSdk 21 与模糊所需的 API 33 之间的落差

`preferGpu = true` 时低版本自动使用 CPU 的 `GradientBlurTransformation`，接口不变。需要统一路径时设 `preferGpu = false`。

---

[返回根目录 README](../../README.md)
