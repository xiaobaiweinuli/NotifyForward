package com.notifyforward.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notifyforward.app.NotifyForwardApp
import com.notifyforward.app.data.entity.ForwardHistory
import com.notifyforward.app.data.entity.ForwardStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val items: List<ForwardHistory> = emptyList(),
    val statusFilter: String?       = null,   // null = 全部
    val searchQuery: String         = ""
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo          = NotifyForwardApp.from(app).repository
    private val _statusFilter = MutableStateFlow<String?>(null)
    private val _searchQuery  = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> = combine(
        repo.observeHistory(), _statusFilter, _searchQuery
    ) { all, filter, query ->
        val filtered = all
            .filter { if (filter != null) it.status == filter else true }
            .filter {
                if (query.isBlank()) true
                else it.appName.contains(query, ignoreCase = true) ||
                     it.notifTitle.contains(query, ignoreCase = true)
            }
        HistoryUiState(items = filtered, statusFilter = filter, searchQuery = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setStatusFilter(f: String?) { _statusFilter.value = f }
    fun setSearchQuery(q: String)   { _searchQuery.value  = q }

    fun clearHistory() {
        viewModelScope.launch { repo.clearHistory() }
    }
}
