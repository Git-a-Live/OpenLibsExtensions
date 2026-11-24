package io.libwrap.coroutines

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineTasksTest {

    @Test
    fun `任务完成后自动从注册表移除`() = runTest {
        val started = CompletableDeferred<Unit>()
        val tag = TaskTag("finish")

        launchTask(tag) {
            started.complete(Unit)
        }
        started.await()
        runCurrent()

        assertFalse("任务结束后不应残留", CoroutineTasks.isActive(tag))
        assertNull("任务结束后应移除条目", CoroutineTasks.job(tag))
        assertEquals(0, CoroutineTasks.size())
    }

    @Test
    fun `任务异常结束后同样移除条目`() = runBlocking {
        // 用 SupervisorJob + 空异常处理器隔离失败，否则未捕获异常会让测试进程失败
        val scope = CoroutineScope(
            SupervisorJob() + CoroutineExceptionHandler { _, _ -> } + Dispatchers.Unconfined,
        )
        val tag = TaskTag("failing")

        val job = scope.launchTask(tag) {
            throw IllegalStateException("boom")
        }
        job.join()

        assertNull(CoroutineTasks.job(tag))
    }

    @Test
    fun `同一 tag 再次启动会取消旧任务`() = runTest {
        val tag = TaskTag("replace")
        val firstCancelled = CompletableDeferred<Unit>()

        launchTask(tag) {
            try {
                delay(10_000)
            } finally {
                firstCancelled.complete(Unit)
            }
        }
        runCurrent()

        launchTask(tag) {
            delay(10_000)
        }
        runCurrent()

        assertTrue("旧任务应被取消", firstCancelled.isCompleted)
        assertEquals("注册表中只保留一个条目", 1, CoroutineTasks.size())
        CoroutineTasks.cancel(tag)
    }

    @Test
    fun `按 tag 取消`() = runTest {
        val tag = TaskTag("cancel-me")
        val job = launchTask(tag) {
            delay(10_000)
        }
        runCurrent()
        assertTrue(CoroutineTasks.isActive(tag))

        assertTrue(CoroutineTasks.cancel(tag))
        assertFalse(job.isActive)
        assertFalse("重复取消应返回 false", CoroutineTasks.cancel(tag))
    }

    @Test
    fun `取消全部`() = runTest {
        repeat(3) { index ->
            launchTask(TaskTag("task-$index")) { delay(10_000) }
        }
        runCurrent()
        assertEquals(3, CoroutineTasks.size())

        CoroutineTasks.cancelAll()
        assertEquals(0, CoroutineTasks.size())
    }

    @Test
    fun `延迟任务在等待结束后执行`() = runTest {
        val tag = TaskTag("delayed")
        var executed = false

        launchDelayedTask(tag, delayMillis = 500) { executed = true }
        testScheduler.advanceTimeBy(499)
        runCurrent()
        assertFalse("未到延迟时间不应执行", executed)

        testScheduler.advanceTimeBy(2)
        runCurrent()
        assertTrue(executed)
    }

    @Test
    fun `FixedRate 的执行耗时不计入周期`() = runTest {
        val tag = TaskTag("fixed-rate")
        val executedAt = mutableListOf<Long>()

        launchRepeatingTask(
            tag = tag,
            periodMillis = 100,
            mode = RepeatMode.FixedRate,
            clock = { testScheduler.currentTime },
        ) {
            executedAt += testScheduler.currentTime
            delay(40)
        }

        testScheduler.advanceTimeBy(351)
        CoroutineTasks.cancel(tag)

        assertEquals(listOf(0L, 100L, 200L, 300L), executedAt)
    }

    @Test
    fun `FixedDelay 的周期包含执行耗时`() = runTest {
        val tag = TaskTag("fixed-delay")
        val executedAt = mutableListOf<Long>()

        launchRepeatingTask(
            tag = tag,
            periodMillis = 100,
            mode = RepeatMode.FixedDelay,
            clock = { testScheduler.currentTime },
        ) {
            executedAt += testScheduler.currentTime
            delay(40)
        }

        testScheduler.advanceTimeBy(451)
        CoroutineTasks.cancel(tag)

        assertEquals(listOf(0L, 140L, 280L, 420L), executedAt)
    }

    @Test
    fun `周期任务可被取消`() = runTest {
        val tag = TaskTag("ticker")
        val job = launchRepeatingTask(
            tag = tag,
            periodMillis = 100,
            clock = { testScheduler.currentTime },
        ) {
            delay(10)
        }
        runCurrent()

        job.cancel()
        job.join()
        assertFalse(job.isActive)
        assertNull(CoroutineTasks.job(tag))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `周期必须为正数`() {
        CoroutineScope(Dispatchers.Unconfined).launchRepeatingTask(
            TaskTag("bad"),
            periodMillis = 0,
        ) {
            // 参数校验在启动前完成
        }
    }
}
