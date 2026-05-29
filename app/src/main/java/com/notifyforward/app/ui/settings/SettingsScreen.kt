package com.notifyforward.app.ui.settings

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.notifyforward.app.model.TemplateVars
import com.notifyforward.app.model.ThemeMode
import com.notifyforward.app.util.PermissionUtils

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var localWebhook by remember(state.webhookUrl) { mutableStateOf(state.webhookUrl) }
    var localTemplate by remember(state.globalTemplate) { mutableStateOf(state.globalTemplate) }
    var showWebhook by remember { mutableStateOf(false) }
    var showVarHelp by remember { mutableStateOf(false) }

    // 短信监听权限状态
    val smsPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
    )

    // 电话监听权限状态
    val phonePermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )
    )

    // 通用权限请求启动器（用于跳转系统设置页面）
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        vm.refreshPermissions()
    }

    // 测试结果 6 秒后自动清除
    LaunchedEffect(state.testResult) {
        if (state.testResult != null) {
            kotlinx.coroutines.delay(6_000)
            vm.clearTestResult()
        }
    }

    // 监听权限状态变化并刷新
    LaunchedEffect(
        smsPermissionsState.allPermissionsGranted,
        phonePermissionsState.allPermissionsGranted
    ) {
        vm.refreshPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            "设置",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

            // ── 权限设置 ──────────────────────────────────────────────
            SettingsSection("权限设置") {
                // 通知监听权限
                PermissionCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "通知监听权限",
                    subtitle = "核心权限，用于读取和转发系统通知",
                    ok = state.hasNotificationListenerPermission,
                    okText = "已授权",
                    failText = "点击前往开启",
                    onClick = {
                        settingsLauncher.launch(vm.getOpenNotificationListenerSettingsIntent())
                    }
                )

                HorizontalDivider()

                // 打开电池优化列表（全局列表）
                PermissionCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "忽略电池优化",
                    subtitle = "防止系统杀死后台服务",
                    ok = state.isIgnoringBattery,
                    okText = "已添加白名单",
                    failText = "点击前往设置",
                    onClick = {
                        settingsLauncher.launch(vm.getOpenBatteryOptimizationSettingsIntent())
                    }
                )

                HorizontalDivider()

                // 短信监听
                PermissionCard(
                    icon = Icons.Default.Sms,
                    title = "监听短信",
                    subtitle = "有权限时直接通过广播接收（比通知更快）",
                    ok = state.hasSmsMonitorPermissions,
                    okText = "权限已授予",
                    failText = "点击申请",
                    onClick = {
                        if (smsPermissionsState.shouldShowRationale) {
                            // 用户之前拒绝过，跳转到设置
                            settingsLauncher.launch(vm.getOpenAppDetailsSettingsIntent())
                        } else {
                            // 直接弹出系统授权对话框
                            smsPermissionsState.launchMultiplePermissionRequest()
                        }
                    }
                )

                HorizontalDivider()

                // 电话监听
                PermissionCard(
                    icon = Icons.Default.Phone,
                    title = "监听电话",
                    subtitle = "有权限时直接通过广播监听来电、接通和挂断事件",
                    ok = state.hasPhoneMonitorPermissions,
                    okText = "权限已授予",
                    failText = "点击申请",
                    onClick = {
                        if (phonePermissionsState.shouldShowRationale) {
                            // 用户之前拒绝过，跳转到设置
                            settingsLauncher.launch(vm.getOpenAppDetailsSettingsIntent())
                        } else {
                            // 直接弹出系统授权对话框
                            phonePermissionsState.launchMultiplePermissionRequest()
                        }
                    }
                )

                HorizontalDivider()

                // 获取应用列表权限
                PermissionCard(
                    icon = Icons.Default.Apps,
                    title = "获取应用列表",
                    subtitle = "用于规则设置时选择应用包名（Android 11+ 需要）",
                    ok = state.hasQueryAllPackagesPermission,
                    okText = "权限已授予",
                    failText = "点击前往开启",
                    onClick = {
                        settingsLauncher.launch(vm.getOpenAppListPermissionSettingsIntent())
                    }
                )
            }

            // ── 外观 ───────────────────────────────────────────────
            SettingsSection("外观") {
                Text(
                    "主题模式",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val (label, icon) = when (mode) {
                            ThemeMode.SYSTEM -> "跟随系统" to Icons.Default.SettingsBrightness
                            ThemeMode.LIGHT -> "浅色" to Icons.Default.LightMode
                            ThemeMode.DARK -> "深色" to Icons.Default.DarkMode
                        }
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { vm.saveThemeMode(mode) },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── 企业微信 Webhook ───────────────────────────────────
            SettingsSection("企业微信 Webhook") {
                OutlinedTextField(
                    value = localWebhook,
                    onValueChange = { localWebhook = it },
                    label = { Text("Webhook 地址") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=…") },
                    visualTransformation = if (showWebhook) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showWebhook = !showWebhook }) {
                            Icon(
                                if (showWebhook) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    minLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.saveWebhookUrl(localWebhook) },
                        enabled = localWebhook.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("保存")
                    }
                    Button(
                        onClick = { vm.testWebhook(localWebhook) },
                        enabled = localWebhook.isNotBlank() && !state.isTestingWebhook
                    ) {
                        if (state.isTestingWebhook) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Default.Send, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("测试发送")
                    }
                }
                // 测试结果提示
                if (state.testResult != null) {
                    val isOk = state.testResult!!.startsWith("✅")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOk) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            state.testResult!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOk) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ── 全局消息模板 ───────────────────────────────────────
            SettingsSection("全局消息模板") {
                OutlinedTextField(
                    value = localTemplate,
                    onValueChange = { localTemplate = it },
                    label = { Text("模板内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    minLines = 4
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showVarHelp = true }) {
                        Icon(Icons.AutoMirrored.Default.Help, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("变量说明")
                    }
                    TextButton(onClick = {
                        localTemplate = "【{appName}】{title}\n{content}\n─── {datetime}"
                    }) {
                        Text("恢复默认")
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { vm.saveGlobalTemplate(localTemplate) }) {
                        Text("保存")
                    }
                }
            }

            // ── 转发策略 ───────────────────────────────────────────
            SettingsSection("转发策略") {
                SwitchRow(
                    title = "去重过滤",
                    subtitle = "相同内容短时间内只转发一次",
                    checked = state.filterDuplicates,
                    onCheckedChange = vm::saveFilterDuplicates
                )

                if (state.filterDuplicates) {
                    Column {
                        Text(
                            "去重时间窗口：${state.duplicateWindowSec} 秒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = state.duplicateWindowSec.toFloat(),
                            onValueChange = { vm.saveDuplicateWindowSec(it.toInt()) },
                            valueRange = 1f..60f,
                            steps = 58
                        )
                    }
                }

                HorizontalDivider()

                SwitchRow(
                    title = "防止重复通知",
                    subtitle = "开启短信/电话监听时，自动过滤对应应用通知",
                    checked = state.preventDuplicateNotifications,
                    onCheckedChange = vm::savePreventDuplicateNotifications
                )

                HorizontalDivider()

                // 重试次数
                val retryLabels = listOf("0 次", "1 次", "2 次", "3 次", "5 次")
                val retryValues = listOf(0, 1, 2, 3, 5)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("失败重试次数", modifier = Modifier.weight(1f))
                    DropdownMenuBox(
                        options = retryLabels,
                        selected = retryLabels.getOrElse(retryValues.indexOf(state.retryCount)) { "3 次" },
                        onSelect = { i -> vm.saveRetryCount(retryValues[i]) }
                    )
                }

                HorizontalDivider()

                SwitchRow(
                    title = "过滤系统通知",
                    subtitle = "跳过 android / com.android.* 等系统包通知",
                    checked = state.filterSystem,
                    onCheckedChange = vm::saveFilterSystem
                )
                SwitchRow(
                    title = "过滤本 App 通知",
                    subtitle = "不转发通知转发自身产生的通知",
                    checked = state.filterSelf,
                    onCheckedChange = vm::saveFilterSelf
                )

                HorizontalDivider()

                SwitchRow(
                    title = "Debug 日志",
                    subtitle = "开启后在 Logcat 中输出详细调试日志",
                    checked = state.debugLogEnabled,
                    onCheckedChange = vm::saveDebugLogEnabled
                )
            }
    }

    // 变量说明弹窗
    if (showVarHelp) {
        AlertDialog(
            onDismissRequest = { showVarHelp = false },
            title = { Text("模板变量说明") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TemplateVars.ALL.forEach { v ->
                        Row {
                            Text(
                                v,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(130.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                TemplateVars.DESC[v] ?: "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVarHelp = false }) { Text("知道了") }
            }
        )
    }
}

// ── 可复用组件 ──────────────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DropdownMenuBox(
    options: List<String>,
    selected: String,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(i); expanded = false }
                )
            }
        }
    }
}

/**
 * 权限卡片组件
 */
@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tintColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (okText.isNotEmpty() || failText.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (ok) okText else failText,
                        style = MaterialTheme.typography.bodySmall,
                        color = tintColor
                    )
                }
            }
            if (!ok && onClick != null) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = tintColor)
            }
        }
    }
}
