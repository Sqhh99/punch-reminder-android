package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequestBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduleRequestBuilderTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val builder = AlarmScheduleRequestBuilder(zoneId = zone)

    private fun task(enabled: Boolean = true, schedule: TaskSchedule = TaskSchedule.Daily) =
        PunchTask(id = "t1", name = "t", hour = 9, minute = 0, schedule = schedule, enabled = enabled)

    @Test
    fun disabledTask_returnsNull() {
        val now = LocalDateTime.of(2026, 6, 18, 8, 0)
        assertNull(builder.build(task(enabled = false), now, exactAllowed = true))
    }

    @Test
    fun noActiveDays_returnsNull() {
        val now = LocalDateTime.of(2026, 6, 18, 8, 0)
        assertNull(builder.build(task(schedule = TaskSchedule.custom(emptySet())), now, true))
    }

    @Test
    fun enabled_buildsExpectedTriggerMillis() {
        val now = LocalDateTime.of(2026, 6, 18, 8, 0)
        val request = builder.build(task(), now, exactAllowed = true)!!
        val expected = LocalDateTime.of(2026, 6, 18, 9, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals("t1", request.taskId)
        assertEquals(expected, request.triggerAtMillis)
        assertTrue(request.exact)
    }

    @Test
    fun exactNotAllowed_marksInexact() {
        val now = LocalDateTime.of(2026, 6, 18, 8, 0)
        val request = builder.build(task(), now, exactAllowed = false)!!
        assertEquals(false, request.exact)
    }
}
