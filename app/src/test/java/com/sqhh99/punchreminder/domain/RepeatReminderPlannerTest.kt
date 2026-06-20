package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.scheduler.RepeatReminderPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepeatReminderPlannerTest {

    private fun task(
        repeatReminder: Boolean = true,
        maxReminderCount: Int = 3,
        intervalMinutes: Int = 5,
    ) = PunchTask(
        id = "t1", name = "t", hour = 9, minute = 0, schedule = TaskSchedule.Daily,
        repeatReminder = repeatReminder, maxReminderCount = maxReminderCount,
        reminderIntervalMinutes = intervalMinutes,
    )

    @Test
    fun repeatOff_returnsNull() {
        assertNull(RepeatReminderPlanner.nextRepeatIndex(task(repeatReminder = false), currentIndex = 0))
    }

    @Test
    fun firstFire_schedulesSecond() {
        assertEquals(1, RepeatReminderPlanner.nextRepeatIndex(task(maxReminderCount = 3), currentIndex = 0))
    }

    @Test
    fun lastAllowed_stops() {
        // maxReminderCount=3 含首次：index 0,1,2 共三次提醒；index 2 后不再重复。
        assertNull(RepeatReminderPlanner.nextRepeatIndex(task(maxReminderCount = 3), currentIndex = 2))
    }

    @Test
    fun maxCountOne_neverRepeats() {
        assertNull(RepeatReminderPlanner.nextRepeatIndex(task(maxReminderCount = 1), currentIndex = 0))
    }

    @Test
    fun nonPositiveInterval_returnsNull() {
        assertNull(RepeatReminderPlanner.nextRepeatIndex(task(intervalMinutes = 0), currentIndex = 0))
    }

    @Test
    fun maxRepeatIndex_repeatOff_isZero() {
        assertEquals(0, RepeatReminderPlanner.maxRepeatIndex(task(repeatReminder = false, maxReminderCount = 5)))
    }

    @Test
    fun maxRepeatIndex_nonPositiveInterval_isZero() {
        assertEquals(0, RepeatReminderPlanner.maxRepeatIndex(task(intervalMinutes = 0, maxReminderCount = 5)))
    }

    @Test
    fun maxRepeatIndex_isCountMinusOne() {
        assertEquals(2, RepeatReminderPlanner.maxRepeatIndex(task(maxReminderCount = 3)))
        assertEquals(4, RepeatReminderPlanner.maxRepeatIndex(task(maxReminderCount = 5)))
    }

    @Test
    fun maxRepeatIndex_countOne_isZero() {
        assertEquals(0, RepeatReminderPlanner.maxRepeatIndex(task(maxReminderCount = 1)))
    }
}
