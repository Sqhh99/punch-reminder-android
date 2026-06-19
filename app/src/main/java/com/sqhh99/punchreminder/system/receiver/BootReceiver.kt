package com.sqhh99.punchreminder.system.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sqhh99.punchreminder.PunchReminderApp
import com.sqhh99.punchreminder.system.service.ReminderForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机 / 应用更新 / 时间变更后重新注册所有启用任务的闹钟（实现方案 §4.5）。
 *
 * Android 在重启、覆盖安装、时区/时间改变后会清空已注册的 AlarmManager 闹钟，
 * 否则已启用的提醒会全部失效。这里仅做框架黏合，真正的调度逻辑在可单测的
 * [com.sqhh99.punchreminder.domain.usecase.TaskScheduler.rescheduleAll] 中。
 *
 * 监听的均为系统受保护广播（第三方无法伪造）。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val container = (context.applicationContext as PunchReminderApp).container
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                container.taskScheduler.rescheduleAll()
                // 开机是后台启动前台服务的系统豁免场景，重排后顺带拉起保活服务。
                ReminderForegroundService.start(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )

        const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
