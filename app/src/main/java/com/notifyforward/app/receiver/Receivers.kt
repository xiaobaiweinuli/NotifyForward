package com.notifyforward.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import android.util.Log
import com.notifyforward.app.NotifyForwardApp
import com.notifyforward.app.model.NotificationData
import com.notifyforward.app.service.ForwardForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("BootReceiver", "收到广播: ${intent.action}")
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = NotifyForwardApp.from(context).configStore.configFlow.first()
                if (config.fgServiceEnabled) ForwardForegroundService.start(context)
            } finally { pending.finish() }
        }
    }
}

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val hasNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.activeNetwork?.let { network ->
                cm.getNetworkCapabilities(network)?.let { capabilities ->
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
            } ?: false
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
        if (!hasNetwork) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app    = NotifyForwardApp.from(context)
                val config = app.configStore.configFlow.first()
                if (config.forwardingEnabled && !app.isForegroundServiceRunning.value) {
                    Log.i("NetworkChangeReceiver", "网络恢复，重启前台服务")
                    ForwardForegroundService.start(context)
                }
            } finally { pending.finish() }
        }
    }
}

/**
 * 短信接收器
 */
class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
        private const val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SMS_RECEIVED) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = NotifyForwardApp.from(context)
                val config = app.configStore.configFlow.first()

                // 确保前台服务正在运行
                if (config.forwardingEnabled && !app.isForegroundServiceRunning.value) {
                    ForwardForegroundService.start(context)
                }

                // 解析短信
                val messages = parseSms(intent.extras)
                if (messages.isEmpty()) return@launch

                // 发送每条短信
                for (msg in messages) {
                    val data = NotificationData(
                        packageName = "com.android.mms",
                        appName = "短信",
                        title = "来自 ${msg.sender}",
                        content = msg.body,
                        subText = "",
                        timestamp = msg.timestamp,
                        sbnKey = "sms_${msg.timestamp}_${msg.sender.hashCode()}",
                        source = com.notifyforward.app.model.NotificationSource.SMS_BROADCAST,
                        isOnlyAlertOnce = false,
                        isOngoing = false,
                        category = null,
                        flags = 0
                    )
                    app.repository.enqueue(data)
                    Log.i(TAG, "收到短信: ${msg.sender}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理短信失败", e)
            } finally {
                pending.finish()
            }
        }
    }

    private data class SmsMsg(
        val sender: String,
        val body: String,
        val timestamp: Long
    )

    private fun parseSms(extras: Bundle?): List<SmsMsg> {
        @Suppress("DEPRECATION")
        val pdus = extras?.get("pdus") as? Array<*> ?: return emptyList()
        val format = extras.getString("format")
        val messages = mutableListOf<SmsMsg>()
        val fullBody = StringBuilder()
        var sender: String? = null
        var timestamp: Long = System.currentTimeMillis()

        for (pdu in pdus) {
            val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pdu as ByteArray)
            } ?: continue

            sender = sms.originatingAddress ?: sender
            fullBody.append(sms.messageBody ?: "")
            timestamp = sms.timestampMillis
        }

        if (sender != null && fullBody.isNotEmpty()) {
            messages.add(SmsMsg(sender!!, fullBody.toString(), timestamp))
        }

        return messages
    }
}

/**
 * 电话接收器
 */
class PhoneReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PhoneReceiver"
        private const val PHONE_STATE = "android.intent.action.PHONE_STATE"
        private const val NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL"
        private var lastState: String = TelephonyManager.EXTRA_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = NotifyForwardApp.from(context)
                val config = app.configStore.configFlow.first()

                // 确保前台服务正在运行
                if (config.forwardingEnabled && !app.isForegroundServiceRunning.value) {
                    ForwardForegroundService.start(context)
                }

                when (intent.action) {
                    PHONE_STATE -> handlePhoneState(context, intent, app)
                    NEW_OUTGOING_CALL -> handleOutgoingCall(context, intent, app)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理电话失败", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handlePhoneState(context: Context, intent: Intent, app: NotifyForwardApp) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        @Suppress("DEPRECATION")
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "电话状态变化: $lastState → $state, 号码: $incomingNumber")

        when {
            // 来电响铃
            state == TelephonyManager.EXTRA_STATE_RINGING && lastState == TelephonyManager.EXTRA_STATE_IDLE -> {
                val number = incomingNumber ?: "未知号码"
                val data = NotificationData(
                    packageName = "com.android.incallui",
                    appName = "电话",
                    title = "来电",
                    content = number,
                    subText = "",
                    timestamp = System.currentTimeMillis(),
                    sbnKey = "phone_in_${System.currentTimeMillis()}_${number.hashCode()}",
                    source = com.notifyforward.app.model.NotificationSource.PHONE_BROADCAST,
                    isOnlyAlertOnce = false,
                    isOngoing = false,
                    category = null,
                    flags = 0
                )
                app.repository.enqueue(data)
            }

            // 电话接通
            state == TelephonyManager.EXTRA_STATE_OFFHOOK && lastState == TelephonyManager.EXTRA_STATE_RINGING -> {
                val number = incomingNumber ?: "未知号码"
                val data = NotificationData(
                    packageName = "com.android.phone",
                    appName = "电话",
                    title = "电话已接通",
                    content = number,
                    subText = "",
                    timestamp = System.currentTimeMillis(),
                    sbnKey = "phone_answer_${System.currentTimeMillis()}_${number.hashCode()}",
                    source = com.notifyforward.app.model.NotificationSource.PHONE_BROADCAST,
                    isOnlyAlertOnce = false,
                    isOngoing = false,
                    category = null,
                    flags = 0
                )
                app.repository.enqueue(data)
            }

            // 电话挂断
            state == TelephonyManager.EXTRA_STATE_IDLE && lastState != TelephonyManager.EXTRA_STATE_IDLE -> {
                val number = incomingNumber ?: "未知号码"
                val data = NotificationData(
                    packageName = "com.android.phone",
                    appName = "电话",
                    title = "电话已挂断",
                    content = number,
                    subText = "",
                    timestamp = System.currentTimeMillis(),
                    sbnKey = "phone_hang_${System.currentTimeMillis()}_${number.hashCode()}",
                    source = com.notifyforward.app.model.NotificationSource.PHONE_BROADCAST,
                    isOnlyAlertOnce = false,
                    isOngoing = false,
                    category = null,
                    flags = 0
                )
                app.repository.enqueue(data)
            }
        }

        lastState = state
    }

    private suspend fun handleOutgoingCall(context: Context, intent: Intent, app: NotifyForwardApp) {
        @Suppress("DEPRECATION")
        val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: "未知号码"
        Log.d(TAG, "拨出电话: $number")

        val data = NotificationData(
            packageName = "com.android.phone",
            appName = "电话",
            title = "拨出电话",
            content = number,
            subText = "",
            timestamp = System.currentTimeMillis(),
            sbnKey = "phone_out_${System.currentTimeMillis()}_${number.hashCode()}",
            source = com.notifyforward.app.model.NotificationSource.PHONE_BROADCAST,
            isOnlyAlertOnce = false,
            isOngoing = false,
            category = null,
            flags = 0
        )
        app.repository.enqueue(data)
    }
}
