package com.sqhh99.punchreminder.system.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** 通知权限检查（POST_NOTIFICATIONS 自 Android 13 起需运行时授予）。 */
object NotificationPermission {

    /** 运行时是否需要请求通知权限。 */
    fun isRequired(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun isGranted(context: Context): Boolean {
        if (!isRequired()) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permission: String
        get() = Manifest.permission.POST_NOTIFICATIONS
}
