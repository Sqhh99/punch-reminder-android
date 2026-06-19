package com.sqhh99.punchreminder.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sqhh99.punchreminder.domain.permission.PermissionItem
import com.sqhh99.punchreminder.domain.permission.PermissionType
import com.sqhh99.punchreminder.viewmodel.PermissionUiState

const val PermissionScreenTag = "permission_screen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    state: PermissionUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: (PermissionType) -> Unit,
    onOpenAutostart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 从系统设置页返回时自动刷新状态。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.testTag(PermissionScreenTag),
        topBar = {
            TopAppBar(
                title = { Text("权限检查") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = if (state.allGranted) {
                        "全部就绪：提醒将按系统闹钟方式尽量准时触发。"
                    } else {
                        "部分权限未开启，可能影响退出应用后的准时提醒或锁屏弹出，建议逐项开启。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(state.items, key = { it.type }) { item ->
                PermissionCard(item = item, onOpenSettings = { onOpenSettings(item.type) })
            }
            item {
                // 自启动无系统标准 API，既无法在应用内授权也无法检测状态，仅提供跳转引导。
                AutostartCard(onOpenAutostart = onOpenAutostart)
            }
            item {
                Text(
                    text = "说明：主提醒使用系统闹钟方式调度，应用已常驻一条无声通知用于后台保活。\n" +
                        "国产手机（OPPO/vivo/小米/华为等）请在「自启动管理 / 允许后台活动」中允许本应用，" +
                        "并尽量不要从最近任务中划掉本应用——否则系统会强行停止应用并取消后续全部提醒。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItem, onOpenSettings: () -> Unit) {
    val info = permissionInfo(item.type)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(info.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    info.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (item.granted) "已开启" else "未开启",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (!item.granted) {
                OutlinedButton(onClick = onOpenSettings) { Text("去设置") }
            }
        }
    }
}

@Composable
private fun AutostartCard(onOpenAutostart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("自启动 / 后台运行", style = MaterialTheme.typography.titleMedium)
                Text(
                    "国产手机需在系统「自启动管理」中允许本应用，否则退出后会被系统杀掉、收不到提醒。" +
                        "系统未提供检测开关，请点击右侧手动开启。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onOpenAutostart) { Text("去开启") }
        }
    }
}

private data class PermissionInfo(val title: String, val description: String)

private fun permissionInfo(type: PermissionType): PermissionInfo = when (type) {
    PermissionType.NOTIFICATION -> PermissionInfo("通知权限", "到点发送打卡提醒通知。")
    PermissionType.EXACT_ALARM -> PermissionInfo("闹钟和提醒", "允许使用系统闹钟能力，在退出应用后仍尽量准时提醒。")
    PermissionType.FULL_SCREEN -> PermissionInfo("全屏提醒", "锁屏/息屏时弹出全屏提醒页，更不易错过。")
    PermissionType.BATTERY_OPTIMIZATION -> PermissionInfo("后台电池限制", "关系到退出应用后能否持续提醒，建议关闭电池优化并允许后台运行/自启动。")
}
