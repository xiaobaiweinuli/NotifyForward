package com.notifyforward.app.ui.rules

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notifyforward.app.NotifyForwardApp
import com.notifyforward.app.data.entity.ForwardRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InstalledApp(val packageName: String, val label: String)

data class RulesUiState(
    val rules: List<ForwardRule> = emptyList()
)

class RulesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo     = NotifyForwardApp.from(app).repository
    private val pm       = app.packageManager
    private val gson     = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    val uiState: StateFlow<RulesUiState> = repo.observeRules()
        .map { RulesUiState(rules = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    fun loadInstalledApps(onDone: (List<InstalledApp>) -> Unit) {
        viewModelScope.launch {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
                .sortedBy { it.label }
            onDone(apps)
        }
    }

    fun saveRule(rule: ForwardRule) {
        viewModelScope.launch {
            if (rule.id == 0L) repo.insertRule(rule)
            else repo.updateRule(rule)
        }
    }

    fun deleteRule(rule: ForwardRule) {
        viewModelScope.launch { repo.deleteRule(rule) }
    }

    fun toggleRule(id: Long, enabled: Boolean) {
        viewModelScope.launch { repo.setRuleEnabled(id, enabled) }
    }

    fun decodeList(json: String): List<String> =
        runCatching { gson.fromJson<List<String>>(json, listType) }.getOrDefault(emptyList())

    fun encodeList(list: List<String>): String = gson.toJson(list)
}
