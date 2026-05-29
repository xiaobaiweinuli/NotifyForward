package com.notifyforward.app.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.notifyforward.app.service.NotificationMonitorService

/**
 * 权限工具类，用于处理和权限相关的操作
 */
object PermissionUtils {

    /**
     * 检查是否已经忽略电池优化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            // Android 6.0 以下没有这个概念，默认返回 true
            true
        }
    }

    /**
     * 获取请求忽略电池优化的 Intent（直接页）
     */
    fun getRequestIgnoreBatteryOptimizationsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
    }

    /**
     * 打开电池优化列表（全局列表）
     */
    fun getOpenBatteryOptimizationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Settings.ACTION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * 打开通用应用设置页面（用于应用列表权限等其他设置）
     */
    fun getOpenAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * 打开通知访问设置
     */
    fun getOpenNotificationListenerSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * 检查是否有接收短信的权限
     */
    fun hasSmsReceivePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否有读取短信的权限
     */
    fun hasReadSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否有读取电话状态的权限
     */
    fun hasReadPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否有拨打电话的权限
     */
    fun hasCallPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否有读取通话记录的权限
     */
    fun hasReadCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查短信监听相关权限是否都已授予
     */
    fun hasSmsMonitorPermissions(context: Context): Boolean {
        return hasSmsReceivePermission(context) && hasReadSmsPermission(context)
    }

    /**
     * 检查电话监听相关权限是否都已授予
     */
    fun hasPhoneMonitorPermissions(context: Context): Boolean {
        return hasReadPhoneStatePermission(context) && hasCallPhonePermission(context)
    }

    /**
     * 检查是否有查询所有已安装应用列表的权限
     */
    fun hasQueryAllPackagesPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Android 11 以下不需要这个权限
            return true
        }
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    "android:get_installed_apps",
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    "android:get_installed_apps",
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            // 如果检查失败，尝试直接查一个应用看能不能查到
            try {
                val pm = context.packageManager
                val apps = pm.getInstalledApplications(0)
                // 如果能查到多个应用，说明有权限
                apps.size > 5
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * 获取打开应用列表权限设置的 Intent
     * 注：这个权限没有统一的设置页面，通常直接跳转到应用详情页
     */
    fun getOpenAppListPermissionSettingsIntent(context: Context): Intent {
        // 先尝试跳转到应用详情页
        return getOpenAppDetailsSettingsIntent(context)
    }

    /**
     * 检查是否有通知监听权限
     */
    fun hasNotificationListenerPermission(context: Context): Boolean {
        return NotificationMonitorService.isEnabled(context)
    }
}
