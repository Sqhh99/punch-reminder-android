package com.sqhh99.punchreminder.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sqhh99.punchreminder.data.repository.DataStoreTaskRepository
import com.sqhh99.punchreminder.data.repository.TaskRepository
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequestBuilder
import com.sqhh99.punchreminder.domain.scheduler.NextTriggerTimeCalculator
import com.sqhh99.punchreminder.domain.usecase.ReminderTriggerHandler
import com.sqhh99.punchreminder.domain.usecase.TaskScheduler
import com.sqhh99.punchreminder.system.alarm.AlarmScheduler
import com.sqhh99.punchreminder.system.launcher.AppLauncher
import com.sqhh99.punchreminder.system.launcher.InstalledAppProvider
import com.sqhh99.punchreminder.system.notification.NotificationDispatcher
import com.sqhh99.punchreminder.system.permission.PermissionInspector
import com.sqhh99.punchreminder.system.permission.PermissionSettingsLauncher
import com.sqhh99.punchreminder.viewmodel.AppPickerViewModel
import com.sqhh99.punchreminder.viewmodel.PermissionViewModel
import com.sqhh99.punchreminder.viewmodel.TaskEditViewModel
import com.sqhh99.punchreminder.viewmodel.TaskListViewModel

/**
 * 轻量手动依赖容器（MVP 阶段不使用 DI 框架，实现方案 §2/§22）。
 * 由 [com.sqhh99.punchreminder.PunchReminderApp] 持有，按需懒加载单例。
 */
class AppContainer(private val appContext: Context) {

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create {
            appContext.preferencesDataStoreFile("tasks")
        }
    }

    val repository: TaskRepository by lazy { DataStoreTaskRepository(dataStore) }

    val calculator: NextTriggerTimeCalculator by lazy { NextTriggerTimeCalculator() }
    private val requestBuilder: AlarmScheduleRequestBuilder by lazy { AlarmScheduleRequestBuilder(calculator) }

    val appLauncher: AppLauncher by lazy { AppLauncher(appContext) }
    val installedAppProvider: InstalledAppProvider by lazy { InstalledAppProvider(appContext) }
    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(appContext) }
    val notificationDispatcher: NotificationDispatcher by lazy { NotificationDispatcher(appContext) }
    val permissionSettingsLauncher: PermissionSettingsLauncher by lazy { PermissionSettingsLauncher(appContext) }
    private val permissionInspector: PermissionInspector by lazy { PermissionInspector(appContext, alarmScheduler) }

    val taskScheduler: TaskScheduler by lazy {
        TaskScheduler(repository, alarmScheduler, requestBuilder)
    }

    val reminderTriggerHandler: ReminderTriggerHandler by lazy {
        ReminderTriggerHandler(
            repository = repository,
            notificationGateway = notificationDispatcher,
            alarmGateway = alarmScheduler,
            installChecker = appLauncher,
            requestBuilder = requestBuilder,
        )
    }

    val ownPackage: String get() = appContext.packageName

    // --- ViewModel 工厂（无 DI，手动构造） ---

    fun taskListFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { TaskListViewModel(repository, taskScheduler, calculator) }
    }

    fun taskEditFactory(taskId: String?): ViewModelProvider.Factory = viewModelFactory {
        initializer { TaskEditViewModel(repository, taskScheduler, appLauncher, taskId = taskId) }
    }

    fun appPickerFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { AppPickerViewModel(installedAppProvider, ownPackage) }
    }

    fun permissionFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { PermissionViewModel(permissionInspector) }
    }
}
