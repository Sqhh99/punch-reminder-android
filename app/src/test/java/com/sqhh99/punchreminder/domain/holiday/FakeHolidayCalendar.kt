package com.sqhh99.punchreminder.domain.holiday

import java.time.LocalDate

/** 测试用日历：按给定 map 返回类型，未列出的日期为 UNKNOWN。 */
class FakeHolidayCalendar(
    private val map: Map<LocalDate, DayType> = emptyMap(),
) : HolidayCalendar {
    override fun dayType(date: LocalDate): DayType = map[date] ?: DayType.UNKNOWN
}
