package com.sqhh99.punchreminder.system.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sqhh99.punchreminder.MainActivity
import com.sqhh99.punchreminder.PunchReminderApp
import com.sqhh99.punchreminder.R
import com.sqhh99.punchreminder.system.notification.NotificationDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 常驻低优先级前台服务（0.8.0）。在国产 ROM（OPPO/vivo 等）上，应用被从最近任务划掉会进入
 * force-stopped 状态，系统随即取消其全部闹钟并拒绝唤醒，导致退出后收不到任何提醒。
 *
 * 本服务通过一条无声常驻通知保持进程存活、不进入 stopped 状态，并在被划掉时（[onTaskRemoved]）
 * 重排闹钟作为安全网，从而显著降低漏提醒的概率。属 Android 标准能力，不做保活外的任何越权操作。
 */
class ReminderForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val dispatcher = (application as PunchReminderApp).container.notificationDispatcher
        dispatcher.ensureServiceChannel()
        startForeground(ONGOING_ID, buildOngoingNotification())
        // 进程被系统回收后尽量重建服务，继续保活。
        return START_STICKY
    }

    /** 用户从最近任务划掉应用：部分 ROM 会取消闹钟，这里重排一次作为安全网。 */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val container = (application as PunchReminderApp).container
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { container.taskScheduler.rescheduleAll() }
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun buildOngoingNotification() =
        NotificationCompat.Builder(this, NotificationDispatcher.SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_punch_reminder)
            .setContentTitle("打卡提醒运行中")
            .setContentText("保持提醒按时送达，请勿在最近任务中划掉本应用")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(openAppIntent())
            .build()

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this,
            ONGOING_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        private const val ONGOING_ID = 1001

        /**
         * 启动保活服务。仅应在前台场景（[MainActivity] onCreate）或系统豁免场景（开机广播）调用——
         * 用 runCatching 包裹，避免 Android 12+ 后台启动 FGS 抛 ForegroundServiceStartNotAllowedException
         * 时崩溃。
         */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ReminderForegroundService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ReminderForegroundService::class.java))
        }
    }
}
