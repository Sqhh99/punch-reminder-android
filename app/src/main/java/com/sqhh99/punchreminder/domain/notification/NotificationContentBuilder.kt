package com.sqhh99.punchreminder.domain.notification

import com.sqhh99.punchreminder.domain.model.PunchTask

/** 通知文案。纯数据，由 system 层渲染为系统通知。 */
data class NotificationContent(
    val title: String,
    val text: String,
    val showOpenAction: Boolean,
)

/**
 * 构建提醒通知文案（实现方案 §15.1 / §19.2：站在用户角度，不含技术术语）。纯函数。
 */
class NotificationContentBuilder {

    fun build(task: PunchTask): NotificationContent {
        val appLabel = task.targetAppLabel?.takeIf { it.isNotBlank() }
        val title = "该打卡啦：${task.name}"
        val text = when {
            appLabel != null && task.autoLaunch -> "正在尝试打开「$appLabel」，点击立即打开"
            appLabel != null -> "点击打开「$appLabel」完成打卡"
            else -> "点击查看任务并完成打卡"
        }
        return NotificationContent(
            title = title,
            text = text,
            showOpenAction = appLabel != null,
        )
    }
}
