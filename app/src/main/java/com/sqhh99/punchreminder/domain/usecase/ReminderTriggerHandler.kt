package com.sqhh99.punchreminder.domain.usecase

import com.sqhh99.punchreminder.data.repository.TaskRepository
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequestBuilder
import java.time.Clock
import java.time.LocalDateTime

/** 一次触发处理的结果，便于测试断言（实现方案 §16）。 */
enum class TriggerOutcome {
    NOTIFIED_AND_RESCHEDULED,
    SKIPPED_DISABLED,
    SKIPPED_WRONG_DAY,
    SKIPPED_MISSING_TASK,
}

/**
 * 处理闹钟触发的核心业务（实现方案 §16）。纯业务、依赖网关接口，可用 fake 做 JVM 单测。
 *
 * 流程：查任务 → 校验启用 → 校验今天属激活星期 → 按 [AppLaunchDecision] 决定是否拉起目标应用
 * → 发通知 → 计算下一次并重排闹钟。
 */
class ReminderTriggerHandler(
    private val repository: TaskRepository,
    private val notificationGateway: NotificationGateway,
    private val alarmGateway: AlarmGateway,
    private val installChecker: AppInstallChecker,
    private val launchDecision: AppLaunchDecision = AppLaunchDecision(),
    private val requestBuilder: AlarmScheduleRequestBuilder = AlarmScheduleRequestBuilder(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    suspend fun handle(taskId: String): TriggerOutcome {
        val task = repository.getById(taskId) ?: return TriggerOutcome.SKIPPED_MISSING_TASK
        if (!task.enabled) return TriggerOutcome.SKIPPED_DISABLED

        val now = LocalDateTime.now(clock)
        if (now.dayOfWeek !in task.schedule.activeDays()) {
            // 不是激活日（理论上不该触发）：仍重排到正确的下一次。
            rescheduleNext(task.id)
            return TriggerOutcome.SKIPPED_WRONG_DAY
        }

        val installed = installChecker.isInstalled(task.targetPackage)
        val action = launchDecision.decide(task, installed)
        val openTargetApp = action == LaunchAction.LAUNCH_DIRECTLY
        notificationGateway.notify(task, openTargetApp)

        rescheduleNext(task.id)
        return TriggerOutcome.NOTIFIED_AND_RESCHEDULED
    }

    private suspend fun rescheduleNext(taskId: String) {
        val task = repository.getById(taskId) ?: return
        val request = requestBuilder.build(
            task,
            LocalDateTime.now(clock),
            alarmGateway.canScheduleExact(),
        )
        if (request != null) alarmGateway.schedule(request)
    }
}
