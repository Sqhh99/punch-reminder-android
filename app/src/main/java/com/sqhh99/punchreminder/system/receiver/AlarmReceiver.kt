package com.sqhh99.punchreminder.system.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sqhh99.punchreminder.PunchReminderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 接收 AlarmManager 到点广播，交给 [com.sqhh99.punchreminder.domain.usecase.ReminderTriggerHandler]
 * 处理（实现方案 §16）。用 goAsync 在协程中读取仓库并发通知/重排。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val container = (context.applicationContext as PunchReminderApp).container
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                container.reminderTriggerHandler.handle(taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER = "com.sqhh99.punchreminder.action.ALARM_TRIGGER"
        const val EXTRA_TASK_ID = "task_id"
    }
}
