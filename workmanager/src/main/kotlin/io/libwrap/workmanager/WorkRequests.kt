package io.libwrap.workmanager

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import java.util.concurrent.TimeUnit

/** 退避策略。WorkManager 要求延迟不低于 [androidx.work.WorkRequest.MIN_BACKOFF_MILLIS]。 */
public data class Backoff(
    public val policy: BackoffPolicy,
    public val delayMillis: Long,
) {
    init {
        require(delayMillis >= androidx.work.WorkRequest.MIN_BACKOFF_MILLIS) {
            "延迟不得小于 ${androidx.work.WorkRequest.MIN_BACKOFF_MILLIS}ms，当前为 $delayMillis"
        }
    }
}

/**
 * 构造 [OneTimeWorkRequest.Builder]，worker 类型由调用点推导。
 *
 * 类型参数参与构造，`Builder` 持有的是具体的 worker 类；WorkManager 依据该类型实例化 worker。
 */
public inline fun <reified T : ListenableWorker> oneTimeRequestBuilder(): OneTimeWorkRequest.Builder =
    OneTimeWorkRequest.Builder(T::class.java)

/**
 * 构造 [PeriodicWorkRequest.Builder]。
 *
 * @param repeatInterval 执行间隔，不得小于 [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]
 */
public inline fun <reified T : ListenableWorker> periodicRequestBuilder(
    repeatInterval: Long,
    timeUnit: TimeUnit,
): PeriodicWorkRequest.Builder {
    requireValidPeriod(repeatInterval, timeUnit)
    return PeriodicWorkRequest.Builder(T::class.java, repeatInterval, timeUnit)
}

/** 下限以毫秒计，校验前需按 [timeUnit] 换算，否则 15 分钟会因数值 15 被判为过短。 */
@PublishedApi
internal fun requireValidPeriod(repeatInterval: Long, timeUnit: TimeUnit) {
    val millis = timeUnit.toMillis(repeatInterval)
    require(millis >= PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS) {
        "周期不得小于 ${PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS}ms，当前为 ${millis}ms"
    }
}

/** 构造约束；参数为 null 表示不添加该约束。 */
public fun buildConstraints(
    networkType: NetworkType? = null,
    requiresCharging: Boolean? = null,
    requiresDeviceIdle: Boolean? = null,
    requiresBatteryNotLow: Boolean? = null,
    requiresStorageNotLow: Boolean? = null,
): Constraints {
    val builder = Constraints.Builder()
    networkType?.let(builder::setRequiredNetworkType)
    requiresCharging?.let(builder::setRequiresCharging)
    requiresDeviceIdle?.let(builder::setRequiresDeviceIdle)
    requiresBatteryNotLow?.let(builder::setRequiresBatteryNotLow)
    requiresStorageNotLow?.let(builder::setRequiresStorageNotLow)
    return builder.build()
}

/**
 * 构造一次性任务。
 *
 * @param tag 任务标签，用于 [WorkScheduler.cancelByTag] 批量取消；为 null 时不添加标签
 * @param initialDelayMillis 首次执行前的等待时间，必须为正数
 */
public fun <T : ListenableWorker> oneTimeRequest(
    workerClass: Class<T>,
    tag: String? = null,
    workerParameters: androidx.work.Data = androidx.work.Data.EMPTY,
    constraints: Constraints? = null,
    initialDelayMillis: Long? = null,
    backoff: Backoff? = null,
): OneTimeWorkRequest {
    val builder = OneTimeWorkRequest.Builder(workerClass).setInputData(workerParameters)
    tag?.let(builder::addTag)
    constraints?.let(builder::setConstraints)
    initialDelayMillis?.let { builder.setInitialDelay(it, TimeUnit.MILLISECONDS) }
    backoff?.let { builder.setBackoffCriteria(it.policy, it.delayMillis, TimeUnit.MILLISECONDS) }
    return builder.build()
}

/**
 * 构造周期任务。
 *
 * @param repeatInterval 执行间隔，不得小于 [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]
 */
public fun <T : ListenableWorker> periodicRequest(
    workerClass: Class<T>,
    repeatInterval: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    tag: String? = null,
    workerParameters: androidx.work.Data = androidx.work.Data.EMPTY,
    constraints: Constraints? = null,
    initialDelayMillis: Long? = null,
): PeriodicWorkRequest {
    requireValidPeriod(repeatInterval, timeUnit)
    val builder = PeriodicWorkRequest.Builder(workerClass, repeatInterval, timeUnit)
        .setInputData(workerParameters)
    tag?.let(builder::addTag)
    constraints?.let(builder::setConstraints)
    initialDelayMillis?.let { builder.setInitialDelay(it, TimeUnit.MILLISECONDS) }
    return builder.build()
}
