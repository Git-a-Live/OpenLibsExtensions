# coroutines: task index

English | [中文](coroutines.md)

Module `:coroutines`, depends on kotlinx-coroutines.

Coordinates and repository setup: [root README](../../README_EN.md#getting-started).

Tasks are registered by `TaskTag` and can be cancelled by that identifier. Tasks run in the caller's scope and are bound by its lifetime.

```kotlin
val tag = TaskTag("poll-config")

scope.launchTask(tag) { refresh() }
scope.launchDelayedTask(tag, delayMillis = 500) { refresh() }
scope.launchRepeatingTask(tag, periodMillis = 60_000) { refresh() }

CoroutineTasks.cancel(tag)
CoroutineTasks.cancelAll()
```

| Property | Behaviour |
|---|---|
| Registry cleanup | Entries are removed when a task finishes (completion, failure or cancellation), so the count equals the number of live tasks |
| Thread safety | Backed by `ConcurrentHashMap`; removal compares the `Job` instance, so a later task with the same tag is not evicted |
| Same tag | Replacement semantics: the task already registered under the tag is cancelled first |
| Repeat modes | `FixedRate` schedules from the start instant and ignores execution time; `FixedDelay` waits one period after each run |

The `clock` parameter of `launchRepeatingTask` defaults to `System.nanoTime()`; unit tests inject a virtual clock for deterministic results.

---

[Back to the root README](../../README_EN.md)
