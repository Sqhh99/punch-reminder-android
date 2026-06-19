package com.sqhh99.punchreminder

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqhh99.punchreminder.di.AppContainer
import com.sqhh99.punchreminder.system.launcher.LaunchResult
import com.sqhh99.punchreminder.system.service.ReminderForegroundService
import com.sqhh99.punchreminder.system.permission.NotificationPermission
import com.sqhh99.punchreminder.ui.apppicker.AppPickerScreen
import com.sqhh99.punchreminder.ui.permission.PermissionScreen
import com.sqhh99.punchreminder.ui.taskedit.TaskEditScreen
import com.sqhh99.punchreminder.ui.tasklist.TaskListScreen
import com.sqhh99.punchreminder.ui.theme.PunchReminderTheme
import com.sqhh99.punchreminder.viewmodel.AppPickerViewModel
import com.sqhh99.punchreminder.viewmodel.PermissionViewModel
import com.sqhh99.punchreminder.viewmodel.TaskEditViewModel
import com.sqhh99.punchreminder.viewmodel.TaskListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PunchReminderApp).container
        // 应用在前台时启动保活前台服务（前台场景启动 FGS 必定被允许）。
        ReminderForegroundService.start(this)
        setContent {
            PunchReminderTheme {
                AppRoot(container)
            }
        }
    }
}

private sealed interface Route {
    data object Home : Route
    data class Edit(val taskId: String?) : Route
    data object Permissions : Route
}

@Composable
private fun AppRoot(container: AppContainer) {
    val context = LocalContext.current

    // 请求通知权限（Android 13+）。
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 结果忽略；完整权限诊断页见 0.7.0 */ }
    LaunchedEffect(Unit) {
        if (NotificationPermission.isRequired() && !NotificationPermission.isGranted(context)) {
            permissionLauncher.launch(NotificationPermission.permission)
        }
    }

    var route by remember { mutableStateOf<Route>(Route.Home) }

    when (val current = route) {
        is Route.Home -> {
            val vm: TaskListViewModel = viewModel(factory = container.taskListFactory())
            val state by vm.uiState.collectAsState()
            TaskListScreen(
                state = state,
                onAddTask = { route = Route.Edit(null) },
                onEditTask = { route = Route.Edit(it) },
                onToggleEnabled = vm::toggleEnabled,
                onDelete = vm::delete,
                onOpenPermissions = { route = Route.Permissions },
            )
        }

        is Route.Edit -> {
            EditFlow(
                container = container,
                taskId = current.taskId,
                onDone = { route = Route.Home },
            )
        }

        is Route.Permissions -> {
            val permVm: PermissionViewModel = viewModel(factory = container.permissionFactory())
            val permState by permVm.uiState.collectAsState()
            PermissionScreen(
                state = permState,
                onBack = { route = Route.Home },
                onRefresh = permVm::refresh,
                onOpenSettings = { container.permissionSettingsLauncher.open(it) },
            )
        }
    }
}

@Composable
private fun EditFlow(
    container: AppContainer,
    taskId: String?,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    // 把编辑页 ViewModel 的生命周期绑定到本编辑会话：离开编辑页即清理，
    // 避免 Activity 级缓存导致再次进入时拿到上次「已保存」的陈旧状态而异常退出。
    val editOwner = remember(taskId) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(editOwner) {
        onDispose { editOwner.viewModelStore.clear() }
    }
    val editVm: TaskEditViewModel = viewModel(
        viewModelStoreOwner = editOwner,
        factory = container.taskEditFactory(taskId),
    )
    val state by editVm.uiState.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    if (showPicker) {
        val pickerVm: AppPickerViewModel = viewModel(factory = container.appPickerFactory())
        val pickerState by pickerVm.uiState.collectAsState()
        AppPickerScreen(
            state = pickerState,
            onQueryChange = pickerVm::setQuery,
            onPick = { app ->
                editVm.setTargetApp(app.packageName, app.label)
                showPicker = false
            },
            iconProvider = { container.installedAppProvider.iconFor(it) },
            onBack = { showPicker = false },
        )
    } else {
        TaskEditScreen(
            state = state,
            onBack = onDone,
            onNameChange = editVm::setName,
            onTimeChange = editVm::setTime,
            onScheduleTypeChange = editVm::setScheduleType,
            onToggleCustomDay = editVm::toggleCustomDay,
            onPickApp = { showPicker = true },
            onTestOpen = {
                val result = container.appLauncher.launch(state.targetPackage)
                val msg = when (result) {
                    LaunchResult.SUCCESS -> "已尝试打开目标应用"
                    LaunchResult.NOT_INSTALLED, LaunchResult.NO_LAUNCH_INTENT ->
                        "目标应用未安装或无法启动，请重新选择"
                    LaunchResult.FAILED -> "打开失败，请重试"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onAutoLaunchChange = editVm::setAutoLaunch,
            onLockScreenAlertChange = editVm::setLockScreenAlert,
            onEnabledChange = editVm::setEnabled,
            onRepeatReminderChange = editVm::setRepeatReminder,
            onReminderIntervalChange = editVm::setReminderInterval,
            onMaxReminderCountChange = editVm::setMaxReminderCount,
            onSave = editVm::save,
        )
    }
}
