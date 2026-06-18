package com.sqhh99.punchreminder.system.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sqhh99.punchreminder.MainActivity
import com.sqhh99.punchreminder.R
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.notification.NotificationContentBuilder
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
            "打卡提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "上下班打卡时间到点提醒"
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
                R.drawable.ic_launcher_foreground,
                "打开打卡应用",
                openTargetAppIntent(task),
            )
        }

        NotificationManagerCompat.from(context).notify(task.id.hashCode(), builder.build())
    }

    private fun contentIntent(task: PunchTask, openTargetApp: Boolean): PendingIntent {
        val launchIntent = task.targetPackage
            ?.takeIf { openTargetApp }
            ?.let { context.packageManager.getLaunchIntentForPackage(it) }
            ?: Intent(context, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            launchIntent,
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
        }
        return PendingIntent.getActivity(
            context,
            task.id.hashCode() + 2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun openTargetAppIntent(task: PunchTask): PendingIntent {
        val intent = task.targetPackage
            ?.let { context.packageManager.getLaunchIntentForPackage(it) }
            ?: Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            task.id.hashCode() + 1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_ID = "punch_reminder"
    }
}
