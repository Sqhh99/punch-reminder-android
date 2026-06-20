package com.sqhh99.punchreminder.ui.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.sqhh99.punchreminder.PunchReminderApp
import com.sqhh99.punchreminder.system.launcher.LaunchResult
import com.sqhh99.punchreminder.ui.theme.PunchReminderTheme

/**
 * 锁屏全屏提醒页（实现方案 §5）。由通知的 Full-screen Intent 在锁屏/息屏时拉起，
 * 越过锁屏并点亮屏幕，引导用户一键打开打卡应用。展示字段经 intent extras 传入，
 * 避免在锁屏关键路径做异步仓库读取。
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME).orEmpty()
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        val maxRepeatIndex = intent.getIntExtra(EXTRA_MAX_REPEAT_INDEX, 0)

        setContent {
            PunchReminderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmContent(
                        taskName = taskName,
                        appLabel = appLabel,
                        canOpen = !targetPackage.isNullOrBlank(),
                        onOpen = {
                            openTargetApp(targetPackage)
                            dismiss(taskId, maxRepeatIndex)
                        },
                        onDismiss = { dismiss(taskId, maxRepeatIndex) },
                    )
                }
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun openTargetApp(targetPackage: String?) {
        val result = (application as PunchReminderApp).container.appLauncher.launch(targetPackage)
        if (result != LaunchResult.SUCCESS) {
            Toast.makeText(this, "目标应用未安装或无法启动，请在应用内重新选择", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dismiss(taskId: String?, maxRepeatIndex: Int) {
        taskId?.let {
            NotificationManagerCompat.from(this).cancel(it.hashCode())
            // 用户已确认提醒：取消今日剩余重复提醒闹钟，避免确认后仍反复弹出。
            (application as PunchReminderApp).container.alarmScheduler.cancelRepeats(it, maxRepeatIndex)
        }
        finish()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_APP_LABEL = "app_label"
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_MAX_REPEAT_INDEX = "max_repeat_index"
    }
}

@Composable
private fun AlarmContent(
    taskName: String,
    appLabel: String?,
    canOpen: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "该打卡啦",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = taskName,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        val hint = when {
            appLabel != null -> "点击下方按钮打开「$appLabel」完成打卡"
            else -> "请打开打卡应用完成打卡"
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )

        if (canOpen) {
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
            ) { Text("打开打卡应用") }
        }
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) { Text("我知道了") }
    }
}
