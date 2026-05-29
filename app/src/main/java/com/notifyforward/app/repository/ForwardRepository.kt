package com.notifyforward.app.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notifyforward.app.data.AppDatabase
import com.notifyforward.app.data.AppConfigStore
import com.notifyforward.app.data.entity.ForwardHistory
import com.notifyforward.app.data.entity.ForwardRule
import com.notifyforward.app.data.entity.ForwardStatus
import com.notifyforward.app.model.AppConfig
import com.notifyforward.app.model.NotificationData
import com.notifyforward.app.network.WeChatForwarder
import com.notifyforward.app.util.PermissionUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ForwardRepository"

class ForwardRepository(
    private val db: AppDatabase,
    val configStore: AppConfigStore,
    private val context: Context
) {
    private val gson = Gson()
    private val forwarder = WeChatForwarder()

    /** 通知队列：NLS → Repository 的数据管道 */
    private val notificationQueue = Channel<NotificationData>(Channel.UNLIMITED)

    /** 去重缓存：dedupeKey → 最后转发时间戳 */
    private val dedupeCache = ConcurrentHashMap<String, Long>()

    /** 实时转发统计（今日） */
    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount

    /** 最后一次成功转发时间 */
    private val _lastForwardTime = MutableStateFlow<Long?>(null)
    val lastForwardTime: StateFlow<Long?> = _lastForwardTime

    // ──────────────────── 规则 DAO 代理 ────────────────────

    fun observeRules(): Flow<List<ForwardRule>> = db.forwardRuleDao().observeAll()
    suspend fun insertRule(rule: ForwardRule): Long = db.forwardRuleDao().insert(rule)
    suspend fun updateRule(rule: ForwardRule) = db.forwardRuleDao().update(rule)
    suspend fun deleteRule(rule: ForwardRule) = db.forwardRuleDao().delete(rule)
    suspend fun setRuleEnabled(id: Long, enabled: Boolean) = db.forwardRuleDao().setEnabled(id, enabled)

    // ──────────────────── 历史 DAO 代理 ────────────────────

    fun observeHistory(): Flow<List<ForwardHistory>> = db.forwardHistoryDao().observeRecent()
    suspend fun clearHistory() = db.forwardHistoryDao().clearAll()

    // ──────────────────── 队列入口 ────────────────────

    /** 由 NotificationMonitorService 调用，将新通知放入处理队列 */
    fun enqueueNotification(data: NotificationData) {
        val result = notificationQueue.trySend(data)
        Log.d(TAG, "enqueue: ${data.appName}/${data.title} → $result")
    }

    /** 通用的发送通知数据方法，供 SmsReceiver 和 PhoneReceiver 使用 */
    fun enqueue(data: NotificationData) {
        val result = notificationQueue.trySend(data)
        Log.d(TAG, "enqueue custom: ${data.appName}/${data.title} → $result")
    }

    // ──────────────────── 队列消费（在 FGS 的协程作用域内运行） ────────────────────

    /**
     * 阻塞地消费队列，直到协程被取消。
     * 应在 [ForwardForegroundService] 的 CoroutineScope 内调用。
     */
    suspend fun processQueue() {
        Log.d(TAG, "processQueue started")
        for (data in notificationQueue) {
            safeProcess(data)
        }
    }

    private suspend fun safeProcess(data: NotificationData) {
        try {
            processNotification(data)
        } catch (e: Exception) {
            Log.e(TAG, "processNotification error: ${e.message}", e)
        }
    }

    private suspend fun processNotification(data: NotificationData) {
        val config = configStore.configFlow.first()

        // 1. 全局开关
        if (!config.forwardingEnabled) {
            saveHistory(data, ForwardStatus.RECEIVED, "全局开关关闭", config)
            return
        }

        // 2. Webhook 检查
        if (config.webhookUrl.isBlank()) {
            Log.w(TAG, "webhook 未配置，跳过")
            saveHistory(data, ForwardStatus.RECEIVED, "Webhook 未配置", config)
            return
        }

        // 3. 过滤自身
        if (config.filterSelf && data.packageName == context.packageName) {
            saveHistory(data, ForwardStatus.RECEIVED, "自身通知已过滤", config)
            return
        }

        // 4. 防止重复通知：如果有短信/电话权限，就过滤对应通知（只过滤来自 NotificationListenerService 的）
        if (config.preventDuplicateNotifications && data.source == com.notifyforward.app.model.NotificationSource.NOTIFICATION_SERVICE) {
            if (PermissionUtils.hasSmsMonitorPermissions(context) && isSmsPackage(data.packageName)) {
                Log.d(TAG, "已有权限监听短信，过滤短信通知")
                saveHistory(data, ForwardStatus.SKIPPED, "已通过广播接收短信", config)
                return
            }
            if (PermissionUtils.hasPhoneMonitorPermissions(context) && isPhonePackage(data.packageName)) {
                Log.d(TAG, "已有权限监听电话，过滤电话通知")
                saveHistory(data, ForwardStatus.SKIPPED, "已通过广播接收电话事件", config)
                return
            }
        }

        // 5. 去重
        if (config.filterDuplicates) {
            val last = dedupeCache[data.dedupeKey]
            if (last != null && System.currentTimeMillis() - last < config.duplicateWindowMs) {
                Log.d(TAG, "去重跳过: ${data.dedupeKey}")
                saveHistory(data, ForwardStatus.SKIPPED, "重复通知（去重窗口内）", config)
                return
            }
        }

        // 6. 判断通知类型，准备规则匹配
        val isSms = data.packageName == "com.android.mms"
        val isPhone = data.packageName == "com.android.phone"

        // 7. 获取规则
        val rules = db.forwardRuleDao().getEnabledRules()

        // 8. 根据通知类型筛选规则
        val candidateRules = when {
            isSms -> rules.filter { it.ruleType == com.notifyforward.app.data.entity.RuleType.SMS }
            isPhone -> rules.filter { it.ruleType == com.notifyforward.app.data.entity.RuleType.PHONE }
            else -> rules.filter { it.ruleType == com.notifyforward.app.data.entity.RuleType.GENERAL }
        }

        // 9. 如果是普通通知，先检查系统过滤
        if (!isSms && !isPhone && config.filterSystem && isSystemPackage(data.packageName)) {
            saveHistory(data, ForwardStatus.RECEIVED, "系统通知已过滤", config)
            return
        }

        // 10. 规则匹配
        val matchedRule = candidateRules.firstOrNull { rule -> matchesRule(rule, data) }

        if (matchedRule == null) {
            val reason = if (rules.isNotEmpty()) "无匹配规则" else "未配置任何规则"
            Log.d(TAG, "$reason: ${data.packageName}/${data.title}")
            saveHistory(data, ForwardStatus.SKIPPED, reason, config)
            return
        }

        // 11. 渲染模板
        val template = if (matchedRule.messageTemplate.isNotBlank()) matchedRule.messageTemplate else config.globalTemplate
        val message = renderTemplate(template, data)

        // 12. 发送（带重试，网络 IO 在调用方的 IO dispatcher 上运行）
        val result = forwarder.sendWithRetry(config.webhookUrl, message, config.retryCount)

        // 13. 更新去重缓存 & 统计
        when (result) {
            is WeChatForwarder.ForwardResult.Success -> {
                dedupeCache[data.dedupeKey] = System.currentTimeMillis()
                cleanDedupeCache(config.duplicateWindowMs)
                _todayCount.value = _todayCount.value + 1
                _lastForwardTime.value = System.currentTimeMillis()
                saveHistory(data, ForwardStatus.SUCCESS, "", config, matchedRule.name)
                Log.i(TAG, "✅ 转发成功: ${data.appName}/${data.title}")
            }
            is WeChatForwarder.ForwardResult.Failure -> {
                saveHistory(data, ForwardStatus.FAILED, "errcode=${result.code} ${result.message}", config, matchedRule.name)
                Log.w(TAG, "❌ 转发失败[${result.code}]: ${result.message}")
            }
            is WeChatForwarder.ForwardResult.NetworkError -> {
                saveHistory(data, ForwardStatus.FAILED, result.cause.message ?: "网络异常", config, matchedRule.name)
                Log.w(TAG, "❌ 网络错误: ${result.cause.message}")
            }
        }

        // 14. 定期清理历史
        db.forwardHistoryDao().trimOldRecords(500)
    }

    // ──────────────────── 规则匹配 ────────────────────

    private fun matchesRule(rule: ForwardRule, data: NotificationData): Boolean {
        val listType = object : TypeToken<List<String>>() {}.type

        when (rule.ruleType) {
            com.notifyforward.app.data.entity.RuleType.SMS -> {
                // 短信规则：必须是短信
                if (data.packageName != "com.android.mms") return false
            }
            com.notifyforward.app.data.entity.RuleType.PHONE -> {
                // 电话规则：必须是电话，且状态匹配
                if (data.packageName != "com.android.phone") return false
                val phoneStates = gson.fromJson<List<String>>(rule.phoneStatesJson, listType)
                if (phoneStates.isNotEmpty()) {
                    val state = getPhoneStateFromTitle(data.title)
                    if (state == null || state !in phoneStates) return false
                }
            }
            com.notifyforward.app.data.entity.RuleType.GENERAL -> {
                // 普通规则：包名过滤
                val packageNames = gson.fromJson<List<String>>(rule.packageNamesJson, listType)
                if (packageNames.isNotEmpty() && data.packageName !in packageNames) return false
            }
        }

        // 所有规则类型都支持正则表达式过滤
        // 标题正则
        if (rule.titleRegex.isNotBlank()) {
            try {
                val regex = Regex(rule.titleRegex, RegexOption.IGNORE_CASE)
                if (!regex.containsMatchIn(data.title)) return false
            } catch (e: Exception) {
                Log.e(TAG, "标题正则表达式无效: ${rule.titleRegex}", e)
                // 正则无效则跳过匹配
            }
        }

        // 内容正则
        if (rule.contentRegex.isNotBlank()) {
            try {
                val regex = Regex(rule.contentRegex, RegexOption.IGNORE_CASE)
                if (!regex.containsMatchIn(data.content)) return false
            } catch (e: Exception) {
                Log.e(TAG, "内容正则表达式无效: ${rule.contentRegex}", e)
                // 正则无效则跳过匹配
            }
        }

        return true
    }

    private fun getPhoneStateFromTitle(title: String): String? {
        return when (title) {
            "来电" -> com.notifyforward.app.data.entity.PhoneState.RINGING
            "电话已接通" -> com.notifyforward.app.data.entity.PhoneState.ANSWERED
            "电话已挂断" -> com.notifyforward.app.data.entity.PhoneState.HANGUP
            "拨出电话" -> com.notifyforward.app.data.entity.PhoneState.OUTGOING
            else -> null
        }
    }

    // ──────────────────── 模板渲染 ────────────────────

    private fun renderTemplate(template: String, data: NotificationData): String {
        val fmt24 = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fmtDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fmtDatetime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val ts = Date(data.timestamp)
        return template
            .replace("{appName}",     data.appName)
            .replace("{title}",       data.title)
            .replace("{content}",     data.content)
            .replace("{subText}",     data.subText)
            .replace("{packageName}", data.packageName)
            .replace("{time}",        fmt24.format(ts))
            .replace("{date}",        fmtDate.format(ts))
            .replace("{datetime}",    fmtDatetime.format(ts))
    }

    // ──────────────────── 辅助方法 ────────────────────

    private fun isSystemPackage(pkg: String): Boolean {
        val systemPrefixes = listOf(
            "android", "com.android.", "com.google.android.gms",
            "com.miui.", "com.huawei.systemmanager", "com.oplus."
        )
        return systemPrefixes.any { pkg == it || pkg.startsWith(it) }
    }

    private fun isSmsPackage(pkg: String): Boolean {
        val smsPackages = listOf(
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.oneplus.mms",
            "com.oppo.mms",
            "com.vivo.messages",
            "com.meizu.sms"
        )
        return smsPackages.contains(pkg) || pkg.contains("sms") || pkg.contains("mms") || pkg.contains("message")
    }

    private fun isPhonePackage(pkg: String): Boolean {
        val phonePackages = listOf(
            "com.android.phone",
            "com.android.incallui",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.oneplus.dialer",
            "com.oppo.dialer",
            "com.vivo.dialer",
            "com.meizu.flyme.dialer"
        )
        return phonePackages.contains(pkg) || pkg.contains("phone") || pkg.contains("dialer") || pkg.contains("incall")
    }

    private fun cleanDedupeCache(windowMs: Long) {
        val threshold = System.currentTimeMillis() - windowMs * 10
        dedupeCache.entries.removeIf { it.value < threshold }
    }

    private suspend fun saveHistory(
        data: NotificationData,
        status: String,
        errorMsg: String,
        config: AppConfig,
        ruleName: String = ""
    ) {
        db.forwardHistoryDao().insert(
            ForwardHistory(
                packageName    = data.packageName,
                appName        = data.appName,
                notifTitle     = data.title,
                notifContent   = data.content,
                notifyTime     = data.timestamp,
                forwardTime    = System.currentTimeMillis(),
                matchedRuleName = ruleName,
                status         = status,
                errorMsg       = errorMsg,
                webhookHint    = forwarder.extractKeyHint(config.webhookUrl),
                source         = data.source
            )
        )
    }

    /** 刷新今日转发数（在 App/Service 启动时调用） */
    suspend fun refreshTodayCount() {
        val start = todayStartMs()
        _todayCount.value = db.forwardHistoryDao().countTodaySuccess(start)
        _lastForwardTime.value = db.forwardHistoryDao().lastSuccessTime()
    }

    private fun todayStartMs(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
