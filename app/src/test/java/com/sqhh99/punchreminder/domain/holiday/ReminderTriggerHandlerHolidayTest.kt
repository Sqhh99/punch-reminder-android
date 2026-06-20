package com.sqhh99.punchreminder.domain.holiday

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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTriggerHandlerHolidayTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun clockAt(y: Int, m: Int, d: Int, h: Int = 9): Clock =
        Clock.fixed(ZonedDateTime.of(y, m, d, h, 0, 0, 0, zone).toInstant(), zone)

    private class FakeRepo(initial: List<PunchTask>) : TaskRepository {
        private val state = MutableStateFlow(initial)
        override val tasks = state
        override suspend fun upsert(task: PunchTask) = state.update { it + task }
        override suspend fun delete(taskId: String) = state.update { it.filterNot { t -> t.id == taskId } }
        override suspend fun setEnabled(taskId: String, enabled: Boolean) =
            state.update { it.map { t -> if (t.id == taskId) t.copy(enabled = enabled) else t } }
        override suspend fun getById(taskId: String) = state.value.firstOrNull { it.id == taskId }
        override suspend fun getAll() = state.value
    }

    private class FakeAlarm : AlarmGateway {
        val scheduled = mutableListOf<AlarmScheduleRequest>()
        override fun schedule(request: AlarmScheduleRequest) { scheduled += request }
        override fun cancel(taskId: String) {}
        override fun cancelRepeats(taskId: String, maxRepeatIndex: Int) {}
        override fun canScheduleExact() = true
    }

    private class FakeNotifier : NotificationGateway {
        val sent = mutableListOf<String>()
        override fun notify(task: PunchTask, openTargetApp: Boolean) { sent += task.id }
    }

    private class FakeInstall : AppInstallChecker {
        override fun isInstalled(packageName: String?) = !packageName.isNullOrBlank()
    }

    private fun weekdaysTask(follow: Boolean = true) = PunchTask(
        id = "t1", name = "上班打卡", hour = 9, minute = 0,
        schedule = TaskSchedule.Weekdays, targetPackage = "com.example",
        followStatutoryCalendar = follow,
    )

    @Test
    fun holidayToday_skipsNotifyButReschedules_andLoadsCalendar() = runBlocking {
        // 2026-06-18 周四标放假
        val repo = FakeRepo(listOf(weekdaysTask()))
        val alarm = FakeAlarm()
        val notifier = FakeNotifier()
        var loaded = false
        val handler = ReminderTriggerHandler(
            repository = repo,
            notificationGateway = notifier,
            alarmGateway = alarm,
            installChecker = FakeInstall(),
            clock = clockAt(2026, 6, 18),
            holidayCalendar = FakeHolidayCalendar(mapOf(LocalDate.of(2026, 6, 18) to DayType.HOLIDAY)),
            ensureCalendarLoaded = { loaded = true },
        )

        val outcome = handler.handle("t1")

        assertEquals(TriggerOutcome.SKIPPED_WRONG_DAY, outcome)
        assertTrue(notifier.sent.isEmpty())
        assertEquals(1, alarm.scheduled.size) // 重排到下一个工作日
        assertTrue("应已调用 ensureCalendarLoaded", loaded)
    }

    @Test
    fun makeupSaturday_notifies() = runBlocking {
        // 2026-06-20 周六本不提醒；标为调休上班 → 照常提醒
        val repo = FakeRepo(listOf(weekdaysTask()))
        val notifier = FakeNotifier()
        val handler = ReminderTriggerHandler(
            repository = repo,
            notificationGateway = notifier,
            alarmGateway = FakeAlarm(),
            installChecker = FakeInstall(),
            clock = clockAt(2026, 6, 20),
            holidayCalendar = FakeHolidayCalendar(mapOf(LocalDate.of(2026, 6, 20) to DayType.MAKEUP_WORKDAY)),
        )

        val outcome = handler.handle("t1")

        assertEquals(TriggerOutcome.NOTIFIED_AND_RESCHEDULED, outcome)
        assertEquals(listOf("t1"), notifier.sent)
    }
}
