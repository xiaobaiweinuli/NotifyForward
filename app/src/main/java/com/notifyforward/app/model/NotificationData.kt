package com.notifyforward.app.model

/**
 * 通知来源类型
 */
object NotificationSource {
    /** 来自 NotificationListenerService 的系统通知 */
    const val NOTIFICATION_SERVICE = 0
    /** 来自 SmsReceiver 的短信广播 */
    const val SMS_BROADCAST = 1
    /** 来自 PhoneReceiver 的电话广播 */
    const val PHONE_BROADCAST = 2
}

/**
 * 从 StatusBarNotification 提取出的结构化通知数据
 * @param dedupeKey 用于去重判断的 key，由 packageName+title+content 的 hash 组成
 */
data class NotificationData(
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val subText: String,
    val timestamp: Long,
    val sbnKey: String,
    val source: Int = NotificationSource.NOTIFICATION_SERVICE,
    val isOnlyAlertOnce: Boolean = false,
    val isOngoing: Boolean = false,
    val category: String? = null,
    val flags: Int = 0,
    val dedupeKey: String = "$packageName|$title|$content".hashCode().toString()
)
