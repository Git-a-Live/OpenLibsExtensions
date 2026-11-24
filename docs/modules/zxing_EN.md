# zxing: QR codes

English | [中文](zxing.md)

Module `:zxing`, depends on ZXing core.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

```kotlin
val bitmap = QrCodes.encode(
    content = "https://example.com",
    options = QrCodeOptions(sizePx = 512, quietZoneModules = 1),
    centerIcon = logo,
    iconRatio = 0.18f,
)
```

- Error correction defaults to level H (about 30% tolerance), which is what makes a centre icon viable; the icon ratio is clamped to `[0.1, 0.25]`.
- A ring of background colour is drawn around the icon so it never touches adjacent modules, which would otherwise sharply reduce readability.
- Pixels are written once through `setPixels`, reducing call count compared with per-point drawing.
- Only `com.google.zxing:core` is required; camera-bearing artifacts such as `zxing-android-embedded` are not pulled in.

---

[Back to the root README](../../README_EN.md)
