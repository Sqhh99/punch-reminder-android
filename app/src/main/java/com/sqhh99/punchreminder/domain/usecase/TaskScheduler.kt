package com.sqhh99.punchreminder.domain.usecase

import com.sqhh99.punchreminder.data.repository.TaskRepository
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequestBuilder
import java.time.Clock
import java.time.LocalDateTime

/**
 * 任务调度协调用例：把任务的增删改启停转换为系统闹钟的注册/取消。
 *
 * 增删改启停后由 ViewModel 调用；触发后由 [ReminderTriggerHandler] 调用以重排下一次。
 */
class TaskScheduler(
    private val repository: TaskRepository,
    private val alarmGateway: AlarmGateway,
    private val requestBuilder: AlarmScheduleRequestBuilder = AlarmScheduleRequestBuilder(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /** 任务保存（新增/编辑）后：启用则按下一次时间注册，停用则取消。 */
    fun onTaskSaved(task: PunchTask) {
        if (!task.enabled) {
            alarmGateway.cancel(task.id)
            return
        }
        val request = requestBuilder.build(task, now(), alarmGateway.canScheduleExact())
        if (request != null) alarmGateway.schedule(request) else alarmGateway.cancel(task.id)
    }

    fun onTaskRemoved(taskId: String) {
        alarmGateway.cancel(taskId)
    }

    /** 重新调度单个任务（按当前仓库状态）。 */
    suspend fun reschedule(taskId: String) {
        val task = repository.getById(taskId) ?: run {
            alarmGateway.cancel(taskId)
            return
        }
        onTaskSaved(task)
    }

    /** 重新调度所有启用任务（开机恢复 0.5.0 会复用）。 */
    suspend fun rescheduleAll() {
        repository.getAll().forEach { task ->
            if (task.enabled) onTaskSaved(task) else alarmGateway.cancel(task.id)
        }
    }

    private fun now(): LocalDateTime = LocalDateTime.now(clock)
}
