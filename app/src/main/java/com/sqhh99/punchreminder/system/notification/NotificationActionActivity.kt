package com.sqhh99.punchreminder.system.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import com.sqhh99.punchreminder.MainActivity

/**
 * 通知点击中转页（无 UI，瞬时 finish）。
 *
 * 用途：保证「点击通知/操作按钮后通知被清理」。`setAutoCancel(true)` 只对通知主体点击生效、对操作按钮无效；
 * 而 Android 12+ 禁止「通知 trampoline」——不能从被通知拉起的 BroadcastReceiver/Service 里 startActivity。
 * 因此用一个 Activity 中转：先取消通知，再启动目标（目标 App 或本应用主界面），随即结束自身。
 * Activity 中转不在 trampoline 禁令范围内，可在 Android 12+ 正常拉起目标。
 */
class NotificationActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            NotificationManagerCompat.from(this).cancel(notificationId)
        }

        val action = intent.getStringExtra(EXTRA_ACTION)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        startTarget(action, targetPackage)

        finish()
    }

    private fun startTarget(action: String?, targetPackage: String?) {
        val launchIntent = targetPackage
            ?.takeIf { action == ACTION_OPEN_TARGET }
            ?.let { packageManager.getLaunchIntentForPackage(it) }
            ?: Intent(this, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(launchIntent) }
            .onFailure {
                // 目标 App 启动失败时回退到本应用主界面。
                runCatching {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_ACTION = "action"
        const val EXTRA_TARGET_PACKAGE = "target_package"

        const val ACTION_OPEN_TARGET = "open_target"
        const val ACTION_OPEN_MAIN = "open_main"

        /** 构建指向本中转页的 Intent。 */
        fun intent(
            context: Context,
            notificationId: Int,
            action: String,
            targetPackage: String?,
        ): Intent = Intent(context, NotificationActionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_ACTION, action)
            putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
        }
    }
}
