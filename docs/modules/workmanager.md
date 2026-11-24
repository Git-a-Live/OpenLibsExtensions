# workmanager：请求构造

[English](workmanager_EN.md) | 中文

模块 `:workmanager`，依赖 WorkManager。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

```kotlin
oneTimeRequest(
    workerClass = SyncWorker::class.java,
    tag = "sync",
    constraints = buildConstraints(networkType = NetworkType.CONNECTED),
    backoff = Backoff(BackoffPolicy.EXPONENTIAL, delayMillis = 30_000),
)
```

- `oneTimeRequestBuilder<T>()` 与 `periodicRequestBuilder<T>()` 通过 `reified` 取得具体 worker 类型，`Builder` 持有的即为该类型
- `Backoff` 与周期参数在构造期校验下限（退避 ≥ 10s、周期 ≥ 15min），越界直接抛出
- `WorkScheduler` 提供 `init(context)` 与按标签、唯一名称、单次请求的取消入口

---

[返回根目录 README](../../README.md)
