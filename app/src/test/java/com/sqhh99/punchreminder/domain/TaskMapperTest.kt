package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.data.datastore.TaskDto
import com.sqhh99.punchreminder.data.mapper.TaskMapper
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.ScheduleType
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import kotlinx.serialization.json.Json
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
            lockScreenAlert = false,
            repeatReminder = true,
            reminderIntervalMinutes = 10,
            maxReminderCount = 3,
            followStatutoryCalendar = false,
        )
        val restored = TaskMapper.toDomain(TaskMapper.toDto(task))
        assertEquals(task, restored)
    }

    @Test
    fun followStatutoryCalendar_roundTripsBothValues() {
        val base = PunchTask(id = "x", name = "n", hour = 9, minute = 0, schedule = TaskSchedule.Daily)
        listOf(true, false).forEach { value ->
            val restored = TaskMapper.toDomain(TaskMapper.toDto(base.copy(followStatutoryCalendar = value)))
            assertEquals(value, restored.followStatutoryCalendar)
        }
    }

    @Test
    fun legacyDtoWithoutFollowField_defaultsToTrue() {
        // 旧版本持久化的 JSON 缺少 followStatutoryCalendar 字段，应解码为默认 true（向后兼容）。
        val legacyJson = """{"id":"x","name":"n","hour":9,"minute":0,"scheduleType":"DAILY"}"""
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<TaskDto>(legacyJson)
        assertEquals(true, TaskMapper.toDomain(dto).followStatutoryCalendar)
    }

    @Test
    fun lockScreenAlert_roundTripsBothValues() {
        val base = PunchTask(id = "x", name = "n", hour = 9, minute = 0, schedule = TaskSchedule.Daily)
        listOf(true, false).forEach { value ->
            val restored = TaskMapper.toDomain(TaskMapper.toDto(base.copy(lockScreenAlert = value)))
            assertEquals(value, restored.lockScreenAlert)
        }
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
