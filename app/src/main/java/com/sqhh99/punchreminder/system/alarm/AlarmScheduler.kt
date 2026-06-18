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
        val pendingIntent = pendingIntentFor(request.taskId, create = true)
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
        pendingIntentFor(taskId, create = false)?.let { alarmManager.cancel(it) }
    }

    private fun pendingIntentFor(taskId: String, create: Boolean): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        var flags = PendingIntent.FLAG_IMMUTABLE
        flags = flags or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, taskId.hashCode(), intent, flags)
    }
}
