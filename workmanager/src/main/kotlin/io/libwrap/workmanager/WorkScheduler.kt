package io.libwrap.workmanager

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest

/**
 * [WorkManager] 的薄封装，持有应用级实例并提供按标签、唯一名称的取消入口。
 *
 * 使用前需调用 [init]。若应用通过 `Configuration.Provider` 关闭了默认初始化，
 * 必须在调用 [init] 之前完成 `WorkManager.initialize`，否则 [init] 会抛出
 * `IllegalStateException`（由 WorkManager 自身抛出，含明确原因）。
 */
public object WorkScheduler {

    @Volatile
    private var instance: WorkManager? = null

    public fun init(context: Context) {
        instance = WorkManager.getInstance(context.applicationContext)
    }

    private fun require(): WorkManager = instance ?: error("WorkScheduler.init(context) 尚未调用")

    public fun enqueue(vararg requests: WorkRequest) {
        require().enqueue(requests.toList())
    }

    public fun enqueueUniqueWork(
        uniqueWorkName: String,
        policy: ExistingWorkPolicy,
        vararg requests: OneTimeWorkRequest,
    ) {
        require().enqueueUniqueWork(uniqueWorkName, policy, requests.toList())
    }

    public fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        policy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ) {
        require().enqueueUniquePeriodicWork(uniqueWorkName, policy, request)
    }

    public fun cancelByTag(tag: String) {
        require().cancelAllWorkByTag(tag)
    }

    public fun cancelUniqueWork(uniqueWorkName: String) {
        require().cancelUniqueWork(uniqueWorkName)
    }

    public fun cancel(request: WorkRequest) {
        require().cancelWorkById(request.id)
    }

    public fun cancelAll() {
        require().cancelAllWork()
    }
}
