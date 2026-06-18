package com.sqhh99.punchreminder

import android.app.Application
import com.sqhh99.punchreminder.di.AppContainer

/** Application 入口，持有全局 [AppContainer]（手动依赖容器）。 */
class PunchReminderApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationDispatcher.ensureChannel()
    }
}
