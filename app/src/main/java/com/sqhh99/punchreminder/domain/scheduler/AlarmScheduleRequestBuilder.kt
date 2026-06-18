package com.sqhh99.punchreminder.domain.scheduler

import com.sqhh99.punchreminder.domain.model.PunchTask
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 一次闹钟调度请求。system 层据此调用 AlarmManager。
 *
 * @param repeatIndex 第几次提醒：0 为当天首次（日常闹钟），>0 为「未处理时再次提醒」的重复闹钟。
 */
data class AlarmScheduleRequest(
    val taskId: String,
    val triggerAtMillis: Long,
    val exact: Boolean,
    val repeatIndex: Int = 0,
)

/**
 * 由任务和下一次触发时间构建闹钟调度请求（实现方案 §15.1 必测模块）。纯函数。
 *
 * - 任务未启用 → null（不调度）。
 * - 无下一次触发时间 → null。
 * - [exactAllowed] 反映系统是否允许精确闹钟（API/权限），决定 exact 字段。
 */
class AlarmScheduleRequestBuilder(
    private val calculator: NextTriggerTimeCalculator = NextTriggerTimeCalculator(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    fun build(
        task: PunchTask,
        now: LocalDateTime,
        exactAllowed: Boolean,
    ): AlarmScheduleRequest? {
        if (!task.enabled) return null
        val next = calculator.nextTrigger(task, now) ?: return null
        val triggerAtMillis = next.atZone(zoneId).toInstant().toEpochMilli()
        return AlarmScheduleRequest(
            taskId = task.id,
            triggerAtMillis = triggerAtMillis,
            exact = exactAllowed,
            repeatIndex = 0,
        )
    }

    /**
     * 构建「未处理时再次提醒」的重复闹钟：在 [now] 后 reminderIntervalMinutes 分钟触发，
     * 携带 [repeatIndex]。任务未启用或未开启重复提醒时返回 null。纯函数。
     */
    fun buildRepeat(
        task: PunchTask,
        now: LocalDateTime,
        exactAllowed: Boolean,
        repeatIndex: Int,
    ): AlarmScheduleRequest? {
        if (!task.enabled || !task.repeatReminder) return null
        val triggerAt = now.plusMinutes(task.reminderIntervalMinutes.toLong())
        return AlarmScheduleRequest(
            taskId = task.id,
            triggerAtMillis = triggerAt.atZone(zoneId).toInstant().toEpochMilli(),
            exact = exactAllowed,
            repeatIndex = repeatIndex,
        )
    }
}
