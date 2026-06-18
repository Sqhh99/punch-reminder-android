package com.sqhh99.punchreminder.domain.model

import java.time.DayOfWeek

/**
 * 任务执行周期。
 *
 * - [ScheduleType.DAILY]：每天。
 * - [ScheduleType.WEEKDAYS]：工作日（周一至周五）。
 * - [ScheduleType.CUSTOM]：自定义星期，由 [customDays] 指定。
 *
 * 纯领域模型，不依赖 Android，便于单元测试（实现方案 §5）。
 */
enum class ScheduleType { DAILY, WEEKDAYS, CUSTOM }

data class TaskSchedule(
    val type: ScheduleType,
    val customDays: Set<DayOfWeek> = emptySet(),
) {
    /** 该周期实际激活的星期集合。 */
    fun activeDays(): Set<DayOfWeek> = when (type) {
        ScheduleType.DAILY -> DayOfWeek.values().toSet()
        ScheduleType.WEEKDAYS -> setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        ScheduleType.CUSTOM -> customDays
    }

    companion object {
        val Daily = TaskSchedule(ScheduleType.DAILY)
        val Weekdays = TaskSchedule(ScheduleType.WEEKDAYS)
        fun custom(days: Set<DayOfWeek>) = TaskSchedule(ScheduleType.CUSTOM, days)
    }
}
