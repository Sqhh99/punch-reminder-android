package com.sqhh99.punchreminder.data.repository

import com.sqhh99.punchreminder.domain.model.PunchTask
import kotlinx.coroutines.flow.Flow

/** 任务数据仓库。UI/ViewModel 只依赖此接口（实现方案 §5）。 */
interface TaskRepository {
    /** 当前所有任务，按创建顺序。数据变化时自动推送。 */
    val tasks: Flow<List<PunchTask>>

    /** 新增或更新（按 id 匹配）。 */
    suspend fun upsert(task: PunchTask)

    suspend fun delete(taskId: String)

    suspend fun setEnabled(taskId: String, enabled: Boolean)

    suspend fun getById(taskId: String): PunchTask?

    /** 一次性获取当前快照（用于广播接收等非 Flow 场景）。 */
    suspend fun getAll(): List<PunchTask>
}
