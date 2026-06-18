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
                Text(
                    text = "说明：主提醒使用系统闹钟方式调度；如果在系统设置中强行停止应用，Android 仍会取消后续提醒。",
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

private data class PermissionInfo(val title: String, val description: String)

private fun permissionInfo(type: PermissionType): PermissionInfo = when (type) {
    PermissionType.NOTIFICATION -> PermissionInfo("通知权限", "到点发送打卡提醒通知。")
    PermissionType.EXACT_ALARM -> PermissionInfo("闹钟和提醒", "允许使用系统闹钟能力，在退出应用后仍尽量准时提醒。")
    PermissionType.FULL_SCREEN -> PermissionInfo("全屏提醒", "锁屏/息屏时弹出全屏提醒页，更不易错过。")
    PermissionType.BATTERY_OPTIMIZATION -> PermissionInfo("后台电池限制", "允许后台运行，降低退出应用后提醒被系统拦截的概率。")
}
