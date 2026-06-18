package com.sqhh99.punchreminder.domain.model

import java.time.LocalTime
import java.util.UUID

/**
 * 一个打卡提醒任务。纯领域模型（实现方案 §4.2 任务数据字段）。
 *
 * @param id 唯一标识。
 * @param name 任务名称，例如「上班打卡」。
 * @param hour 执行时间-小时（0-23）。
 * @param minute 执行时间-分钟（0-59）。
 * @param schedule 执行周期。
 * @param targetPackage 目标应用包名；为空表示尚未选择目标应用。
 * @param targetAppLabel 目标应用显示名（缓存，便于列表展示）。
 * @param enabled 是否启用。
 * @param autoLaunch 触发时是否尝试自动打开目标应用。
 * @param repeatReminder 是否重复提醒（完整实现见 0.7.0）。
 * @param reminderIntervalMinutes 重复提醒间隔（分钟）。
 * @param maxReminderCount 最大提醒次数。
 */
data class PunchTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hour: Int,
    val minute: Int,
    val schedule: TaskSchedule,
    val targetPackage: String? = null,
    val targetAppLabel: String? = null,
    val enabled: Boolean = true,
    val autoLaunch: Boolean = true,
    val repeatReminder: Boolean = false,
    val reminderIntervalMinutes: Int = 5,
    val maxReminderCount: Int = 2,
) {
    val time: LocalTime get() = LocalTime.of(hour, minute)

    val hasTargetApp: Boolean get() = !targetPackage.isNullOrBlank()
}
