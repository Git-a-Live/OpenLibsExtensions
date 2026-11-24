package io.libwrap.coroutines

/**
 * 任务标识。
 *
 * 以值类型承载 key，调用点之间的标识不会因拼写相同而互相替代。
 * 等值与哈希基于 [value]，可直接作为 Map 的键。
 */
@JvmInline
public value class TaskTag(public val value: String) {
    override fun toString(): String = value
}

/** 周期性任务的调度方式。 */
public enum class RepeatMode {
    /**
     * 以启动时刻为基准排期：第 n 次执行的目标时刻是 `start + n * period`，
     * 单次执行耗时不会累积到后续周期。
     */
    FixedRate,

    /**
     * 每次执行结束后再等待 `period`：周期 = `period + 执行耗时`。
     * 适用于不希望重叠执行的后台轮询。
     */
    FixedDelay,
}
