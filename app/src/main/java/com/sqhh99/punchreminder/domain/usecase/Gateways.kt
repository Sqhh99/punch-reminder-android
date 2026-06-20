package com.sqhh99.punchreminder.domain.usecase

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.scheduler.AlarmScheduleRequest

/**
 * 领域层对系统能力的抽象网关（实现方案 §16：用 fake 替代真实系统 API 进行测试）。
 * system 层的具体类实现这些接口。
 */
interface AlarmGateway {
    fun schedule(request: AlarmScheduleRequest)
    fun cancel(taskId: String)

    /** 取消某任务今天剩余的重复提醒闹钟（repeatIndex 1..maxRepeatIndex），不影响次日日常闹钟(index 0)。 */
    fun cancelRepeats(taskId: String, maxRepeatIndex: Int)
    fun canScheduleExact(): Boolean
}

interface NotificationGateway {
    fun notify(task: PunchTask, openTargetApp: Boolean)
}

interface AppInstallChecker {
    fun isInstalled(packageName: String?): Boolean
}
