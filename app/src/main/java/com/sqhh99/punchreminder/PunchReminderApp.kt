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
            container.taskScheduler.rescheduleAll()
        }
    }
}
