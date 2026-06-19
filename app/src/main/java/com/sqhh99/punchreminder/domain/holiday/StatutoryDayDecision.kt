package com.sqhh99.punchreminder.domain.holiday

/**
 * 「今天/某日是否应提醒」的纯决策（实现方案 §5 扩展）。计算器与触发处理器的**单一事实来源**。
 *
 * 规则：
 * - 任务未开启「遵循法定节假日/调休」→ 按基础星期判定（[base]）。
 * - 法定放假日 [DayType.HOLIDAY] → 不提醒（即使是工作日）。
 * - 调休上班日 [DayType.MAKEUP_WORKDAY] → 提醒（即使是周末）。
 * - 工作日 / 无数据 [DayType.WORKDAY]/[DayType.UNKNOWN] → 按基础星期判定。
 */
object StatutoryDayDecision {

    /**
     * @param base 该日的星期是否属于任务激活星期（date.dayOfWeek in schedule.activeDays()）。
     * @param follow 任务是否开启 followStatutoryCalendar。
     * @param dayType 该日的法定类型。
     */
    fun shouldRemind(base: Boolean, follow: Boolean, dayType: DayType): Boolean =
        if (!follow) {
            base
        } else when (dayType) {
            DayType.HOLIDAY -> false
            DayType.MAKEUP_WORKDAY -> true
            DayType.WORKDAY, DayType.UNKNOWN -> base
        }
}
