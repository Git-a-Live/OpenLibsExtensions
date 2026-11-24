# zxing：二维码

[English](zxing_EN.md) | 中文

模块 `:zxing`，依赖 ZXing core。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

```kotlin
val bitmap = QrCodes.encode(
    content = "https://example.com",
    options = QrCodeOptions(sizePx = 512, quietZoneModules = 1),
    centerIcon = logo,
    iconRatio = 0.18f,
)
```

- 默认纠错等级 H（约 30% 容错），为叠加中心图标的前提；图标占比被钳制在 `[0.1, 0.25]`
- 图标外围自动留出一圈背景色，避免与相邻码元贴边导致识别率下降
- 通过 `setPixels` 一次性写入像素数组，较逐点绘制减少调用次数
- 仅依赖 `com.google.zxing:core`，不引入 `zxing-android-embedded` 等含 Camera 的依赖

---

[返回根目录 README](../../README.md)
