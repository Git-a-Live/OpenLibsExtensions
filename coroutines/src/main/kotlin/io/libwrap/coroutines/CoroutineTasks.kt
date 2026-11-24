package io.libwrap.coroutines

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 按 [TaskTag] 登记协程任务，提供按标识取消的能力。
 *
 * 三条约束：
 *
 * 1. **注册表自动清理**：任务结束（正常完成、异常或取消）时通过 `invokeOnCompletion`
 *    移除自身，条目数始终等于存活任务数。
 * 2. **注册表线程安全**：使用 [ConcurrentHashMap]，移除时比对 Job 实例，
 *    避免先启动的任务结束时误删后启动的同名任务。
 * 3. **同一 tag 为替换语义**：启动前取消该 tag 上的旧任务，同一标识不会同时存在两个任务。
 *
 * 任务本身仍挂在调用方的 [CoroutineScope] 上，生命周期受其约束；本对象只负责索引与取消，
 * 不创建游离作用域，因此不会让任务脱离结构化并发。
 */
public object CoroutineTasks {

    private val jobs = ConcurrentHashMap<TaskTag, Job>()

    /** 进程级作用域，带 [SupervisorJob]，单个任务失败不影响其他任务。 */
    public val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 当前登记的任务数量。 */
    public fun size(): Int = jobs.size

    /** 取出 [tag] 对应的任务，不存在时返回 null。 */
    public fun job(tag: TaskTag): Job? = jobs[tag]

    /** [tag] 上的任务是否存在且仍在运行。 */
    public fun isActive(tag: TaskTag): Boolean = jobs[tag]?.isActive == true

    /** 取消 [tag] 上的任务并从注册表移除；返回是否存在可取消的任务。 */
    public fun cancel(tag: TaskTag): Boolean {
        val job = jobs.remove(tag) ?: return false
        job.cancel()
        return true
    }

    /** 取消全部登记的任务。 */
    public fun cancelAll() {
        val snapshot = jobs.keys.toList()
        snapshot.forEach { cancel(it) }
    }

    internal fun register(tag: TaskTag, job: Job) {
        val previous = jobs.put(tag, job)
        previous?.cancel()
        job.invokeOnCompletion { jobs.remove(tag, job) }
    }
}

/** 在调用方作用域内启动任务，并按 [tag] 登记。 */
public fun CoroutineScope.launchTask(
    tag: TaskTag,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(start = start, block = block).also { CoroutineTasks.register(tag, it) }

/** 延迟 [delayMillis] 后执行一次，并按 [tag] 登记。 */
public fun CoroutineScope.launchDelayedTask(
    tag: TaskTag,
    delayMillis: Long,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(start = start) {
    delay(delayMillis.milliseconds)
    block()
}.also { CoroutineTasks.register(tag, it) }

/**
 * 周期执行，按 [tag] 登记。
 *
 * @param mode [RepeatMode.FixedRate] 以启动时刻为基准排期，执行耗时不计入周期；
 *             [RepeatMode.FixedDelay] 每次结束后再等待一个周期
 * @param initialDelayMillis 首次执行前的等待时间
 * @param clock 单调递增的毫秒时钟，用于 FixedRate 计算下一拍的目标时刻。
 *              默认取 `System.nanoTime()`；单元测试可注入虚拟时钟以获得确定结果
 */
public fun CoroutineScope.launchRepeatingTask(
    tag: TaskTag,
    periodMillis: Long,
    mode: RepeatMode = RepeatMode.FixedRate,
    initialDelayMillis: Long = 0L,
    clock: () -> Long = { System.nanoTime() / 1_000_000 },
    block: suspend CoroutineScope.() -> Unit,
): Job {
    require(periodMillis > 0) { "periodMillis 必须大于 0" }
    require(initialDelayMillis >= 0) { "initialDelayMillis 不能为负" }

    return launchTask(tag) {
        when (mode) {
            RepeatMode.FixedDelay -> {
                delay(initialDelayMillis.milliseconds)
                while (isActive) {
                    block()
                    delay(periodMillis.milliseconds)
                }
            }

            RepeatMode.FixedRate -> {
                val origin = clock() + initialDelayMillis
                var iteration = 0L
                while (isActive) {
                    val target = origin + iteration * periodMillis
                    val wait = target - clock()
                    if (wait > 0) delay(wait.milliseconds)
                    block()
                    iteration++
                }
            }
        }
    }
}
