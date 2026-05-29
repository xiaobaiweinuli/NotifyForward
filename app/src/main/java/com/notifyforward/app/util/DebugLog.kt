package com.notifyforward.app.util

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug 日志工具类
 * 使用 inline + reified 自动获取调用者类名作为 Tag
 * 支持同时输出到 Logcat 和本地文件
 */
object DebugLog {

    /**
     * 内部存储的 debug 开关状态
     * 初始值由应用启动时从配置读取
     */
    @PublishedApi
    internal var isDebugEnabled: Boolean = false

    private var context: Context? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 初始化 debug 开关状态和 Context
     */
    fun init(context: Context, enabled: Boolean) {
        this.context = context.applicationContext
        isDebugEnabled = enabled
        
        // 初始化时立即输出日志到文件（不管开关状态）
        val logDir = File(context.getExternalFilesDir(null), "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        val message = "DebugLog 初始化完成，日志开关：${if (enabled) "ON" else "OFF"}"
        Log.i("DebugLog", message)
        writeToFileInternal("DebugLog", "INFO", message, null)
    }

    /**
     * 更新 debug 开关状态
     */
    fun setEnabled(enabled: Boolean) {
        val oldState = isDebugEnabled
        isDebugEnabled = enabled
        
        // 开关变化时输出日志（不管开关状态）
        val message = "DebugLog 开关变化：${if (oldState) "ON" else "OFF"} → ${if (enabled) "ON" else "OFF"}"
        Log.i("DebugLog", message)
        writeToFileInternal("DebugLog", "INFO", message, null)
    }

    /**
     * Debug 级别日志
     * 使用 inline + reified 自动获取调用者类名作为 Tag
     * 只有在 debug 开关开启时才会输出
     */
    inline fun <reified T> d(message: String) {
        if (isDebugEnabled) {
            val tag = T::class.simpleName ?: "DebugLog"
            Log.d(tag, message)
            writeToFile(tag, "DEBUG", message, null)
        }
    }

    /**
     * Debug 级别日志（带异常）
     * 使用 inline + reified 自动获取调用者类名作为 Tag
     * 只有在 debug 开关开启时才会输出
     */
    inline fun <reified T> d(message: String, throwable: Throwable) {
        if (isDebugEnabled) {
            val tag = T::class.simpleName ?: "DebugLog"
            Log.d(tag, message, throwable)
            writeToFile(tag, "DEBUG", message, throwable)
        }
    }

    /**
     * Info 级别日志（总是输出到 Logcat，日志文件输出受开关控制）
     */
    inline fun <reified T> i(message: String) {
        val tag = T::class.simpleName ?: "DebugLog"
        Log.i(tag, message)
        if (isDebugEnabled) {
            writeToFile(tag, "INFO", message, null)
        }
    }

    /**
     * Error 级别日志（总是输出到文件，不管开关）
     */
    inline fun <reified T> e(message: String) {
        val tag = T::class.simpleName ?: "DebugLog"
        Log.e(tag, message)
        writeToFile(tag, "ERROR", message, null)
    }

    inline fun <reified T> e(message: String, throwable: Throwable) {
        val tag = T::class.simpleName ?: "DebugLog"
        Log.e(tag, message, throwable)
        writeToFile(tag, "ERROR", message, throwable)
    }
    
    /**
     * 直接写入日志文件（不受开关控制，内部使用）
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun writeToFileInternal(tag: String, level: String, message: String, throwable: Throwable?) {
        val ctx = context ?: return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val logDir = File(ctx.getExternalFilesDir(null), "logs")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }

                val fileName = "log_${dateFormat.format(Date())}.txt"
                val logFile = File(logDir, fileName)

                val timeStr = timeFormat.format(Date())
                val logLine = if (throwable != null) {
                    "[$timeStr] [$level] [$tag] $message\n${Log.getStackTraceString(throwable)}\n"
                } else {
                    "[$timeStr] [$level] [$tag] $message\n"
                }

                FileWriter(logFile, true).use { writer ->
                    writer.append(logLine)
                }
            } catch (e: Exception) {
                Log.e("DebugLog", "写入日志文件失败: ${e.message}")
            }
        }
    }

    /**
     * 写入日志到文件
     * 文件名格式：log_2026-04-26.txt
     */
    @OptIn(DelicateCoroutinesApi::class)
    @PublishedApi
    internal fun writeToFile(tag: String, level: String, message: String, throwable: Throwable?) {
        val ctx = context ?: return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val logDir = File(ctx.getExternalFilesDir(null), "logs")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }

                val fileName = "log_${dateFormat.format(Date())}.txt"
                val logFile = File(logDir, fileName)

                val timeStr = timeFormat.format(Date())
                val logLine = if (throwable != null) {
                    "[$timeStr] [$level] [$tag] $message\n${Log.getStackTraceString(throwable)}\n"
                } else {
                    "[$timeStr] [$level] [$tag] $message\n"
                }

                FileWriter(logFile, true).use { writer ->
                    writer.append(logLine)
                }
            } catch (e: Exception) {
                Log.e("DebugLog", "写入日志文件失败: ${e.message}")
            }
        }
    }
}
