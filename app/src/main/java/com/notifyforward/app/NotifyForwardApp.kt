package com.notifyforward.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.notifyforward.app.data.AppConfigStore
import com.notifyforward.app.data.AppDatabase
import com.notifyforward.app.repository.ForwardRepository
import com.notifyforward.app.util.DebugLog
import com.notifyforward.app.worker.ServiceCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotifyForwardApp : Application() {

    /** Application 级别协程作用域，用于初始化任务 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set
    lateinit var configStore: AppConfigStore
        private set
    lateinit var repository: ForwardRepository
        private set

    /** NotificationListenerService 连接状态（由 NLS 回调更新） */
    val isListenerConnected = MutableStateFlow(false)

    /** 前台服务是否正在运行 */
    val isForegroundServiceRunning: StateFlow<Boolean> get() = _isFgServiceRunning
    private val _isFgServiceRunning = MutableStateFlow(false)

    fun setFgServiceRunning(running: Boolean) {
        _isFgServiceRunning.value = running
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        database    = AppDatabase.create(this)
        configStore = AppConfigStore(this)
        repository  = ForwardRepository(database, configStore, this)

        // 初始化 debug 日志开关
        appScope.launch {
            val config = configStore.configFlow.first()
            DebugLog.init(this@NotifyForwardApp, config.debugLogEnabled)
        }

        // 初始化今日统计
        appScope.launch {
            repository.refreshTodayCount()
        }

        // WorkManager 定时兜底检查（每 15 分钟）
        ServiceCheckWorker.schedule(this)

        Log.i(TAG, "App initialized")
    }

    companion object {
        private const val TAG = "NotifyForwardApp"
        private var INSTANCE: NotifyForwardApp? = null

        fun from(context: Context): NotifyForwardApp {
            return context.applicationContext as NotifyForwardApp
        }

        fun get(): NotifyForwardApp = requireNotNull(INSTANCE) {
            "NotifyForwardApp not initialized"
        }
    }
}
