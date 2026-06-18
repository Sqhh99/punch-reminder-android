package com.sqhh99.punchreminder.domain.permission

/** 影响提醒可靠性的权限/能力项（实现方案 §11 权限诊断页）。 */
enum class PermissionType {
    /** 发送通知（POST_NOTIFICATIONS，Android 13+）。 */
    NOTIFICATION,

    /** 精确闹钟（SCHEDULE_EXACT_ALARM，Android 12+）。 */
    EXACT_ALARM,

    /** 全屏提醒（USE_FULL_SCREEN_INTENT，Android 14+ 锁屏全屏弹出）。 */
    FULL_SCREEN,

    /** 电池优化白名单，用于降低后台提醒被系统延后或拦截的概率。 */
    BATTERY_OPTIMIZATION,
}

/** 单项权限状态。[granted] 为 false 时引导用户去系统设置开启。 */
data class PermissionItem(
    val type: PermissionType,
    val granted: Boolean,
)

/**
 * 把各项实际授予状态汇总为诊断列表（纯函数，可单测）。固定顺序便于稳定展示与测试。
 */
object PermissionDiagnostics {

    fun build(
        notificationGranted: Boolean,
        exactAlarmGranted: Boolean,
        fullScreenGranted: Boolean,
        batteryOptimizationIgnored: Boolean,
    ): List<PermissionItem> = listOf(
        PermissionItem(PermissionType.NOTIFICATION, notificationGranted),
        PermissionItem(PermissionType.EXACT_ALARM, exactAlarmGranted),
        PermissionItem(PermissionType.FULL_SCREEN, fullScreenGranted),
        PermissionItem(PermissionType.BATTERY_OPTIMIZATION, batteryOptimizationIgnored),
    )

    /** 是否所有项均已授予。 */
    fun allGranted(items: List<PermissionItem>): Boolean = items.all { it.granted }
}
