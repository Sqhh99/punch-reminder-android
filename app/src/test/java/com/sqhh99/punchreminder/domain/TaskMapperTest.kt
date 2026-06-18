package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.data.datastore.TaskDto
import com.sqhh99.punchreminder.data.mapper.TaskMapper
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.ScheduleType
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class TaskMapperTest {

    @Test
    fun roundTrip_preservesAllFields() {
        val task = PunchTask(
            id = "abc",
            name = "上班打卡",
            hour = 8,
            minute = 50,
            schedule = TaskSchedule.custom(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
            targetPackage = "com.example.punch",
            targetAppLabel = "企业微信",
            enabled = false,
            autoLaunch = false,
            repeatReminder = true,
            reminderIntervalMinutes = 10,
            maxReminderCount = 3,
        )
        val restored = TaskMapper.toDomain(TaskMapper.toDto(task))
        assertEquals(task, restored)
    }

    @Test
    fun unknownScheduleType_fallsBackToDaily() {
        val dto = TaskDto(id = "x", name = "n", hour = 9, minute = 0, scheduleType = "GARBAGE")
        assertEquals(ScheduleType.DAILY, TaskMapper.toDomain(dto).schedule.type)
    }

    @Test
    fun invalidCustomDay_isDropped() {
        val dto = TaskDto(
            id = "x", name = "n", hour = 9, minute = 0,
            scheduleType = "CUSTOM", customDays = listOf(1, 99),
        )
        assertEquals(setOf(DayOfWeek.MONDAY), TaskMapper.toDomain(dto).schedule.customDays)
    }
}
