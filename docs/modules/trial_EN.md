# trial: trial-range seek bar

English | [中文](trial.md)

Modules `:trial` and `:trial-compose`, the latter depending on Compose.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

A seek bar that displays a trial interval, with View and Compose implementations sharing one model and style definition.

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
    onSeekOutOfRange = { /* the host decides what happens next */ },
)
```

### Design constraints

| Aspect | Behaviour |
|---|---|
| Coordinate system | Progress is a `0..1` fraction; the control holds no media duration and maintains a single coordinate space |
| Colours | Track and played colours are configured separately for inside and outside the range, four values in total |
| Endpoints | `None` / `Line` / `Circle` / `Custom`; `Custom` receives a native `Canvas` |
| Out of range | Reported through `onSeekOutOfRange` with the raw fraction; the control performs no business action, and clamping is governed by `clampToRange` |
| Playback writes | The View variant ignores non-user `setProgress` calls while dragging, so a stale position cannot snap the thumb back |

### Limitations

- No XML attributes; styling is applied from code
- The `Custom` endpoint entry point receives a native `Canvas`; the Compose side hands it over through `drawIntoCanvas`
- Rendering has no automated tests; unit tests cover the model conversion and validation

---

[Back to the root README](../../README_EN.md)
