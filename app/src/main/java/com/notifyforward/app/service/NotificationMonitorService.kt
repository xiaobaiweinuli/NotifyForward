package com.notifyforward.app.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notifyforward.app.NotifyForwardApp
import com.notifyforward.app.model.NotificationData
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "NotifMonitor"

class NotificationMonitorService : NotificationListenerService() {

    private val app get() = NotifyForwardApp.from(applicationContext)

    // 活跃通知 Key 缓存：用于识别"已知通知"的更新
    private val activeNotifKeys = ConcurrentHashMap<String, Long>()

    // Ongoing 通知的 Title 缓存：用于判断是否是有意义的更新
    private val ongoingTitleCache = ConcurrentHashMap<String, String>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        app.isListenerConnected.value = true
        Log.i(TAG, "监听服务已连接")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        app.isListenerConnected.value = false
        Log.w(TAG, "监听服务断开，尝试重连…")
        requestRebind(ComponentName(this, NotificationMonitorService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!shouldProcess(sbn)) return

        val flags = sbn.notification.flags
        val isOnlyAlertOnce = flags and Notification.FLAG_ONLY_ALERT_ONCE != 0
        val isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0
        val sbnKey = sbn.key
        val isKnownNotification = activeNotifKeys.containsKey(sbnKey)

        // ── 第一层：FLAG_ONLY_ALERT_ONCE（进度类更新，直接丢弃）──
        if (isOnlyAlertOnce && isKnownNotification) {
            Log.d(TAG, "跳过安静更新: $sbnKey (${sbn.packageName})")
            return
        }

        // ── 第二层：Ongoing 通知，按 Title 变化决定是否转发 ──
        if (isOngoing && isKnownNotification) {
            val currentTitle = sbn.notification.extras?.getString(Notification.EXTRA_TITLE).orEmpty()
            val lastTitle = ongoingTitleCache[sbnKey]

            if (currentTitle == lastTitle) {
                // Title 没变 → 内容数字在刷新（网速/进度），丢弃
                Log.d(TAG, "跳过 Ongoing 更新（Title 未变）: $sbnKey (${sbn.packageName})")
                return
            }
            // Title 变了 → 有意义的更新（换歌/切节点），放行并更新缓存
            ongoingTitleCache[sbnKey] = currentTitle
        }

        val data = extractData(sbn) ?: return

        // ── 记录并转发 ──
        activeNotifKeys[sbnKey] = System.currentTimeMillis()

        if (!isOngoing) {
            // 非 ongoing 的首次通知，也初始化 title 缓存
            val title = sbn.notification.extras?.getString(Notification.EXTRA_TITLE).orEmpty()
            ongoingTitleCache[sbnKey] = title
        } else if (!isKnownNotification) {
            // Ongoing 的首次通知，初始化 title 缓存
            val title = sbn.notification.extras?.getString(Notification.EXTRA_TITLE).orEmpty()
            ongoingTitleCache[sbnKey] = title
        }

        app.repository.enqueueNotification(data)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 通知消失时清理缓存
        activeNotifKeys.remove(sbn.key)
        ongoingTitleCache.remove(sbn.key)
        Log.d(TAG, "清理缓存 Key: ${sbn.key}")
    }

    // ── 数据提取 ──────────────────────────────────

    private fun extractData(sbn: StatusBarNotification): NotificationData? {
        return try {
            val extras  = sbn.notification.extras
            val title   = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
            val text    = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""
            if (title.isBlank() && text.isBlank()) return null

            val appName = runCatching {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(sbn.packageName, 0)
                ).toString()
            }.getOrDefault(sbn.packageName)

            val flags = sbn.notification.flags
            val isOnlyAlertOnce = flags and Notification.FLAG_ONLY_ALERT_ONCE != 0
            val isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0
            val category = sbn.notification.category

            NotificationData(
                packageName = sbn.packageName,
                appName     = appName,
                title       = title,
                content     = text,
                subText     = subText,
                timestamp   = sbn.postTime,
                sbnKey      = sbn.key,
                isOnlyAlertOnce = isOnlyAlertOnce,
                isOngoing = isOngoing,
                category = category,
                flags = flags
            )
        } catch (e: Exception) {
            Log.e(TAG, "extractData: ${e.message}")
            null
        }
    }

    private fun shouldProcess(sbn: StatusBarNotification): Boolean {
        if (sbn.isOngoing && sbn.packageName == packageName) return false
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return true
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            val cn = ComponentName(context, NotificationMonitorService::class.java)
            return flat.split(":").any { ComponentName.unflattenFromString(it) == cn }
        }

        /**
         * 请求重新绑定通知监听服务（用于授权后立即生效）
         */
        fun requestRebind(context: Context) {
            val cn = ComponentName(context, NotificationMonitorService::class.java)
            try {
                // 先尝试直接调用 requestRebind（反射方式）
                val clazz = Class.forName("android.service.notification.NotificationListenerService")
                val method = clazz.getDeclaredMethod("requestRebind", ComponentName::class.java)
                method.isAccessible = true
                // requestRebind 是静态方法，传入 null 作为实例
                method.invoke(null, cn)
                Log.i(TAG, "requestRebind 调用成功")
            } catch (e: Exception) {
                Log.w(TAG, "反射调用 requestRebind 失败，尝试切换组件状态", e)
                // 如果反射失败，尝试通过 PackageManager 切换组件状态
                toggleNotificationListenerService(context)
            }
        }

        /**
         * 通过切换组件启用/禁用状态来触发重新绑定
         */
        private fun toggleNotificationListenerService(context: Context) {
            val pm = context.packageManager
            val cn = ComponentName(context, NotificationMonitorService::class.java)
            try {
                // 先禁用
                pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.i(TAG, "服务已禁用，等待重新启用")
                // 短暂延迟后重新启用
                Thread.sleep(200)
                pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.i(TAG, "服务已重新启用")
            } catch (e: Exception) {
                Log.e(TAG, "切换组件状态失败", e)
            }
        }
    }
}
