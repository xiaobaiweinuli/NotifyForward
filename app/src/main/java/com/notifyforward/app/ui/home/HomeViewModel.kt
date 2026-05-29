package com.notifyforward.app.ui.home

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notifyforward.app.NotifyForwardApp
import com.notifyforward.app.service.ForwardForegroundService
import com.notifyforward.app.service.NotificationMonitorService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isForwardingEnabled: Boolean = false,
    val isFgServiceEnabled: Boolean = false,
    val isListenerConnected: Boolean = false,
    val isFgServiceRunning: Boolean = false,
    val webhookConfigured: Boolean = false,
    val todayCount: Int = 0,
    val lastForwardTime: Long? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val notifyApp = NotifyForwardApp.from(app)
    private val configStore = notifyApp.configStore
    private val repo = notifyApp.repository

    val uiState: StateFlow<HomeUiState> = combine(
        configStore.configFlow,
        notifyApp.isListenerConnected,
        notifyApp.isForegroundServiceRunning,
        repo.todayCount,
        repo.lastForwardTime
    ) { config, listenerOn, fgOn, count, lastTime ->
        HomeUiState(
            isForwardingEnabled = config.forwardingEnabled,
            isFgServiceEnabled = config.fgServiceEnabled,
            isListenerConnected = listenerOn,
            isFgServiceRunning = fgOn,
            webhookConfigured = config.webhookUrl.isNotBlank(),
            todayCount = count,
            lastForwardTime = lastTime
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleForwarding(enabled: Boolean) {
        viewModelScope.launch {
            configStore.setForwardingEnabled(enabled)
        }
    }

    fun toggleFgService(enabled: Boolean) {
        viewModelScope.launch {
            configStore.setFgServiceEnabled(enabled)
            if (enabled) ForwardForegroundService.start(getApplication())
            else ForwardForegroundService.stop(getApplication())
        }
    }

    fun openNotificationSettings() {
        getApplication<Application>().startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun isListenerEnabled(): Boolean =
        NotificationMonitorService.isEnabled(getApplication())
}
