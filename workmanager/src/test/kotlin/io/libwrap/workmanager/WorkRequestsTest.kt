package io.libwrap.workmanager

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 请求构造的验证。
 *
 * 重点在 worker 类型：`Builder` 必须持有具体的 worker 类，WorkManager 依据该类型实例化 worker。
 * 测试只使用 `Class` 对象，不实例化 worker 与 `Context`。
 */
class WorkRequestsTest {

    private class SampleWorker(context: Context, parameters: WorkerParameters) :
        ListenableWorker(context, parameters) {
        override fun startWork(): ListenableFuture<Result> = throw UnsupportedOperationException()
    }

    private val workerClass = SampleWorker::class.java

    @Test
    fun `一次性请求持有具体 worker 类型与标签`() {
        val request = oneTimeRequest(workerClass = workerClass, tag = "sync")

        assertEquals(workerClass.name, request.workSpec.workerClassName)
        assertTrue("标签应包含调用方指定的值", request.tags.contains("sync"))
    }

    @Test
    fun `周期请求持有具体 worker 类型`() {
        val request = periodicRequest(
            workerClass = workerClass,
            repeatInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
        )

        assertEquals(workerClass.name, request.workSpec.workerClassName)
    }

    @Test
    fun `约束按参数逐个添加`() {
        val built = buildConstraints(
            networkType = NetworkType.CONNECTED,
            requiresCharging = true,
            requiresBatteryNotLow = false,
        )

        assertEquals(NetworkType.CONNECTED, built.requiredNetworkType)
        assertTrue(built.requiresCharging())
        assertFalse(built.requiresBatteryNotLow())
    }

    @Test
    fun `未指定的约束不改变默认值`() {
        val built = buildConstraints()

        assertEquals(NetworkType.NOT_REQUIRED, built.requiredNetworkType)
        assertFalse(built.requiresCharging())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `退避延迟低于下限时拒绝构造`() {
        Backoff(BackoffPolicy.LINEAR, delayMillis = 1_000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `周期低于下限时拒绝构造`() {
        periodicRequest(workerClass = workerClass, repeatInterval = 60_000)
    }

    @Test
    fun `合法退避延迟写入请求`() {
        val request = oneTimeRequest(
            workerClass = workerClass,
            backoff = Backoff(BackoffPolicy.EXPONENTIAL, delayMillis = 30_000),
        )

        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
        assertEquals(30_000L, request.workSpec.backoffDelayDuration)
    }

    @Test
    fun `时间单位参与周期换算`() {
        val request = periodicRequest(
            workerClass = workerClass,
            repeatInterval = 15,
            timeUnit = TimeUnit.MINUTES,
        )

        assertEquals(
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
            request.workSpec.intervalDuration,
        )
    }
}
