package com.sqhh99.punchreminder.data.datastore

import kotlinx.serialization.Serializable

/**
 * 任务的持久化表示。用 kotlinx.serialization 序列化为 JSON 存入 DataStore。
 *
 * 故意使用稳定的原始类型（星期存 Int 1-7，周期类型存名称字符串），
 * 与领域模型解耦，便于未来字段演进。
 */
@Serializable
data class TaskDto(
    val id: String,
    val name: String,
    val hour: Int,
    val minute: Int,
    val scheduleType: String,
    val customDays: List<Int> = emptyList(),
    val targetPackage: String? = null,
    val targetAppLabel: String? = null,
    val enabled: Boolean = true,
    val autoLaunch: Boolean = true,
    val lockScreenAlert: Boolean = true,
    val repeatReminder: Boolean = false,
    val reminderIntervalMinutes: Int = 5,
    val maxReminderCount: Int = 2,
)
