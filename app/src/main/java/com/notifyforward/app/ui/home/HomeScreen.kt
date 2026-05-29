package com.notifyforward.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val listenerGranted = vm.isListenerEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 标题 ──────────────────────────────────────────────────
        Text(
            "通知转发",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )

        // ── 转发总开关卡片 ────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier              = Modifier.padding(20.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "转发总开关",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when {
                            !listenerGranted         -> "需先授权通知监听权限"
                            !state.webhookConfigured -> "需先配置 Webhook 地址"
                            state.isForwardingEnabled -> "运行中，监听系统通知"
                            else                     -> "已停止"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked         = state.isForwardingEnabled,
                    onCheckedChange = { vm.toggleForwarding(it) },
                    enabled         = state.webhookConfigured && listenerGranted
                )
            }
        }

        // ── 前台保活开关卡片 ────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier              = Modifier.padding(20.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "前台保活",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (state.isFgServiceEnabled && state.isFgServiceRunning) {
                            "运行正常"
                        } else if (state.isFgServiceEnabled && !state.isFgServiceRunning) {
                            "已开启，服务异常"
                        } else {
                            "未开启"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked         = state.isFgServiceEnabled,
                    onCheckedChange = { vm.toggleFgService(it) }
                )
            }
        }

        // ── 状态卡片组 ────────────────────────────────────────────
        StatusCard(
            icon     = Icons.Default.Notifications,
            title    = "通知监听权限",
            ok       = listenerGranted,
            okText   = "已授权",
            failText = "未授权，点击前往开启",
            onClick  = if (!listenerGranted) vm::openNotificationSettings else null
        )

        StatusCard(
            icon     = Icons.Default.Webhook,
            title    = "企业微信 Webhook",
            ok       = state.webhookConfigured,
            okText   = "已配置",
            failText = "未配置，请前往设置页面填写"
        )

        // ── 今日统计 ──────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier            = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "今日统计",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider()
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("成功转发")
                    Text(
                        "${state.todayCount} 条",
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.lastForwardTime != null) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("最后转发")
                        Text(
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(state.lastForwardTime!!)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── 状态卡片 ────────────────────────────────────────────────────

@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    ok: Boolean,
    okText: String,
    failText: String,
    onClick: (() -> Unit)? = null
) {
    val containerColor = if (ok)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.errorContainer

    val tintColor = if (ok)
        MaterialTheme.colorScheme.secondary
    else
        MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        onClick  = { onClick?.invoke() }
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tintColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (ok) okText else failText,
                    style = MaterialTheme.typography.bodySmall,
                    color = tintColor
                )
            }
            if (!ok && onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = tintColor)
            }
        }
    }
}
