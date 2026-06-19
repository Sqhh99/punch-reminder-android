package com.sqhh99.punchreminder.domain.holiday

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.scheduler.NextTriggerTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class NextTriggerTimeCalculatorHolidayTest {

    private fun task(
        schedule: TaskSchedule = TaskSchedule.Weekdays,
        follow: Boolean = true,
    ) = PunchTask(
        name = "上班打卡", hour = 9, minute = 0,
        schedule = schedule, followStatutoryCalendar = follow,
    )

    private fun calc(map: Map<LocalDate, DayType>) =
        NextTriggerTimeCalculator(FakeHolidayCalendar(map))

    @Test
    fun weekday_marked_holiday_isSkipped() {
        // 2026-06-18 周四标为放假 → 跳到周五 06-19
        val c = calc(mapOf(LocalDate.of(2026, 6, 18) to DayType.HOLIDAY))
        val next = c.nextTrigger(task(), LocalDateTime.of(2026, 6, 18, 8, 0))
        assertEquals(LocalDateTime.of(2026, 6, 19, 9, 0), next)
    }

    @Test
    fun saturday_makeupWorkday_isIncluded() {
        // 2026-06-20 周六本不提醒；标为调休上班 → 当天命中
        val c = calc(mapOf(LocalDate.of(2026, 6, 20) to DayType.MAKEUP_WORKDAY))
        val next = c.nextTrigger(task(), LocalDateTime.of(2026, 6, 20, 8, 0))
        assertEquals(LocalDateTime.of(2026, 6, 20, 9, 0), next)
    }

    @Test
    fun fullHolidayWeek_jumpsPastTheBlock() {
        // 国庆：2026-10-01(周四)~10-07 全部放假，跳到 10-08(周四)
        val days = (1..7).associate { LocalDate.of(2026, 10, it) to DayType.HOLIDAY }
        val c = calc(days)
        val next = c.nextTrigger(task(), LocalDateTime.of(2026, 10, 1, 8, 0))
        assertEquals(LocalDateTime.of(2026, 10, 8, 9, 0), next)
    }

    @Test
    fun yearBoundary_newYearHoliday_rollsToNextWorkday() {
        // 2026-12-31 周四 10:00（已过 9:00）；2027-01-01 周五标放假 → 2027-01-04 周一
        val c = calc(mapOf(LocalDate.of(2027, 1, 1) to DayType.HOLIDAY))
        val next = c.nextTrigger(task(), LocalDateTime.of(2026, 12, 31, 10, 0))
        assertEquals(LocalDateTime.of(2027, 1, 4, 9, 0), next)
    }

    @Test
    fun followFalse_ignoresHolidayData() {
        // 不遵循法定节假日：周四标放假也照常命中当天（回归原逻辑）
        val c = calc(mapOf(LocalDate.of(2026, 6, 18) to DayType.HOLIDAY))
        val next = c.nextTrigger(task(follow = false), LocalDateTime.of(2026, 6, 18, 8, 0))
        assertEquals(LocalDateTime.of(2026, 6, 18, 9, 0), next)
    }
}
