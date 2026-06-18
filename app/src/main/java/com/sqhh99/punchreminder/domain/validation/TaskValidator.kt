package com.sqhh99.punchreminder.domain.validation

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.ScheduleType

/** 任务校验错误（实现方案 §15.3 任务配置不完整路径）。 */
enum class TaskValidationError {
    EMPTY_NAME,
    INVALID_TIME,
    NO_CUSTOM_DAY,
}

/** 校验任务配置完整性。纯函数，可单测。 */
class TaskValidator {

    fun validate(task: PunchTask): List<TaskValidationError> {
        val errors = mutableListOf<TaskValidationError>()
        if (task.name.isBlank()) errors += TaskValidationError.EMPTY_NAME
        if (task.hour !in 0..23 || task.minute !in 0..59) errors += TaskValidationError.INVALID_TIME
        if (task.schedule.type == ScheduleType.CUSTOM && task.schedule.customDays.isEmpty()) {
            errors += TaskValidationError.NO_CUSTOM_DAY
        }
        return errors
    }

    fun isValid(task: PunchTask): Boolean = validate(task).isEmpty()
}
