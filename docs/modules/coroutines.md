# coroutines：协程任务索引

[English](coroutines_EN.md) | 中文

模块 `:coroutines`，依赖 kotlinx-coroutines。

依赖坐标与仓库配置见[根目录 README](../../README.md#接入)。

按 [TaskTag] 登记任务，支持按标识取消。任务挂在调用方作用域上，生命周期受其约束。

```kotlin
val tag = TaskTag("poll-config")

scope.launchTask(tag) { refresh() }
scope.launchDelayedTask(tag, delayMillis = 500) { refresh() }
scope.launchRepeatingTask(tag, periodMillis = 60_000) { refresh() }

CoroutineTasks.cancel(tag)
CoroutineTasks.cancelAll()
```

| 特性 | 行为 |
|---|---|
| 注册表清理 | 任务结束（完成、异常、取消）时自动移除条目，数量等于存活任务数 |
| 线程安全 | 基于 `ConcurrentHashMap`，移除时比对 Job 实例，不误删后启动的同名任务 |
| 同一 tag | 替换语义，启动前取消该 tag 上的任务 |
| 周期模式 | `FixedRate` 以启动时刻排期，执行耗时不计入；`FixedDelay` 每次结束后再等待 |

`launchRepeatingTask` 的 `clock` 参数默认取 `System.nanoTime()`，单元测试可注入虚拟时钟以获得确定结果。

---

[返回根目录 README](../../README.md)
