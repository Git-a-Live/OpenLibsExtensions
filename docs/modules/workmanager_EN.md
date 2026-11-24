# workmanager: request construction

English | [中文](workmanager.md)

Module `:workmanager`, depends on WorkManager.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

```kotlin
oneTimeRequest(
    workerClass = SyncWorker::class.java,
    tag = "sync",
    constraints = buildConstraints(networkType = NetworkType.CONNECTED),
    backoff = Backoff(BackoffPolicy.EXPONENTIAL, delayMillis = 30_000),
)
```

- `oneTimeRequestBuilder<T>()` and `periodicRequestBuilder<T>()` obtain the concrete worker type through `reified`, and the `Builder` holds exactly that type.
- `Backoff` and periodic intervals validate their floors at construction time (backoff at least 10 s, interval at least 15 min) and throw when out of range.
- `WorkScheduler` exposes `init(context)` and cancellation by tag, unique work name or single request.

---

[Back to the root README](../../README_EN.md)
