package com.sqhh99.punchreminder.domain.scheduler

import com.sqhh99.punchreminder.domain.model.PunchTask

/**
 * 决定一次提醒触发后是否还要安排下一次「未处理时再次提醒」（实现方案 §4.4 重复提醒）。纯函数，可单测。
 *
 * 语义：[PunchTask.maxReminderCount] 为「到点后最多提醒次数（含首次）」。无法感知用户是否已完成打卡，
 * 因此重复提醒以次数封顶：发出第 0 次（首次）后，按间隔再提醒，直到累计达到 maxReminderCount。
 */
object RepeatReminderPlanner {

    /**
     * @param currentIndex 本次刚发出的提醒序号（首次为 0）。
     * @return 若还应再次提醒，返回下一次序号；否则返回 null。
     */
    fun nextRepeatIndex(task: PunchTask, currentIndex: Int): Int? {
        if (!task.repeatReminder) return null
        val next = currentIndex + 1
        // maxReminderCount 含首次：累计提醒数 = next + 1，需 <= maxReminderCount。
        return if (next < task.maxReminderCount && task.reminderIntervalMinutes > 0) next else null
    }
}
