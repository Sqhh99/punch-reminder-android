package com.sqhh99.punchreminder.domain.holiday

import java.time.LocalDate

/**
 * 法定节假日/调休日历的同步查询接口（实现方案 §5 扩展）。
 *
 * [dayType] 必须是同步、无 IO、无副作用——它会在闹钟触发的关键路径（可能是冷启动的后台进程）被调用，
 * 因此实现需基于已加载到内存的缓存，绝不能在此处发起网络或阻塞读盘。无数据时返回 [DayType.UNKNOWN]。
 */
interface HolidayCalendar {
    fun dayType(date: LocalDate): DayType

    companion object {
        /** 默认实现：一律 UNKNOWN，使所有调用点回退到「按星期」原逻辑（零行为变化）。 */
        val ALWAYS_UNKNOWN: HolidayCalendar = object : HolidayCalendar {
            override fun dayType(date: LocalDate): DayType = DayType.UNKNOWN
        }
    }
}
