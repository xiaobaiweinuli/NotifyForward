package com.notifyforward.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 转发结果状态 */
object ForwardStatus {
    const val SUCCESS  = "SUCCESS"   // 成功转发
    const val FAILED   = "FAILED"    // 转发失败（网络/服务端错误）
    const val SKIPPED  = "SKIPPED"   // 被规则过滤/去重，未转发
    const val RECEIVED = "RECEIVED"  // 已接收通知，但未转发（全局关/Webhook未配置/自身/系统通知）
}

@Entity(tableName = "forward_history")
data class ForwardHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packageName: String,
    val appName: String,

    /** 通知标题 */
    val notifTitle: String,

    /** 通知正文 */
    val notifContent: String,

    /** 通知原始时间戳 */
    val notifyTime: Long,

    /** 实际转发时间戳 */
    val forwardTime: Long = System.currentTimeMillis(),

    /** 命中的规则名称；SKIPPED 时为过滤原因说明 */
    val matchedRuleName: String = "",

    /** 转发结果 @see ForwardStatus */
    val status: String,

    /** 失败时的错误信息 */
    val errorMsg: String = "",

    /** 转发目标 Webhook（脱敏：仅存最后 8 位 key */
    val webhookHint: String = "",

    /** 通知来源 @see com.notifyforward.app.model.NotificationSource */
    val source: Int = com.notifyforward.app.model.NotificationSource.NOTIFICATION_SERVICE
)
