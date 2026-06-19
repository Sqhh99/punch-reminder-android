package com.sqhh99.punchreminder.domain.holiday

/**
 * 某一日历日相对法定工作安排的类型。
 *
 * - [WORKDAY]：法定工作日（数据明确）。
 * - [HOLIDAY]：法定放假日（中秋/国庆/劳动节等，不提醒）。
 * - [MAKEUP_WORKDAY]：调休上班日（周末变工作日，照常提醒）。
 * - [UNKNOWN]：无数据（未联网/未覆盖该日）→ 回退到「按星期」原逻辑。
 */
enum class DayType { WORKDAY, HOLIDAY, MAKEUP_WORKDAY, UNKNOWN }
