package com.sqhh99.punchreminder.system.permission

import android.content.Context
import com.sqhh99.punchreminder.domain.permission.PermissionDiagnostics
import com.sqhh99.punchreminder.domain.permission.PermissionItem
import com.sqhh99.punchreminder.domain.usecase.AlarmGateway

/**
 * 读取通知 / 精确闹钟 / 全屏提醒的真实授予状态，汇总为诊断列表（实现方案 §11）。
 * 真实状态读取放 system 层；汇总逻辑在可单测的 [PermissionDiagnostics]。
 */
class PermissionInspector(
    private val context: Context,
    private val alarmGateway: AlarmGateway,
) {
    fun inspect(): List<PermissionItem> = PermissionDiagnostics.build(
        notificationGranted = NotificationPermission.isGranted(context),
        exactAlarmGranted = alarmGateway.canScheduleExact(),
        fullScreenGranted = FullScreenIntentPermission.canUse(context),
    )
}
