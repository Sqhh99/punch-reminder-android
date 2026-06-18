package com.sqhh99.punchreminder.domain.scheduler

import com.sqhh99.punchreminder.domain.model.PunchTask
import java.time.LocalDateTime

/**
 * 计算任务的下一次触发时间。核心业务模块，纯函数，重点单元测试（实现方案 §15.2）。
 *
 * 规则：
 * - 从 [now] 当天开始，向后最多查找 7 天。
 * - 候选日必须属于任务激活的星期。
 * - 若候选日是今天，则任务时间必须严格晚于当前时间（已过则顺延）。
 * - 找到第一个满足条件的日期，返回该日期 + 任务时间。
 * - 任务没有任何激活星期时返回 null。
 *
 * 注意：本方法不检查任务是否启用，是否启用由调用方决定（便于复用与测试）。
 */
class NextTriggerTimeCalculator {

    fun nextTrigger(task: PunchTask, now: LocalDateTime): LocalDateTime? {
        val activeDays = task.schedule.activeDays()
        if (activeDays.isEmpty()) return null

        val taskTime = task.time
        // 0..7：当天到下周同一天，覆盖跨天/跨周/跨月/跨年。
        for (offset in 0..7) {
            val candidateDate = now.toLocalDate().plusDays(offset.toLong())
            if (candidateDate.dayOfWeek !in activeDays) continue

            val candidate = LocalDateTime.of(candidateDate, taskTime)
            // 当天必须严格晚于现在；未来日期直接接受。
            if (offset == 0 && !candidate.isAfter(now)) continue
            return candidate
        }
        return null
    }
}
