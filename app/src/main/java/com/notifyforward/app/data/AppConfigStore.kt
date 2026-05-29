package com.notifyforward.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.notifyforward.app.model.AppConfig
import com.notifyforward.app.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_config")

class AppConfigStore(private val context: Context) {

    private object Keys {
        val THEME_MODE          = stringPreferencesKey("theme_mode")
        val FORWARDING_ENABLED  = booleanPreferencesKey("forwarding_enabled")
        val FG_SERVICE_ENABLED  = booleanPreferencesKey("fg_service_enabled")
        val WEBHOOK_URL         = stringPreferencesKey("webhook_url")
        val GLOBAL_TEMPLATE     = stringPreferencesKey("global_template")
        val RETRY_COUNT         = intPreferencesKey("retry_count")
        val FILTER_DUPLICATES   = booleanPreferencesKey("filter_duplicates")
        val DUPLICATE_WINDOW_MS = longPreferencesKey("duplicate_window_ms")
        val FILTER_SYSTEM       = booleanPreferencesKey("filter_system")
        val FILTER_SELF         = booleanPreferencesKey("filter_self")
        val PREVENT_DUPLICATE_NOTIFICATIONS = booleanPreferencesKey("prevent_duplicate_notifications")
        val DEBUG_LOG_ENABLED   = booleanPreferencesKey("debug_log_enabled")
    }

    private val defaultTemplate = "【{appName}】{title}\n{content}\n─── {datetime}"

    val configFlow: Flow<AppConfig> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppConfig(
                themeMode          = prefs[Keys.THEME_MODE]          ?: ThemeMode.SYSTEM.name,
                forwardingEnabled  = prefs[Keys.FORWARDING_ENABLED]  ?: false,
                fgServiceEnabled   = prefs[Keys.FG_SERVICE_ENABLED]  ?: false,
                webhookUrl         = prefs[Keys.WEBHOOK_URL]         ?: "",
                globalTemplate     = prefs[Keys.GLOBAL_TEMPLATE]     ?: defaultTemplate,
                retryCount         = prefs[Keys.RETRY_COUNT]         ?: 3,
                filterDuplicates   = prefs[Keys.FILTER_DUPLICATES]   ?: true,
                duplicateWindowMs  = prefs[Keys.DUPLICATE_WINDOW_MS] ?: 5_000L,
                filterSystem       = prefs[Keys.FILTER_SYSTEM]       ?: true,
                filterSelf         = prefs[Keys.FILTER_SELF]         ?: true,
                preventDuplicateNotifications = prefs[Keys.PREVENT_DUPLICATE_NOTIFICATIONS] ?: true,
                debugLogEnabled    = prefs[Keys.DEBUG_LOG_ENABLED]   ?: false
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setForwardingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FORWARDING_ENABLED] = enabled }
    }

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { it[Keys.WEBHOOK_URL] = url }
    }

    suspend fun setGlobalTemplate(template: String) {
        context.dataStore.edit { it[Keys.GLOBAL_TEMPLATE] = template }
    }

    suspend fun setRetryCount(count: Int) {
        context.dataStore.edit { it[Keys.RETRY_COUNT] = count }
    }

    suspend fun setFilterDuplicates(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FILTER_DUPLICATES] = enabled }
    }

    suspend fun setDuplicateWindowMs(ms: Long) {
        context.dataStore.edit { it[Keys.DUPLICATE_WINDOW_MS] = ms }
    }

    suspend fun setFilterSystem(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FILTER_SYSTEM] = enabled }
    }

    suspend fun setFilterSelf(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FILTER_SELF] = enabled }
    }

    suspend fun setPreventDuplicateNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PREVENT_DUPLICATE_NOTIFICATIONS] = enabled }
    }

    suspend fun setFgServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FG_SERVICE_ENABLED] = enabled }
    }

    suspend fun setDebugLogEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEBUG_LOG_ENABLED] = enabled }
        com.notifyforward.app.util.DebugLog.setEnabled(enabled)
    }

    suspend fun readOnce(): AppConfig = configFlow.first()
}
