# trial：试听区间进度条

[English](trial_EN.md) | 中文

模块 `:trial` 与 `:trial-compose`，依赖 Compose（仅 trial-compose）。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

展示可试听区间的进度条，View 与 Compose 两种实现共用同一套模型与样式定义。

```kotlin
val range = TrialRange.ofMillis(startMillis = 30_000, endMillis = 60_000, durationMillis = 180_000)

seekBar.trialRange = range
seekBar.style = TrialSeekBarStyle(
    trackColor = 0x33FFFFFF,
    progressColor = 0xFFFFFFFF.toInt(),
    trialTrackColor = 0x33FFFFFF,
    trialProgressColor = 0xFFFFC107.toInt(),
    trackHeightPx = 6f * density,
    startEndpoint = TrialEndpoint.Circle(color = 0xFFFFFFFF.toInt(), radiusPx = 6f),
    endEndpoint = TrialEndpoint.Line(color = 0xFFFFFFFF.toInt(), widthPx = 4f),
)
seekBar.setProgress(positionMillis / durationMillis.toFloat())
```

```kotlin
TrialSeekBar(
    progress = positionMillis / durationMillis.toFloat(),
    trialRange = range,
    style = style,
    onProgressChange = { seekTo(it) },
    onSeekOutOfRange = { /* 由宿主决定后续动作 */ },
)
```

### 设计约束

| 维度 | 行为 |
|---|---|
| 坐标系 | 进度取 `0..1` 比例，控件不持有媒体时长，内部只维护一套坐标 |
| 配色 | 区间内外的底色与已播放色分别配置，共四个颜色值 |
| 端点 | `None` / `Line` / `Circle` / `Custom`，`Custom` 提供原生 `Canvas` 绘制入口 |
| 越界 | 仅通过 `onSeekOutOfRange` 上报原始比例，控件不执行任何业务动作；是否钳制由 `clampToRange` 决定 |
| 播放回写 | View 版拖动过程中忽略非用户来源的 `setProgress`，避免旧位置顶回手指位置 |

### 限制

- 未提供 XML 属性，样式通过代码设置
- 端点的 `Custom` 绘制入口接收原生 `Canvas`，Compose 侧通过 `drawIntoCanvas` 转交
- 渲染结果无自动化测试，单元测试覆盖区间模型的换算与校验

---

[返回根目录 README](../../README.md)
