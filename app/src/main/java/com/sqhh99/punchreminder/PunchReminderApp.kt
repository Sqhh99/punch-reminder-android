package com.sqhh99.punchreminder

import android.app.Application
import com.sqhh99.punchreminder.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Application 入口，持有全局 [AppContainer]（手动依赖容器）。 */
class PunchReminderApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationDispatcher.ensureChannel()
        applicationScope.launch {
            // 先装载本地节假日缓存，再机会性联网刷新（失败不阻塞），最后按最新数据重排闹钟。
            container.holidayCalendar.ensureLoaded()
            runCatching { container.holidayRefresher.refreshIfStale() }
            container.taskScheduler.rescheduleAll()
        }
    }
}
