package com.sqhh99.punchreminder.system.permission

import android.content.Context
import android.os.PowerManager

/** 电池优化状态检查；白名单可降低后台提醒被厂商系统延后或拦截的概率。 */
object BatteryOptimizationPermission {

    fun isIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
