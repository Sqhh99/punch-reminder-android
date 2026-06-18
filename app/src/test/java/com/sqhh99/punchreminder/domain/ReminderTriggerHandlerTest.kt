package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.data.repository.TaskRepository
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequest
import com.sqhh99.punchreminder.domain.usecase.AlarmGateway
import com.sqhh99.punchreminder.domain.usecase.AppInstallChecker
import com.sqhh99.punchreminder.domain.usecase.NotificationGateway
import com.sqhh99.punchreminder.domain.usecase.ReminderTriggerHandler
import com.sqhh99.punchreminder.domain.usecase.TriggerOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

/** §16 集成式单测：用 fake 替代真实系统 API 验证触发→通知→重排逻辑。 */
class ReminderTriggerHandlerTest {

    // 2026-06-18 周四 09:00（Asia/Shanghai）
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
        override fun canScheduleExact() = true
    }

    private class FakeNotifier : NotificationGateway {
        data class Sent(val taskId: String, val openTargetApp: Boolean)
        val sent = mutableListOf<Sent>()
        override fun notify(task: PunchTask, openTargetApp: Boolean) {
            sent += Sent(task.id, openTargetApp)
        }
    }

    private class FakeInstall(private val installed: Boolean) : AppInstallChecker {
        override fun isInstalled(packageName: String?) = installed && !packageName.isNullOrBlank()
    }

    private fun handler(
        repo: TaskRepository,
        alarm: AlarmGateway,
        notifier: NotificationGateway,
        installed: Boolean,
    ) = ReminderTriggerHandler(
        repository = repo,
        notificationGateway = notifier,
        alarmGateway = alarm,
        installChecker = FakeInstall(installed),
        clock = clock,
    )

    private fun task(
        id: String = "t1",
        enabled: Boolean = true,
        autoLaunch: Boolean = true,
        schedule: TaskSchedule = TaskSchedule.Daily,
    ) = PunchTask(
        id = id, name = "上班打卡", hour = 9, minute = 0, schedule = schedule,
        targetPackage = "com.example", targetAppLabel = "企业微信",
        enabled = enabled, autoLaunch = autoLaunch,
    )

    @Test
    fun enabledInstalledAutoLaunch_notifiesAndReschedules() = runBlocking {
        val repo = FakeRepo(listOf(task()))
        val alarm = FakeAlarm()
        val notifier = FakeNotifier()
        val outcome = handler(repo, alarm, notifier, installed = true).handle("t1")

        assertEquals(TriggerOutcome.NOTIFIED_AND_RESCHEDULED, outcome)
        assertEquals(1, notifier.sent.size)
        assertTrue(notifier.sent.first().openTargetApp)
        assertEquals(1, alarm.scheduled.size)
    }

    @Test
    fun notInstalled_notifiesWithoutOpening() = runBlocking {
        val repo = FakeRepo(listOf(task()))
        val notifier = FakeNotifier()
        val outcome = handler(repo, FakeAlarm(), notifier, installed = false).handle("t1")

        assertEquals(TriggerOutcome.NOTIFIED_AND_RESCHEDULED, outcome)
        assertFalse(notifier.sent.first().openTargetApp)
    }

    @Test
    fun disabledTask_skips() = runBlocking {
        val repo = FakeRepo(listOf(task(enabled = false)))
        val notifier = FakeNotifier()
        val outcome = handler(repo, FakeAlarm(), notifier, installed = true).handle("t1")

        assertEquals(TriggerOutcome.SKIPPED_DISABLED, outcome)
        assertTrue(notifier.sent.isEmpty())
    }

    @Test
    fun missingTask_skips() = runBlocking {
        val outcome = handler(FakeRepo(), FakeAlarm(), FakeNotifier(), installed = true).handle("nope")
        assertEquals(TriggerOutcome.SKIPPED_MISSING_TASK, outcome)
    }

    @Test
    fun wrongDay_skipsNotifyButReschedules() = runBlocking {
        // 今天周四；任务只在周一执行
        val repo = FakeRepo(listOf(task(schedule = TaskSchedule.custom(setOf(DayOfWeek.MONDAY)))))
        val alarm = FakeAlarm()
        val notifier = FakeNotifier()
        val outcome = handler(repo, alarm, notifier, installed = true).handle("t1")

        assertEquals(TriggerOutcome.SKIPPED_WRONG_DAY, outcome)
        assertTrue(notifier.sent.isEmpty())
        assertEquals(1, alarm.scheduled.size)
    }
}
