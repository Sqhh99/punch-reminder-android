package com.sqhh99.punchreminder.data.mapper

import com.sqhh99.punchreminder.data.datastore.TaskDto
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.ScheduleType
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import java.time.DayOfWeek

/** 领域模型 [PunchTask] 与持久化 [TaskDto] 之间的双向映射。纯函数，可单测。 */
object TaskMapper {

    fun toDto(task: PunchTask): TaskDto = TaskDto(
        id = task.id,
        name = task.name,
        hour = task.hour,
        minute = task.minute,
        scheduleType = task.schedule.type.name,
        customDays = task.schedule.customDays.map { it.value }.sorted(),
        targetPackage = task.targetPackage,
        targetAppLabel = task.targetAppLabel,
        enabled = task.enabled,
        autoLaunch = task.autoLaunch,
        lockScreenAlert = task.lockScreenAlert,
        repeatReminder = task.repeatReminder,
        reminderIntervalMinutes = task.reminderIntervalMinutes,
        maxReminderCount = task.maxReminderCount,
        followStatutoryCalendar = task.followStatutoryCalendar,
    )

    fun toDomain(dto: TaskDto): PunchTask {
        val type = runCatching { ScheduleType.valueOf(dto.scheduleType) }
            .getOrDefault(ScheduleType.DAILY)
        val days = dto.customDays.mapNotNull { value ->
            runCatching { DayOfWeek.of(value) }.getOrNull()
        }.toSet()
        return PunchTask(
            id = dto.id,
            name = dto.name,
            hour = dto.hour,
            minute = dto.minute,
            schedule = TaskSchedule(type, days),
            targetPackage = dto.targetPackage,
            targetAppLabel = dto.targetAppLabel,
            enabled = dto.enabled,
            autoLaunch = dto.autoLaunch,
            lockScreenAlert = dto.lockScreenAlert,
            repeatReminder = dto.repeatReminder,
            reminderIntervalMinutes = dto.reminderIntervalMinutes,
            maxReminderCount = dto.maxReminderCount,
            followStatutoryCalendar = dto.followStatutoryCalendar,
        )
    }
}
