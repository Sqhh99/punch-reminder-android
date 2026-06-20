package com.sqhh99.punchreminder.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sqhh99.punchreminder.MainActivity
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequest
import com.sqhh99.punchreminder.domain.usecase.AlarmGateway
import com.sqhh99.punchreminder.system.receiver.AlarmReceiver

/**
 * 封装 AlarmManager（实现方案 §5）。到点通过 [AlarmReceiver] 广播触发。
 *
 * 首次提醒使用 setAlarmClock，以系统闹钟级别触发；重复提醒继续使用
 * setExactAndAllowWhileIdle / setAndAllowWhileIdle，避免占用系统下一次闹钟入口。
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
        if (request.alarmClock) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(request.triggerAtMillis, showIntent(request.taskId)),
                pendingIntent,
            )
            return
        }
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

    override fun cancelRepeats(taskId: String, maxRepeatIndex: Int) {
        // 用户确认提醒后调用：取消今日剩余的重复闹钟（repeatIndex 1..maxRepeatIndex）。
        // 从 1 开始，保护 repeatIndex=0 的次日日常闹钟不被误删；取消幂等，不存在则跳过。
        for (repeatIndex in 1..maxRepeatIndex) {
            existingRepeatPendingIntent(taskId, repeatIndex)?.let { alarmManager.cancel(it) }
        }
    }

    private fun requestCode(taskId: String, repeatIndex: Int): Int = taskId.hashCode() + repeatIndex

    private fun createPendingIntent(taskId: String, repeatIndex: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(taskId, repeatIndex),
            broadcastIntent(taskId, repeatIndex),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun showIntent(taskId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            taskId.hashCode() + SHOW_INTENT_OFFSET,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun existingPendingIntent(taskId: String): PendingIntent? =
        existingRepeatPendingIntent(taskId, 0)

    private fun existingRepeatPendingIntent(taskId: String, repeatIndex: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode(taskId, repeatIndex),
            broadcastIntent(taskId, repeatIndex),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )

    private fun broadcastIntent(taskId: String, repeatIndex: Int): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(AlarmReceiver.EXTRA_REPEAT_INDEX, repeatIndex)
        }

    private companion object {
        const val SHOW_INTENT_OFFSET = 10_000
    }
}
