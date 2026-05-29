package com.notifyforward.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 规则类型
 */
object RuleType {
    /** 普通规则（包名+关键词） */
    const val GENERAL = 0
    /** 短信规则 */
    const val SMS = 1
    /** 电话规则 */
    const val PHONE = 2
}

/**
 * 电话状态
 */
object PhoneState {
    /** 来电响铃 */
    const val RINGING = "ringing"
    /** 电话接通 */
    const val ANSWERED = "answered"
    /** 电话挂断 */
    const val HANGUP = "hangup"
    /** 拨出电话 */
    const val OUTGOING = "outgoing"
}

/**
 * 转发规则实体
 *
 * 普通规则过滤逻辑（AND 关系）：
 * 1. packageNamesJson 为空数组 → 匹配所有 App；否则包名必须在列表内
 * 2. includeKeywordsJson 非空 → 标题或正文必须包含至少一个关键词
 * 3. excludeKeywordsJson 非空 → 标题和正文都不能包含任何排除词
 *
 * 短信规则：只要是短信就匹配（可选关键词过滤）
 *
 * 电话规则：匹配指定的电话状态（可选关键词过滤）
 */
@Entity(tableName = "forward_rules")
data class ForwardRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 规则名称，用户自定义显示 */
    val name: String,

    /** 是否启用 */
    val enabled: Boolean = true,

    /**
     * 规则类型
     * 0 = 普通规则，1 = 短信，2 = 电话
     */
    val ruleType: Int = RuleType.GENERAL,

    /**
     * 包名白名单（JSON 字符串数组，如 ["com.tencent.mm","com.alipay.android.app"]）
     * 空数组 "[]" 表示匹配所有 App
     * 仅普通规则使用
     */
    val packageNamesJson: String = "[]",

    /**
     * 标题正则表达式（包含匹配）
     * 留空表示不过滤标题
     * 所有规则类型都支持
     */
    val titleRegex: String = "",

    /**
     * 内容正则表达式（包含匹配）
     * 留空表示不过滤内容
     * 所有规则类型都支持
     */
    val contentRegex: String = "",

    /**
     * 电话状态（JSON 字符串数组），仅电话规则使用
     * 可选值："ringing", "answered", "hangup", "outgoing"
     */
    val phoneStatesJson: String = "[]",

    /**
     * 本规则使用的消息模板；留空则回退到全局模板
     * 支持变量：{appName} {title} {content} {subText} {packageName} {time} {date} {datetime}
     */
    val messageTemplate: String = "",

    /** 规则优先级（越小越高），匹配第一个 */
    val priority: Int = 100,

    val createdAt: Long = System.currentTimeMillis()
)
