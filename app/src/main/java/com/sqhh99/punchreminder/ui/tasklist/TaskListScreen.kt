package com.sqhh99.punchreminder.ui.tasklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.ScheduleType
import com.sqhh99.punchreminder.viewmodel.TaskListItem
import com.sqhh99.punchreminder.viewmodel.TaskListUiState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

const val TaskListScreenTag = "task_list_screen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    state: TaskListUiState,
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(TaskListScreenTag),
        topBar = { TopAppBar(title = { Text("打卡提醒助手") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "新增任务")
            }
        },
    ) { innerPadding ->
        if (state.items.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = { it.task.id }) { item ->
                    TaskCard(
                        item = item,
                        onClick = { onEditTask(item.task.id) },
                        onToggleEnabled = { onToggleEnabled(item.task.id, it) },
                        onDelete = { onDelete(item.task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("还没有打卡任务", style = MaterialTheme.typography.titleMedium)
        Text(
            "点击右下角 + 新增上班/下班打卡提醒",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TaskCard(
    item: TaskListItem,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val task = item.task
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "%02d:%02d  %s".format(task.hour, task.minute, task.name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = scheduleLabel(task),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = nextTriggerLabel(task, item.nextTrigger),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Switch(checked = task.enabled, onCheckedChange = onToggleEnabled)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除任务")
            }
        }
    }
}

private fun scheduleLabel(task: PunchTask): String {
    val app = task.targetAppLabel?.let { " · 目标：$it" } ?: " · 未选择应用"
    val period = when (task.schedule.type) {
        ScheduleType.DAILY -> "每天"
        ScheduleType.WEEKDAYS -> "工作日"
        ScheduleType.CUSTOM -> task.schedule.customDays
            .sorted()
            .joinToString("、") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
            .ifEmpty { "未选择" }
    }
    return period + app
}

private fun nextTriggerLabel(task: PunchTask, next: LocalDateTime?): String {
    if (!task.enabled) return "已停用"
    if (next == null) return "无下一次提醒"
    return "下一次：" + next.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
}
