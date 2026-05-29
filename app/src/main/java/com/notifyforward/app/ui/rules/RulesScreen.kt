package com.notifyforward.app.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notifyforward.app.data.entity.ForwardRule
import com.notifyforward.app.model.TemplateVars

// ── 主屏幕 ────────────────────────────────────────────────────────

@Composable
fun RulesScreen(vm: RulesViewModel = viewModel()) {
    val state              by vm.uiState.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ForwardRule?>(null) }
    var showTypePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    "转发规则",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

            if (state.rules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                                Icons.AutoMirrored.Default.Rule,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        Spacer(Modifier.height(12.dp))
                        Text("暂无规则", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "点击右下角按钮创建第一条转发规则",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding  = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule       = rule,
                            decodeList = vm::decodeList,
                            onToggle   = { vm.toggleRule(rule.id, it) },
                            onEdit     = { editTarget = rule; showEditor = true },
                            onDelete   = { vm.deleteRule(rule) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // 浮动按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            ExtendedFloatingActionButton(
                icon    = { Icon(Icons.Default.Add, null) },
                text    = { Text("新建规则") },
                onClick = { 
                    editTarget = null
                    showTypePicker = true 
                }
            )
        }
    }

    // 规则类型选择弹窗
    if (showTypePicker) {
        AlertDialog(
            onDismissRequest = { showTypePicker = false },
            title = { Text("选择规则类型") },
            text = {
                Column {
                    TextButton(onClick = {
                        editTarget = ForwardRule(
                            name = "新短信规则",
                            ruleType = com.notifyforward.app.data.entity.RuleType.SMS
                        )
                        showTypePicker = false
                        showEditor = true
                    }) {
                        Icon(Icons.Default.Sms, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("短信规则")
                    }
                    TextButton(onClick = {
                        editTarget = ForwardRule(
                            name = "新电话规则",
                            ruleType = com.notifyforward.app.data.entity.RuleType.PHONE,
                            phoneStatesJson = vm.encodeList(listOf(
                                com.notifyforward.app.data.entity.PhoneState.RINGING,
                                com.notifyforward.app.data.entity.PhoneState.HANGUP
                            ))
                        )
                        showTypePicker = false
                        showEditor = true
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("电话规则")
                    }
                    TextButton(onClick = {
                        editTarget = ForwardRule(
                            name = "新规则",
                            ruleType = com.notifyforward.app.data.entity.RuleType.GENERAL
                        )
                        showTypePicker = false
                        showEditor = true
                    }) {
                        Icon(Icons.AutoMirrored.Default.Rule, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("普通规则")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTypePicker = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showEditor && editTarget != null) {
        RuleEditorDialog(
            initial    = editTarget,
            decodeList = vm::decodeList,
            encodeList = vm::encodeList,
            loadApps   = vm::loadInstalledApps,
            onSave     = { vm.saveRule(it); showEditor = false },
            onDismiss  = { showEditor = false }
        )
    }
}

// ── 规则卡片 ──────────────────────────────────────────────────────

@Composable
private fun RuleCard(
    rule: ForwardRule,
    decodeList: (String) -> List<String>,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (rule.ruleType) {
                            com.notifyforward.app.data.entity.RuleType.SMS -> Icon(Icons.Default.Sms, contentDescription = "短信", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            com.notifyforward.app.data.entity.RuleType.PHONE -> Icon(Icons.Default.Phone, contentDescription = "电话", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            else -> Icon(Icons.AutoMirrored.Default.Rule, contentDescription = "普通", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    val description = when (rule.ruleType) {
                        com.notifyforward.app.data.entity.RuleType.SMS -> "转发短信"
                        com.notifyforward.app.data.entity.RuleType.PHONE -> {
                            val states = decodeList(rule.phoneStatesJson)
                            if (states.isEmpty()) "转发所有电话" else "转发 ${states.size} 种电话状态"
                        }
                        else -> {
                            val pkgs = decodeList(rule.packageNamesJson)
                            if (pkgs.isEmpty()) "监听所有 App" else "监听 ${pkgs.size} 个 App"
                        }
                    }
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }

            if (rule.titleRegex.isNotBlank() || rule.contentRegex.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        if (rule.titleRegex.isNotBlank()) {
                            Text(
                                "标题正则: ${rule.titleRegex}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        if (rule.contentRegex.isNotBlank()) {
                            Text(
                                "内容正则: ${rule.contentRegex}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("编辑")
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("删除规则") },
            text             = { Text("确认删除规则「${rule.name}」？此操作不可恢复。") },
            confirmButton    = {
                TextButton(
                    onClick = { vm_delete(onDelete); showDeleteConfirm = false },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

private fun vm_delete(onDelete: () -> Unit) = onDelete()



// ── 规则编辑对话框 ────────────────────────────────────────────────

@Composable
private fun RuleEditorDialog(
    initial: ForwardRule?,
    decodeList: (String) -> List<String>,
    encodeList: (List<String>) -> String,
    loadApps: ((List<InstalledApp>) -> Unit) -> Unit,
    onSave: (ForwardRule) -> Unit,
    onDismiss: () -> Unit
) {
    var name        by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var enabled     by remember(initial) { mutableStateOf(initial?.enabled ?: true) }
    var template    by remember(initial) { mutableStateOf(initial?.messageTemplate ?: "") }
    var priority    by remember(initial) { mutableIntStateOf(initial?.priority ?: 100) }
    val ruleType    = remember(initial) { initial?.ruleType ?: com.notifyforward.app.data.entity.RuleType.GENERAL }

    var selPkgs     by remember(initial) { mutableStateOf(decodeList(initial?.packageNamesJson ?: "[]").toMutableList()) }
    var titleRegex  by remember(initial) { mutableStateOf(initial?.titleRegex ?: "") }
    var contentRegex by remember(initial) { mutableStateOf(initial?.contentRegex ?: "") }
    var selPhoneStates by remember(initial) { mutableStateOf(decodeList(initial?.phoneStatesJson ?: "[]").toMutableList()) }

    var showAppPicker by remember { mutableStateOf(false) }
    var showVarHelp   by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }

    LaunchedEffect(Unit) { loadApps { installedApps = it } }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape    = RoundedCornerShape(20.dp),
            color    = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (initial == null) "新建规则" else "编辑规则",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier            = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 规则名称
                    OutlinedTextField(
                        value         = name,
                        onValueChange = { name = it },
                        label         = { Text("规则名称") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )

                    // 启用开关
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("启用规则", modifier = Modifier.weight(1f))
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }

                    // 规则类型说明
                    when (ruleType) {
                        com.notifyforward.app.data.entity.RuleType.SMS -> {
                            SectionLabel("此规则会转发收到的短信")
                            Text("可选正则表达式过滤", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        com.notifyforward.app.data.entity.RuleType.PHONE -> {
                            SectionLabel("选择要转发的电话状态")
                            val phoneStateOptions = listOf(
                                com.notifyforward.app.data.entity.PhoneState.RINGING to "来电响铃",
                                com.notifyforward.app.data.entity.PhoneState.ANSWERED to "电话接通",
                                com.notifyforward.app.data.entity.PhoneState.HANGUP to "电话挂断",
                                com.notifyforward.app.data.entity.PhoneState.OUTGOING to "拨出电话"
                            )
                            phoneStateOptions.forEach { (state, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selPhoneStates = if (state in selPhoneStates) {
                                                (selPhoneStates - state).toMutableList()
                                            } else {
                                                (selPhoneStates + state).toMutableList()
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = state in selPhoneStates,
                                        onCheckedChange = { checked ->
                                            selPhoneStates = if (checked) {
                                                (selPhoneStates + state).toMutableList()
                                            } else {
                                                (selPhoneStates - state).toMutableList()
                                            }
                                        }
                                    )
                                    Text(label)
                                }
                            }
                        }
                        else -> {
                            // App 包名选择（仅普通规则）
                            SectionLabel("监听 App（留空 = 全部）")
                            OutlinedButton(
                                onClick  = { showAppPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Apps, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (selPkgs.isEmpty()) "点击选择 App（不选 = 监听全部）"
                                    else "已选 ${selPkgs.size} 个 App"
                                )
                            }
                            if (selPkgs.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    selPkgs.forEach { pkg ->
                                        val label = installedApps.find { it.packageName == pkg }?.label ?: pkg
                                        FilterChip(
                                            selected     = true,
                                            onClick      = { selPkgs = (selPkgs - pkg).toMutableList() },
                                            label        = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 标题正则
                    SectionLabel("标题正则表达式（留空 = 匹配所有）")
                    OutlinedTextField(
                        value         = titleRegex,
                        onValueChange = { titleRegex = it },
                        label         = { Text("标题正则") },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = { Text("例如: 验证码|重要") },
                        singleLine    = true
                    )

                    // 内容正则
                    SectionLabel("内容正则表达式（留空 = 匹配所有）")
                    OutlinedTextField(
                        value         = contentRegex,
                        onValueChange = { contentRegex = it },
                        label         = { Text("内容正则") },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = { Text("例如: \\d{4,}") },
                        singleLine    = true
                    )

                    // 消息模板
                    SectionLabel("消息模板（留空 = 使用全局模板）")
                    OutlinedTextField(
                        value         = template,
                        onValueChange = { template = it },
                        label         = { Text("模板内容") },
                        modifier      = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        placeholder   = { Text("留空则使用全局模板，支持 {title} {content} 等变量") },
                        minLines      = 3
                    )
                    TextButton(onClick = { showVarHelp = true }) {
                        Icon(Icons.AutoMirrored.Default.Help, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("可用变量说明")
                    }
                }

                // 底部按钮
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick  = {
                            if (name.isBlank()) return@Button
                            onSave(
                                ForwardRule(
                                    id                  = initial?.id ?: 0,
                                    name                = name.trim(),
                                    enabled             = enabled,
                                    ruleType            = ruleType,
                                    packageNamesJson    = encodeList(selPkgs),
                                    titleRegex          = titleRegex.trim(),
                                    contentRegex        = contentRegex.trim(),
                                    phoneStatesJson     = encodeList(selPhoneStates),
                                    messageTemplate     = template.trim(),
                                    priority            = priority,
                                    createdAt           = initial?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        },
                        enabled = name.isNotBlank()
                    ) { Text("保存") }
                }
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            installedApps = installedApps,
            selectedPkgs  = selPkgs,
            onConfirm     = { selPkgs = it.toMutableList(); showAppPicker = false },
            onDismiss     = { showAppPicker = false }
        )
    }

    if (showVarHelp) {
        AlertDialog(
            onDismissRequest = { showVarHelp = false },
            title            = { Text("模板变量说明") },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TemplateVars.ALL.forEach { v ->
                        Row {
                            Text(
                                v,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.width(120.dp),
                                color      = MaterialTheme.colorScheme.primary,
                                style      = MaterialTheme.typography.bodySmall
                            )
                            Text(TemplateVars.DESC[v] ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVarHelp = false }) { Text("知道了") } }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style      = MaterialTheme.typography.labelMedium,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

// ── App 选择器弹窗 ────────────────────────────────────────────────

@Composable
private fun AppPickerDialog(
    installedApps: List<InstalledApp>,
    selectedPkgs: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = remember { mutableStateListOf<String>().also { it.addAll(selectedPkgs) } }
    var search   by remember { mutableStateOf("") }

    val filtered = if (search.isBlank()) installedApps
    else installedApps.filter {
        it.label.contains(search, ignoreCase = true) || it.packageName.contains(search, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape    = RoundedCornerShape(20.dp),
            color    = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "选择监听 App",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = search,
                    onValueChange = { search = it },
                    label         = { Text("搜索 App") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Default.Search, null) }
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { app ->
                        val isSelected = app.packageName in selected
                        ListItem(
                            headlineContent   = { Text(app.label) },
                            supportingContent = {
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked         = isSelected,
                                    onCheckedChange = {
                                        if (it) selected.add(app.packageName)
                                        else selected.remove(app.packageName)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (isSelected) selected.remove(app.packageName)
                                else selected.add(app.packageName)
                            }
                        )
                        HorizontalDivider()
                    }
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { selected.clear() }) { Text("清空") }
                    Row {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onConfirm(selected.toList()) }) {
                            Text("确定（${selected.size}）")
                        }
                    }
                }
            }
        }
    }
}
