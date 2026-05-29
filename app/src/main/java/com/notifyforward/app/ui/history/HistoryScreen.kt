package com.notifyforward.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notifyforward.app.data.entity.ForwardHistory
import com.notifyforward.app.data.entity.ForwardStatus
import com.notifyforward.app.model.NotificationSource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: HistoryViewModel = viewModel()) {
    val state              by vm.uiState.collectAsStateWithLifecycle()
    var showClearConfirm   by remember { mutableStateOf(false) }
    var selectedItem       by remember { mutableStateOf<ForwardHistory?>(null) }
    val bottomSheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filters = listOf(
        null                   to "全部",
        ForwardStatus.SUCCESS  to "成功",
        ForwardStatus.FAILED   to "失败",
        ForwardStatus.SKIPPED  to "已过滤",
        ForwardStatus.RECEIVED to "已接收"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "转发记录",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { showClearConfirm = true }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "清空记录")
            }
        }

        Spacer(Modifier.height(16.dp))

        // 搜索框
        OutlinedTextField(
            value         = state.searchQuery,
            onValueChange = vm::setSearchQuery,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("搜索 App 或标题") },
            leadingIcon   = { Icon(Icons.Default.Search, null) },
            singleLine    = true
        )

        Spacer(Modifier.height(12.dp))

        // 状态过滤 Chip
        Row(
            modifier            = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (key, label) ->
                FilterChip(
                    selected = state.statusFilter == key,
                    onClick  = { vm.setStatusFilter(key) },
                    label    = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    HistoryCard(
                        item = item,
                        onClick = { selectedItem = item }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title            = { Text("清空记录") },
            text             = { Text("确认清空所有转发记录？") },
            confirmButton    = {
                TextButton(
                    onClick = { vm.clearHistory(); showClearConfirm = false },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消") } }
        )
    }

    if (selectedItem != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = bottomSheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "通知详情",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(16.dp))
                
                // 详情内容
                HistoryDetailContent(selectedItem!!)
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── 历史记录卡片 ──────────────────────────────────────────────────

@Composable
private fun HistoryCard(item: ForwardHistory, onClick: () -> Unit) {
    val (statusColor, statusIcon, statusLabel) = when (item.status) {
        ForwardStatus.SUCCESS  -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle, "成功")
        ForwardStatus.FAILED   -> Triple(MaterialTheme.colorScheme.error,   Icons.Default.Error,       "失败")
        ForwardStatus.SKIPPED   -> Triple(MaterialTheme.colorScheme.outline, Icons.Default.FilterAlt,   "已过滤")
        ForwardStatus.RECEIVED -> Triple(MaterialTheme.colorScheme.tertiary, Icons.Default.Notifications, "已接收")
        else                   -> Triple(MaterialTheme.colorScheme.outline, Icons.Default.FilterAlt,   "未知")
    }
    
    val (sourceLabel, sourceIcon) = when (item.source) {
        NotificationSource.SMS_BROADCAST -> Pair("短信广播", Icons.Default.Sms)
        NotificationSource.PHONE_BROADCAST -> Pair("电话广播", Icons.Default.Phone)
        else -> Pair("通知服务", Icons.Default.Notifications)
    }
    
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        onClick  = onClick
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 状态行
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    tint     = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = statusColor)
                Spacer(Modifier.width(8.dp))
                Icon(
                    sourceIcon,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(sourceLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.weight(1f))
                Text(
                    fmt.format(Date(item.forwardTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 标题行
            Text(
                "[${item.appName}] ${item.notifTitle}",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )

            // 正文
            if (item.notifContent.isNotBlank()) {
                Text(
                    item.notifContent,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 失败原因
            if (item.status == ForwardStatus.FAILED && item.errorMsg.isNotBlank()) {
                Text(
                    "错误：${item.errorMsg}",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 显示状态原因
            if (item.status == ForwardStatus.RECEIVED && item.errorMsg.isNotBlank()) {
                Text(
                    "原因: ${item.errorMsg}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 匹配规则
            else if (item.matchedRuleName.isNotBlank()) {
                Text(
                    "规则: ${item.matchedRuleName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── 历史记录详情内容 ───────────────────────────────────────────────

@Composable
private fun HistoryDetailContent(item: ForwardHistory) {
    val (statusColor, statusLabel) = when (item.status) {
        ForwardStatus.SUCCESS  -> Pair(MaterialTheme.colorScheme.primary, "成功")
        ForwardStatus.FAILED   -> Pair(MaterialTheme.colorScheme.error,   "失败")
        ForwardStatus.SKIPPED   -> Pair(MaterialTheme.colorScheme.outline, "已过滤")
        ForwardStatus.RECEIVED -> Pair(MaterialTheme.colorScheme.tertiary, "已接收")
        else                   -> Pair(MaterialTheme.colorScheme.outline, "未知")
    }
    
    val sourceLabel = when (item.source) {
        NotificationSource.SMS_BROADCAST -> "短信广播"
        NotificationSource.PHONE_BROADCAST -> "电话广播"
        else -> "通知服务"
    }
    
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 应用信息
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("应用:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(item.appName, style = MaterialTheme.typography.bodyMedium)
        }
        
        // 来源
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("来源:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(sourceLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("包名:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(
                item.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 状态
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("状态:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(statusLabel, style = MaterialTheme.typography.bodyMedium, color = statusColor)
        }
        
        // 时间
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("时间:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(
                fmt.format(Date(item.forwardTime)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 标题
        if (item.notifTitle.isNotBlank()) {
            Row(verticalAlignment = Alignment.Top) {
                Text("标题:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(
                    item.notifTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // 正文
        if (item.notifContent.isNotBlank()) {
            Row(verticalAlignment = Alignment.Top) {
                Text("内容:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(
                    item.notifContent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // 匹配规则
        if (item.matchedRuleName.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("规则:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(
                    item.matchedRuleName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // 错误原因
        if (item.errorMsg.isNotBlank()) {
            Row(verticalAlignment = Alignment.Top) {
                Text("原因:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(
                    item.errorMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.status == ForwardStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Webhook 提示
        if (item.webhookHint.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Webhook:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "***${item.webhookHint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
