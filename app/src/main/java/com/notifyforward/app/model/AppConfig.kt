package com.notifyforward.app.model

/**
 * 全局应用配置，存储于 DataStore
 */
data class AppConfig(
    /** 主题模式 */
    val themeMode: String = ThemeMode.SYSTEM.name,
    /** 全局转发开关 */
    val forwardingEnabled: Boolean = false,
    /** 前台保活服务开关 */
    val fgServiceEnabled: Boolean = false,
    /** 企业微信 Webhook 地址 */
    val webhookUrl: String = "",
    /** 全局消息模板（无规则匹配时使用此模板兜底） */
    val globalTemplate: String = "【{appName}】{title}\n{content}\n─── {datetime}",
    /** 失败后最大重试次数 */
    val retryCount: Int = 3,
    /** 相同内容去重开关 */
    val filterDuplicates: Boolean = true,
    /** 去重时间窗口（毫秒），窗口内相同 dedupeKey 只转发一次 */
    val duplicateWindowMs: Long = 5_000L,
    /** 是否过滤系统通知（包名以 android / com.android 开头） */
    val filterSystem: Boolean = true,
    /** 是否过滤 App 自身通知 */
    val filterSelf: Boolean = true,
    /** 是否防止重复通知（有短信/电话权限时自动过滤对应通知） */
    val preventDuplicateNotifications: Boolean = true,
    /** 是否开启 debug 日志 */
    val debugLogEnabled: Boolean = false
)

/** 模板可用变量说明 */
object TemplateVars {
    const val APP_NAME   = "{appName}"
    const val TITLE      = "{title}"
    const val CONTENT    = "{content}"
    const val SUB_TEXT   = "{subText}"
    const val PKG_NAME   = "{packageName}"
    const val TIME       = "{time}"
    const val DATE       = "{date}"
    const val DATETIME   = "{datetime}"

    val ALL = listOf(APP_NAME, TITLE, CONTENT, SUB_TEXT, PKG_NAME, TIME, DATE, DATETIME)
    val DESC = mapOf(
        APP_NAME  to "应用名称",
        TITLE     to "通知标题",
        CONTENT   to "通知正文",
        SUB_TEXT  to "副标题",
        PKG_NAME  to "包名",
        TIME      to "时间（HH:mm:ss）",
        DATE      to "日期（yyyy-MM-dd）",
        DATETIME  to "日期时间"
    )
}
