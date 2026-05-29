package com.notifyforward.app.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notifyforward.app.NotifyForwardApp
import com.notifyforward.app.model.ThemeMode
import com.notifyforward.app.network.WeChatForwarder
import com.notifyforward.app.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val themeMode: ThemeMode       = ThemeMode.SYSTEM,
    val webhookUrl: String         = "",
    val globalTemplate: String     = "",
    val retryCount: Int            = 3,
    val filterDuplicates: Boolean  = true,
    val duplicateWindowSec: Int    = 5,
    val filterSystem: Boolean      = true,
    val filterSelf: Boolean        = true,
    val isTestingWebhook: Boolean  = false,
    val testResult: String?        = null,
    val isIgnoringBattery: Boolean = false,
    val hasNotificationListenerPermission: Boolean = false,
    val hasSmsMonitorPermissions: Boolean = false,
    val hasPhoneMonitorPermissions: Boolean = false,
    val hasQueryAllPackagesPermission: Boolean = false,
    val preventDuplicateNotifications: Boolean = true,
    val debugLogEnabled: Boolean   = false
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store     = NotifyForwardApp.from(app).configStore
    private val forwarder = WeChatForwarder()
    private val _testing  = MutableStateFlow(false)
    private val _result   = MutableStateFlow<String?>(null)
    private val _permissionRefreshTrigger = MutableStateFlow(0L)

    // 记录上一次的通知监听权限状态，用于检测变化
    private var lastNotificationListenerPermission = PermissionUtils.hasNotificationListenerPermission(app)

    val uiState: StateFlow<SettingsUiState> = combine(
        store.configFlow, _testing, _result, _permissionRefreshTrigger
    ) { cfg, testing, result, _ ->
        val currentPermission = PermissionUtils.hasNotificationListenerPermission(getApplication())
        // 检测权限变化：如果从无到有，则触发重新绑定
        if (!lastNotificationListenerPermission && currentPermission) {
            viewModelScope.launch(Dispatchers.IO) {
                com.notifyforward.app.service.NotificationMonitorService.requestRebind(getApplication())
            }
        }
        lastNotificationListenerPermission = currentPermission

        SettingsUiState(
            themeMode          = runCatching { ThemeMode.valueOf(cfg.themeMode) }.getOrDefault(ThemeMode.SYSTEM),
            webhookUrl         = cfg.webhookUrl,
            globalTemplate     = cfg.globalTemplate,
            retryCount         = cfg.retryCount,
            filterDuplicates   = cfg.filterDuplicates,
            duplicateWindowSec = (cfg.duplicateWindowMs / 1000).toInt(),
            filterSystem       = cfg.filterSystem,
            filterSelf         = cfg.filterSelf,
            isTestingWebhook   = testing,
            testResult         = result,
            isIgnoringBattery  = PermissionUtils.isIgnoringBatteryOptimizations(getApplication()),
            hasNotificationListenerPermission = currentPermission,
            hasSmsMonitorPermissions = PermissionUtils.hasSmsMonitorPermissions(getApplication()),
            hasPhoneMonitorPermissions = PermissionUtils.hasPhoneMonitorPermissions(getApplication()),
            hasQueryAllPackagesPermission = PermissionUtils.hasQueryAllPackagesPermission(getApplication()),
            preventDuplicateNotifications = cfg.preventDuplicateNotifications,
            debugLogEnabled    = cfg.debugLogEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun saveThemeMode(mode: ThemeMode)      = viewModelScope.launch { store.setThemeMode(mode) }
    fun saveWebhookUrl(url: String)         = viewModelScope.launch { store.setWebhookUrl(url.trim()) }
    fun saveGlobalTemplate(t: String)       = viewModelScope.launch { store.setGlobalTemplate(t) }
    fun saveRetryCount(c: Int)              = viewModelScope.launch { store.setRetryCount(c) }
    fun saveFilterDuplicates(e: Boolean)    = viewModelScope.launch { store.setFilterDuplicates(e) }
    fun saveDuplicateWindowSec(s: Int)      = viewModelScope.launch { store.setDuplicateWindowMs(s * 1000L) }
    fun saveFilterSystem(e: Boolean)        = viewModelScope.launch { store.setFilterSystem(e) }
    fun saveFilterSelf(e: Boolean)          = viewModelScope.launch { store.setFilterSelf(e) }
    fun savePreventDuplicateNotifications(e: Boolean) = viewModelScope.launch { store.setPreventDuplicateNotifications(e) }
    fun saveDebugLogEnabled(e: Boolean) = viewModelScope.launch { store.setDebugLogEnabled(e) }

    fun testWebhook(url: String) {
        viewModelScope.launch {
            _testing.value = true
            _result.value  = null
            _result.value  = withContext(Dispatchers.IO) {
                when (val r = forwarder.send(url, "【通知转发】Webhook 连接测试 ✅ 配置成功！")) {
                    is WeChatForwarder.ForwardResult.Success      -> "✅ 发送成功！企业微信已收到测试消息。"
                    is WeChatForwarder.ForwardResult.Failure      -> "❌ 发送失败：errcode=${r.code} ${r.message}"
                    is WeChatForwarder.ForwardResult.NetworkError -> "❌ 网络错误：${r.cause.message}"
                }
            }
            _testing.value = false
        }
    }

    fun clearTestResult() { _result.value = null }

    /**
     * 刷新权限状态（从设置页面返回后调用）
     */
    fun refreshPermissions() {
        _permissionRefreshTrigger.value = System.currentTimeMillis()
    }

    /**
     * 获取请求忽略电池优化的 Intent（直接页）
     */
    fun getRequestIgnoreBatteryOptimizationsIntent(): Intent? {
        return PermissionUtils.getRequestIgnoreBatteryOptimizationsIntent(getApplication())
    }

    /**
     * 获取打开电池优化列表的 Intent（全局列表）
     */
    fun getOpenBatteryOptimizationSettingsIntent(): Intent {
        return PermissionUtils.getOpenBatteryOptimizationSettingsIntent(getApplication())
    }

    /**
     * 获取打开应用详情设置的 Intent（用于其他权限设置）
     */
    fun getOpenAppDetailsSettingsIntent(): Intent {
        return PermissionUtils.getOpenAppDetailsSettingsIntent(getApplication())
    }

    /**
     * 获取打开应用列表权限设置的 Intent
     */
    fun getOpenAppListPermissionSettingsIntent(): Intent {
        return PermissionUtils.getOpenAppListPermissionSettingsIntent(getApplication())
    }

    /**
     * 获取打开通知监听设置的 Intent
     */
    fun getOpenNotificationListenerSettingsIntent(): Intent {
        return PermissionUtils.getOpenNotificationListenerSettingsIntent(getApplication())
    }
}
