package com.sqhh99.punchreminder.ui.tasklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqhh99.punchreminder.ui.theme.PunchReminderTheme

/**
 * 占位任务列表页（里程碑一）。
 *
 * 真正的任务列表、新增/编辑、启用停用等能力将在里程碑二随 ViewModel 与 DataStore 实现。
 * 这里仅展示空状态提示，并打上稳定的 testTag 供后续 UI 测试引用。
 */
const val TaskListScreenTag = "task_list_screen"

@Composable
fun TaskListScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(TaskListScreenTag)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "打卡提醒助手",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "工程骨架已就绪。任务管理将在下一个里程碑实现。",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListScreenPreview() {
    PunchReminderTheme {
        TaskListScreen()
    }
}
