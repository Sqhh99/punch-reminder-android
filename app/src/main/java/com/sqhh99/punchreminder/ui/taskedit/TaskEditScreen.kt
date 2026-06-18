package com.sqhh99.punchreminder.ui.taskedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sqhh99.punchreminder.domain.model.ScheduleType
import com.sqhh99.punchreminder.domain.validation.TaskValidationError
import com.sqhh99.punchreminder.viewmodel.TaskEditUiState
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

const val TaskEditScreenTag = "task_edit_screen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    state: TaskEditUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onScheduleTypeChange: (ScheduleType) -> Unit,
    onToggleCustomDay: (DayOfWeek) -> Unit,
    onPickApp: () -> Unit,
    onTestOpen: () -> Unit,
    onAutoLaunchChange: (Boolean) -> Unit,
    onLockScreenAlertChange: (Boolean) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onRepeatReminderChange: (Boolean) -> Unit,
    onReminderIntervalChange: (Int) -> Unit,
    onMaxReminderCountChange: (Int) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag(TaskEditScreenTag),
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "编辑任务" else "新增任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("任务名称") },
                isError = TaskValidationError.EMPTY_NAME in state.errors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 执行时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("执行时间", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text("%02d:%02d".format(state.hour, state.minute))
                }
            }

            // 执行周期
            Text("执行周期", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleType.entries.forEach { type ->
                    FilterChip(
                        selected = state.scheduleType == type,
                        onClick = { onScheduleTypeChange(type) },
                        label = { Text(scheduleTypeLabel(type)) },
                    )
                }
            }
            if (state.scheduleType == ScheduleType.CUSTOM) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = day in state.customDays,
                            onClick = { onToggleCustomDay(day) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                        )
                    }
                }
                if (TaskValidationError.NO_CUSTOM_DAY in state.errors) {
                    Text("请至少选择一天", color = MaterialTheme.colorScheme.error)
                }
            }

            // 目标应用
            Text("目标打卡应用", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onPickApp) {
                    Text(state.targetAppLabel ?: "选择应用")
                }
                if (state.targetPackage != null) {
                    OutlinedButton(onClick = onTestOpen) { Text("测试打开") }
                }
            }
            if (state.targetPackage != null && !state.targetInstalled) {
                Text(
                    "目标应用已卸载或无法启动，请重新选择",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // 开关
            SwitchRow("到点自动尝试打开目标应用", state.autoLaunch, onAutoLaunchChange)
            SwitchRow("锁屏强提醒（到点全屏弹出）", state.lockScreenAlert, onLockScreenAlertChange)
            SwitchRow("启用此任务", state.enabled, onEnabledChange)
            SwitchRow("重复提醒（未处理时再次提醒）", state.repeatReminder, onRepeatReminderChange)
            if (state.repeatReminder) {
                StepperRow(
                    label = "提醒间隔",
                    valueText = "${state.reminderIntervalMinutes} 分钟",
                    onMinus = { onReminderIntervalChange((state.reminderIntervalMinutes - 1).coerceAtLeast(1)) },
                    onPlus = { onReminderIntervalChange((state.reminderIntervalMinutes + 1).coerceAtMost(60)) },
                )
                StepperRow(
                    label = "最多提醒次数",
                    valueText = "${state.maxReminderCount} 次",
                    onMinus = { onMaxReminderCountChange((state.maxReminderCount - 1).coerceAtLeast(1)) },
                    onPlus = { onMaxReminderCountChange((state.maxReminderCount + 1).coerceAtMost(10)) },
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存") }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.hour,
            initialMinute = state.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.padding(end = 8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onMinus) { Text("−") }
            Text(valueText, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onPlus) { Text("+") }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun scheduleTypeLabel(type: ScheduleType): String = when (type) {
    ScheduleType.DAILY -> "每天"
    ScheduleType.WEEKDAYS -> "工作日"
    ScheduleType.CUSTOM -> "自定义"
}
