package com.sqhh99.punchreminder.system.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.sqhh99.punchreminder.domain.permission.PermissionType

/**
 * 把用户引导到对应的系统设置页去开启权限（实现方案 §11）。仅使用标准 Settings Intent，
 * 不做任何自动授权/越权操作；目标页不存在时回退到应用详情页。
 */
class PermissionSettingsLauncher(private val context: Context) {

    fun open(type: PermissionType) {
        val intent = when (type) {
            PermissionType.NOTIFICATION -> notificationSettings()
            PermissionType.EXACT_ALARM -> exactAlarmSettings()
            PermissionType.FULL_SCREEN -> fullScreenSettings()
        }
        launch(intent)
    }

    private fun notificationSettings(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    private fun exactAlarmSettings(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, appUri())
        } else {
            null
        }

    private fun fullScreenSettings(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, appUri())
        } else {
            null
        }

    private fun appUri(): Uri = Uri.fromParts("package", context.packageName, null)

    private fun appDetailsSettings(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri())

    private fun launch(intent: Intent?) {
        val target = (intent ?: appDetailsSettings()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(target) }
            .onFailure {
                runCatching {
                    context.startActivity(appDetailsSettings().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
    }
}
