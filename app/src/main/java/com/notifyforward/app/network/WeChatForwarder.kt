package com.notifyforward.app.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 企业微信机器人 Webhook 转发器
 *
 * 接口文档：https://developer.work.weixin.qq.com/document/path/91770
 * 消息类型：text（纯文本，最大 2048 字节）
 */
class WeChatForwarder {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    data class TextMessage(
        val msgtype: String = "text",
        val text: TextContent
    )
    data class TextContent(
        val content: String,
        val mentioned_list: List<String> = emptyList(),
        val mentioned_mobile_list: List<String> = emptyList()
    )

    data class WeChatResponse(
        val errcode: Int = -1,
        val errmsg: String = ""
    )

    sealed class ForwardResult {
        object Success : ForwardResult()
        data class Failure(val code: Int, val message: String) : ForwardResult()
        data class NetworkError(val cause: Throwable) : ForwardResult()
    }

    /**
     * 向企业微信 Webhook 发送文本消息
     * @param webhookUrl 完整 Webhook 地址（含 key 参数）
     * @param content    消息正文（已由模板引擎渲染完毕）
     */
    fun send(webhookUrl: String, content: String): ForwardResult {
        if (webhookUrl.isBlank()) {
            return ForwardResult.Failure(-1, "Webhook 地址未配置")
        }

        val body = TextMessage(text = TextContent(content = content.take(2048)))
        val requestBody = gson.toJson(body).toRequestBody(JSON_TYPE)

        val request = Request.Builder()
            .url(webhookUrl)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string() ?: "{}"
                if (!response.isSuccessful) {
                    ForwardResult.Failure(response.code, "HTTP ${response.code}: $raw")
                } else {
                    val resp = runCatching { gson.fromJson(raw, WeChatResponse::class.java) }
                        .getOrDefault(WeChatResponse())
                    if (resp.errcode == 0) {
                        ForwardResult.Success
                    } else {
                        ForwardResult.Failure(resp.errcode, resp.errmsg)
                    }
                }
            }
        } catch (e: Exception) {
            ForwardResult.NetworkError(e)
        }
    }

    /**
     * 带重试的发送
     * @param retryCount 最大重试次数（首次发送 + retryCount 次重试）
     */
    fun sendWithRetry(
        webhookUrl: String,
        content: String,
        retryCount: Int = 3,
        retryDelayMs: Long = 2_000L
    ): ForwardResult {
        var lastResult: ForwardResult = ForwardResult.Failure(-1, "未发起请求")
        repeat(retryCount + 1) { attempt ->
            if (attempt > 0) Thread.sleep(retryDelayMs * attempt)
            lastResult = send(webhookUrl, content)
            if (lastResult is ForwardResult.Success) return lastResult
        }
        return lastResult
    }

    /**
     * 获取 Webhook URL 中 key 的末 8 位，用于存储脱敏 hint
     */
    fun extractKeyHint(webhookUrl: String): String {
        return try {
            val key = Regex("[?&]key=([^&]+)").find(webhookUrl)?.groupValues?.get(1) ?: ""
            if (key.length > 8) "****${key.takeLast(8)}" else key
        } catch (_: Exception) { "" }
    }
}
