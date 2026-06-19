package com.sqhh99.punchreminder.domain.scheduler

import com.sqhh99.punchreminder.domain.holiday.HolidayCalendar
import com.sqhh99.punchreminder.domain.holiday.StatutoryDayDecision
import com.sqhh99.punchreminder.domain.model.PunchTask
import java.time.LocalDateTime

/**
 * 计算任务的下一次触发时间。核心业务模块，纯函数，重点单元测试（实现方案 §15.2）。
 *
 * 规则：
 * - 从 [now] 当天开始，向后最多查找 [MAX_LOOKAHEAD_DAYS] 天（覆盖长假整段全放假时跨过假期）。
 * - 候选日是否提醒由 [StatutoryDayDecision] 决定：基础星期 + 是否遵循法定节假日 + 该日法定类型。
 * - 若候选日是今天，则任务时间必须严格晚于当前时间（已过则顺延）。
 * - 找到第一个满足条件的日期，返回该日期 + 任务时间；找不到返回 null。
 *
 * 注意：本方法不检查任务是否启用，是否启用由调用方决定（便于复用与测试）。
 * [holidayCalendar] 默认为 [HolidayCalendar.ALWAYS_UNKNOWN]，此时退化为纯「按星期」逻辑。
 */
class NextTriggerTimeCalculator(
    private val holidayCalendar: HolidayCalendar = HolidayCalendar.ALWAYS_UNKNOWN,
) {

    fun nextTrigger(task: PunchTask, now: LocalDateTime): LocalDateTime? {
        val activeDays = task.schedule.activeDays()
        val taskTime = task.time
        for (offset in 0..MAX_LOOKAHEAD_DAYS) {
            val candidateDate = now.toLocalDate().plusDays(offset.toLong())
            val base = candidateDate.dayOfWeek in activeDays
            val dayType = holidayCalendar.dayType(candidateDate)
            if (!StatutoryDayDecision.shouldRemind(base, task.followStatutoryCalendar, dayType)) continue

            val candidate = LocalDateTime.of(candidateDate, taskTime)
            // 当天必须严格晚于现在；未来日期直接接受。
            if (offset == 0 && !candidate.isAfter(now)) continue
            return candidate
        }
        return null
    }

    private companion object {
        // 放宽到 31 天：长假（如国庆 7 天 + 相邻周末）全放假时，需越过整段假期找到下一个工作日。
        const val MAX_LOOKAHEAD_DAYS = 31
    }
}
