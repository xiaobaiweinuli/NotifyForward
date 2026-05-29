package com.notifyforward.app.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.notifyforward.app.NotifyForwardApp
import com.notifyforward.app.service.ForwardForegroundService
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val TAG       = "ServiceCheckWorker"
private const val WORK_NAME = "service_check_periodic"

class ServiceCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app    = NotifyForwardApp.from(applicationContext)
        val config = app.configStore.configFlow.first()

        if (!config.fgServiceEnabled) { Log.d(TAG, "前台保活未开启，跳过"); return Result.success() }

        if (!app.isForegroundServiceRunning.value) {
            Log.w(TAG, "前台服务未运行，重启…")
            ForwardForegroundService.start(applicationContext)
        }
        app.repository.refreshTodayCount()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<ServiceCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req
            )
            Log.i(TAG, "WorkManager 心跳已注册")
        }
    }
}
