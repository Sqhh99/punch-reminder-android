package com.sqhh99.punchreminder.system.permission

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * 全屏提醒（Full-screen Intent）权限检查（实现方案 §5 锁屏提醒）。
 *
 * Android 14（API 34）起，非闹钟/来电类应用默认不再被授予 USE_FULL_SCREEN_INTENT，
 * 需用户在设置中开启；未开启时系统会把全屏 Intent 自动降级为 heads-up 通知，不会崩溃。
 * 本类用于降级判断与后续（0.7.0）权限诊断页复用。
 */
object FullScreenIntentPermission {

    fun canUse(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.canUseFullScreenIntent()
    }
}
