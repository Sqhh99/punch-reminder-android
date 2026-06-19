package com.sqhh99.punchreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqhh99.punchreminder.data.repository.TaskRepository
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.ScheduleType
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.usecase.AppInstallChecker
import com.sqhh99.punchreminder.domain.usecase.TaskScheduler
import com.sqhh99.punchreminder.domain.validation.TaskValidationError
import com.sqhh99.punchreminder.domain.validation.TaskValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class TaskEditUiState(
    val id: String? = null,
    val name: String = "",
    val hour: Int = 9,
    val minute: Int = 0,
    val scheduleType: ScheduleType = ScheduleType.WEEKDAYS,
    val customDays: Set<DayOfWeek> = emptySet(),
    val targetPackage: String? = null,
    val targetAppLabel: String? = null,
    val targetInstalled: Boolean = true,
    val enabled: Boolean = true,
    val autoLaunch: Boolean = true,
    val lockScreenAlert: Boolean = true,
    val repeatReminder: Boolean = false,
    val reminderIntervalMinutes: Int = 5,
    val maxReminderCount: Int = 2,
    val followStatutoryCalendar: Boolean = true,
    val errors: List<TaskValidationError> = emptyList(),
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}

class TaskEditViewModel(
    private val repository: TaskRepository,
    private val taskScheduler: TaskScheduler,
    private val installChecker: AppInstallChecker,
    private val validator: TaskValidator = TaskValidator(),
    taskId: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    init {
        if (taskId != null) {
            viewModelScope.launch {
                repository.getById(taskId)?.let { task -> _uiState.value = task.toUiState() }
            }
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }
    fun setTime(hour: Int, minute: Int) = _uiState.update { it.copy(hour = hour, minute = minute) }
    fun setScheduleType(type: ScheduleType) = _uiState.update { it.copy(scheduleType = type) }
    fun toggleCustomDay(day: DayOfWeek) = _uiState.update {
        val days = it.customDays.toMutableSet().apply { if (!add(day)) remove(day) }
        it.copy(customDays = days)
    }
    fun setAutoLaunch(value: Boolean) = _uiState.update { it.copy(autoLaunch = value) }
    fun setLockScreenAlert(value: Boolean) = _uiState.update { it.copy(lockScreenAlert = value) }
    fun setEnabled(value: Boolean) = _uiState.update { it.copy(enabled = value) }
    fun setRepeatReminder(value: Boolean) = _uiState.update { it.copy(repeatReminder = value) }
    fun setReminderInterval(minutes: Int) = _uiState.update { it.copy(reminderIntervalMinutes = minutes) }
    fun setMaxReminderCount(count: Int) = _uiState.update { it.copy(maxReminderCount = count) }
    fun setFollowStatutoryCalendar(value: Boolean) = _uiState.update { it.copy(followStatutoryCalendar = value) }

    fun setTargetApp(packageName: String, label: String) = _uiState.update {
        it.copy(targetPackage = packageName, targetAppLabel = label, targetInstalled = true)
    }

    fun save() {
        val task = _uiState.value.toTask()
        val errors = validator.validate(task)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            repository.upsert(task)
            taskScheduler.onTaskSaved(task)
            _uiState.update { it.copy(errors = emptyList(), saved = true) }
        }
    }

    private fun TaskEditUiState.toTask(): PunchTask {
        val schedule = when (scheduleType) {
            ScheduleType.DAILY -> TaskSchedule.Daily
            ScheduleType.WEEKDAYS -> TaskSchedule.Weekdays
            ScheduleType.CUSTOM -> TaskSchedule.custom(customDays)
        }
        return PunchTask(
            id = id ?: java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            hour = hour,
            minute = minute,
            schedule = schedule,
            targetPackage = targetPackage,
            targetAppLabel = targetAppLabel,
            enabled = enabled,
            autoLaunch = autoLaunch,
            lockScreenAlert = lockScreenAlert,
            repeatReminder = repeatReminder,
            reminderIntervalMinutes = reminderIntervalMinutes,
            maxReminderCount = maxReminderCount,
            followStatutoryCalendar = followStatutoryCalendar,
        )
    }

    private fun PunchTask.toUiState(): TaskEditUiState = TaskEditUiState(
        id = id,
        name = name,
        hour = hour,
        minute = minute,
        scheduleType = schedule.type,
        customDays = schedule.customDays,
        targetPackage = targetPackage,
        targetAppLabel = targetAppLabel,
        targetInstalled = installChecker.isInstalled(targetPackage),
        enabled = enabled,
        autoLaunch = autoLaunch,
        lockScreenAlert = lockScreenAlert,
        repeatReminder = repeatReminder,
        reminderIntervalMinutes = reminderIntervalMinutes,
        maxReminderCount = maxReminderCount,
        followStatutoryCalendar = followStatutoryCalendar,
    )
}
