package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.scheduler.NextTriggerTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class NextTriggerTimeCalculatorTest {

    private val calc = NextTriggerTimeCalculator()

    private fun task(
        hour: Int = 9,
        minute: Int = 0,
        schedule: TaskSchedule = TaskSchedule.Daily,
    ) = PunchTask(name = "t", hour = hour, minute = minute, schedule = schedule)

    @Test
    fun daily_today_notYetPassed_returnsToday() {
        // 2026-06-18 是周四
        val now = LocalDateTime.of(2026, 6, 18, 8, 0)
        val next = calc.nextTrigger(task(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 6, 18, 9, 0), next)
    }

    @Test
    fun daily_today_alreadyPassed_rollsToTomorrow() {
        val now = LocalDateTime.of(2026, 6, 18, 10, 0)
        val next = calc.nextTrigger(task(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 6, 19, 9, 0), next)
    }

    @Test
    fun daily_exactlyNow_rollsToNextDay() {
        val now = LocalDateTime.of(2026, 6, 18, 9, 0)
        val next = calc.nextTrigger(task(9, 0), now)
        // 必须严格晚于现在
        assertEquals(LocalDateTime.of(2026, 6, 19, 9, 0), next)
    }

    @Test
    fun weekdays_fridayAfterTime_rollsToMonday() {
        // 2026-06-19 是周五
        val now = LocalDateTime.of(2026, 6, 19, 10, 0)
        val next = calc.nextTrigger(task(9, 0, TaskSchedule.Weekdays), now)
        // 下一个工作日是周一 2026-06-22
        assertEquals(LocalDateTime.of(2026, 6, 22, 9, 0), next)
    }

    @Test
    fun weekdays_saturday_rollsToMonday() {
        // 2026-06-20 是周六
        val now = LocalDateTime.of(2026, 6, 20, 8, 0)
        val next = calc.nextTrigger(task(9, 0, TaskSchedule.Weekdays), now)
        assertEquals(LocalDateTime.of(2026, 6, 22, 9, 0), next)
    }

    @Test
    fun custom_singleDay_rollsToThatDay() {
        // 只在周三执行；now 周四 → 下周三
        val schedule = TaskSchedule.custom(setOf(DayOfWeek.WEDNESDAY))
        val now = LocalDateTime.of(2026, 6, 18, 8, 0) // 周四
        val next = calc.nextTrigger(task(9, 0, schedule), now)
        assertEquals(LocalDateTime.of(2026, 6, 24, 9, 0), next) // 下周三
    }

    @Test
    fun crossMonth_endOfMonthRollsToNextMonth() {
        // 2026-06-30 周二 10:00，每天 9:00 → 7-01 9:00
        val now = LocalDateTime.of(2026, 6, 30, 10, 0)
        val next = calc.nextTrigger(task(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 7, 1, 9, 0), next)
    }

    @Test
    fun crossYear_endOfYearRollsToNextYear() {
        // 2026-12-31 10:00 每天 9:00 → 2027-01-01 9:00
        val now = LocalDateTime.of(2026, 12, 31, 10, 0)
        val next = calc.nextTrigger(task(9, 0), now)
        assertEquals(LocalDateTime.of(2027, 1, 1, 9, 0), next)
    }

    @Test
    fun emptyCustomDays_returnsNull() {
        val schedule = TaskSchedule.custom(emptySet())
        val now = LocalDateTime.of(2026, 6, 18, 8, 0)
        assertNull(calc.nextTrigger(task(9, 0, schedule), now))
    }
}
