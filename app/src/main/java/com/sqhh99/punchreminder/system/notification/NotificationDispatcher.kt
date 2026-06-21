package com.sqhh99.punchreminder.system.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sqhh99.punchreminder.R
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.notification.NotificationContentBuilder
import com.sqhh99.punchreminder.domain.scheduler.RepeatReminderPlanner
import com.sqhh99.punchreminder.domain.usecase.NotificationGateway
import com.sqhh99.punchreminder.system.permission.NotificationPermission
import com.sqhh99.punchreminder.ui.alarm.AlarmActivity

/**
 * 发送提醒通知（实现方案 §5）。普通 + 高优先级通知；任务开启「锁屏强提醒」时附
 * Full-screen Intent，在锁屏/息屏时拉起 [AlarmActivity] 全屏提醒页（0.6.0）。
 */
class NotificationDispatcher(
    private val context: Context,
    private val contentBuilder: NotificationContentBuilder = NotificationContentBuilder(),
) : NotificationGateway {

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Punch Reminder",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "上下班打卡时间到点提醒"
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** 保活前台服务的常驻通知渠道：最低优先级、无声、无横幅（0.8.0）。 */
    fun ensureServiceChannel() {
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "提醒保活",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = "保持后台运行以确保退出应用后仍能按时提醒"
            setShowBadge(false)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** 发送任务提醒。[openTargetApp] 为 true 时点击通知直接打开目标应用，否则打开本应用。 */
    @SuppressLint("MissingPermission") // 已在上方显式校验 POST_NOTIFICATIONS
    override fun notify(task: PunchTask, openTargetApp: Boolean) {
        // 未授予通知权限时（Android 13+）直接跳过，避免无效调用。
        if (!NotificationPermission.isGranted(context)) return
        ensureChannel()
        val content = contentBuilder.build(task)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_punch_reminder)
            .setLargeIcon(ContextCompat.getDrawable(context, R.drawable.ic_launcher_fg)?.toBitmap())
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (content.useFullScreen) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(task, openTargetApp))

        if (content.useFullScreen) {
            // 锁屏/息屏时拉起全屏提醒页；屏幕亮起时系统降级为 heads-up 通知。
            builder.setFullScreenIntent(fullScreenIntent(task), true)
        }

        if (content.showOpenAction) {
            builder.addAction(
                R.drawable.ic_stat_punch_reminder,
                "打开打卡应用",
                openTargetAppIntent(task),
            )
        }

        NotificationManagerCompat.from(context).notify(task.id.hashCode(), builder.build())
    }

    /**
     * 点击通知主体：经 [NotificationActionActivity] 中转，先取消通知再启动目标，
     * 保证通知被清理（`setAutoCancel` 对操作按钮无效，且 Android 12+ 禁止广播/服务 trampoline）。
     */
    private fun contentIntent(task: PunchTask, openTargetApp: Boolean): PendingIntent {
        val action = if (openTargetApp && !task.targetPackage.isNullOrBlank()) {
            NotificationActionActivity.ACTION_OPEN_TARGET
        } else {
            NotificationActionActivity.ACTION_OPEN_MAIN
        }
        return PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            NotificationActionActivity.intent(
                context,
                notificationId = task.id.hashCode(),
                action = action,
                targetPackage = task.targetPackage,
                taskId = task.id,
                maxRepeatIndex = RepeatReminderPlanner.maxRepeatIndex(task),
            ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun fullScreenIntent(task: PunchTask): PendingIntent {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmActivity.EXTRA_TASK_ID, task.id)
            putExtra(AlarmActivity.EXTRA_TASK_NAME, task.name)
            putExtra(AlarmActivity.EXTRA_APP_LABEL, task.targetAppLabel)
            putExtra(AlarmActivity.EXTRA_TARGET_PACKAGE, task.targetPackage)
            putExtra(AlarmActivity.EXTRA_MAX_REPEAT_INDEX, RepeatReminderPlanner.maxRepeatIndex(task))
        }
        return PendingIntent.getActivity(
            context,
            task.id.hashCode() + 2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** 「打开打卡应用」操作按钮：同样经中转页，确保点击后通知被清理。 */
    private fun openTargetAppIntent(task: PunchTask): PendingIntent =
        PendingIntent.getActivity(
            context,
            task.id.hashCode() + 1,
            NotificationActionActivity.intent(
                context,
                notificationId = task.id.hashCode(),
                action = NotificationActionActivity.ACTION_OPEN_TARGET,
                targetPackage = task.targetPackage,
                taskId = task.id,
                maxRepeatIndex = RepeatReminderPlanner.maxRepeatIndex(task),
            ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val CHANNEL_ID = "punch_reminder"
        const val SERVICE_CHANNEL_ID = "punch_reminder_service"
    }
}
