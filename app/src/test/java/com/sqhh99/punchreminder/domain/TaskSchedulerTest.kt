package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.data.repository.TaskRepository
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequest
import com.sqhh99.punchreminder.domain.usecase.AlarmGateway
import com.sqhh99.punchreminder.domain.usecase.TaskScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 验证调度协调用例 [TaskScheduler]：增删改启停 → 注册/取消闹钟，以及开机恢复用的 [TaskScheduler.rescheduleAll]。
 * 用 fake 网关替代真实 AlarmManager（实现方案 §15 / §16）。
 */
class TaskSchedulerTest {

    // 2026-06-18 周四 09:00（Asia/Shanghai），所有任务设在更晚的 18:00，保证今天仍有下一次。
    private val zone = ZoneId.of("Asia/Shanghai")
    private val clock = Clock.fixed(
        ZonedDateTime.of(2026, 6, 18, 9, 0, 0, 0, zone).toInstant(),
        zone,
    )

    private class FakeRepo(initial: List<PunchTask> = emptyList()) : TaskRepository {
        private val state = MutableStateFlow(initial)
        override val tasks = state
        override suspend fun upsert(task: PunchTask) = state.update { list ->
            val i = list.indexOfFirst { it.id == task.id }
            if (i >= 0) list.toMutableList().also { it[i] = task } else list + task
        }
        override suspend fun delete(taskId: String) = state.update { it.filterNot { t -> t.id == taskId } }
        override suspend fun setEnabled(taskId: String, enabled: Boolean) =
            state.update { it.map { t -> if (t.id == taskId) t.copy(enabled = enabled) else t } }
        override suspend fun getById(taskId: String) = state.value.firstOrNull { it.id == taskId }
        override suspend fun getAll() = state.value
    }

    private class FakeAlarm : AlarmGateway {
        val scheduled = mutableListOf<AlarmScheduleRequest>()
        val cancelled = mutableListOf<String>()
        override fun schedule(request: AlarmScheduleRequest) { scheduled += request }
        override fun cancel(taskId: String) { cancelled += taskId }
        override fun cancelRepeats(taskId: String, maxRepeatIndex: Int) {}
        override fun canScheduleExact() = true
    }

    private fun scheduler(repo: TaskRepository, alarm: AlarmGateway) =
        TaskScheduler(repository = repo, alarmGateway = alarm, clock = clock)

    private fun task(id: String, enabled: Boolean = true) = PunchTask(
        id = id, name = "下班打卡", hour = 18, minute = 0, schedule = TaskSchedule.Daily,
        targetPackage = "com.example", targetAppLabel = "企业微信", enabled = enabled,
    )

    @Test
    fun onTaskSaved_enabled_schedules() {
        val alarm = FakeAlarm()
        scheduler(FakeRepo(), alarm).onTaskSaved(task("t1"))

        assertEquals(1, alarm.scheduled.size)
        assertEquals("t1", alarm.scheduled.first().taskId)
        assertTrue(alarm.cancelled.isEmpty())
    }

    @Test
    fun onTaskSaved_disabled_cancels() {
        val alarm = FakeAlarm()
        scheduler(FakeRepo(), alarm).onTaskSaved(task("t1", enabled = false))

        assertTrue(alarm.scheduled.isEmpty())
        assertEquals(listOf("t1"), alarm.cancelled)
    }

    @Test
    fun onTaskRemoved_cancels() {
        val alarm = FakeAlarm()
        scheduler(FakeRepo(), alarm).onTaskRemoved("t1")

        assertEquals(listOf("t1"), alarm.cancelled)
    }

    @Test
    fun rescheduleAll_schedulesEnabledAndCancelsDisabled() = runBlocking {
        val repo = FakeRepo(
            listOf(
                task("enabled-1"),
                task("disabled", enabled = false),
                task("enabled-2"),
            ),
        )
        val alarm = FakeAlarm()
        scheduler(repo, alarm).rescheduleAll()

        assertEquals(setOf("enabled-1", "enabled-2"), alarm.scheduled.map { it.taskId }.toSet())
        assertEquals(listOf("disabled"), alarm.cancelled)
    }

    @Test
    fun rescheduleAll_emptyRepo_noop() = runBlocking {
        val alarm = FakeAlarm()
        scheduler(FakeRepo(), alarm).rescheduleAll()

        assertTrue(alarm.scheduled.isEmpty())
        assertTrue(alarm.cancelled.isEmpty())
    }
}
