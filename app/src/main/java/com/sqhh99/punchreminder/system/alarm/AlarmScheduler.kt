package com.sqhh99.punchreminder.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequest
import com.sqhh99.punchreminder.domain.usecase.AlarmGateway
import com.sqhh99.punchreminder.system.receiver.AlarmReceiver

/**
 * 封装 AlarmManager（实现方案 §5）。到点通过 [AlarmReceiver] 广播触发。
 *
 * 系统允许精确闹钟时用 setExactAndAllowWhileIdle，否则降级为 setAndAllowWhileIdle，
 * 不承诺所有机型都能精确/后台唤醒（§20）。
 */
class AlarmScheduler(private val context: Context) : AlarmGateway {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    override fun schedule(request: AlarmScheduleRequest) {
        val pendingIntent = createPendingIntent(request.taskId, request.repeatIndex)
        val useExact = request.exact && canScheduleExact()
        if (useExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                request.triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                request.triggerAtMillis,
                pendingIntent,
            )
        }
    }

    override fun cancel(taskId: String) {
        // 取消当天日常闹钟（repeatIndex=0）。残余的重复闹钟到点会被 ReminderTriggerHandler
        // 的启用/激活日校验拦截跳过，无需逐个取消。
        existingPendingIntent(taskId)?.let { alarmManager.cancel(it) }
    }

    private fun requestCode(taskId: String, repeatIndex: Int): Int = taskId.hashCode() + repeatIndex

    private fun createPendingIntent(taskId: String, repeatIndex: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(taskId, repeatIndex),
            broadcastIntent(taskId, repeatIndex),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun existingPendingIntent(taskId: String): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode(taskId, 0),
            broadcastIntent(taskId, 0),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )

    private fun broadcastIntent(taskId: String, repeatIndex: Int): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(AlarmReceiver.EXTRA_REPEAT_INDEX, repeatIndex)
        }
}
