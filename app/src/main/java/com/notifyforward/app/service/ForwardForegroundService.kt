package com.notifyforward.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.notifyforward.app.MainActivity
import com.notifyforward.app.NotifyForwardApp
import com.notifyforward.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG        = "ForwardFGS"
private const val NOTIF_ID   = 1001
private const val CHANNEL_ID = "forward_service_channel"

class ForwardForegroundService : Service() {

    private val app get() = NotifyForwardApp.from(applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { 
            scope.launch {
                app.configStore.setFgServiceEnabled(false)
            }
            stopSelf()
            return START_NOT_STICKY 
        }

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        app.setFgServiceRunning(true)

        scope.launch {
            runCatching { app.repository.processQueue() }
                .onFailure { Log.e(TAG, "processQueue: ${it.message}", it) }
        }
        Log.i(TAG, "前台服务已启动")
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        app.setFgServiceRunning(false)
        Log.w(TAG, "前台服务被销毁")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── 通知 ──────────────────────────────────────

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.foreground_service_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply { description = getString(R.string.foreground_service_channel_desc); setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, ForwardForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_service_title))
            .setContentText(getString(R.string.foreground_service_content))
            .setSmallIcon(R.drawable.ic_notif_small)
            .setContentIntent(tap)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stop)
            .build()
    }

    companion object {
        private const val ACTION_STOP = "com.notifyforward.app.STOP_FGS"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ForwardForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ForwardForegroundService::class.java).apply { action = ACTION_STOP }
            )
        }
    }
}
