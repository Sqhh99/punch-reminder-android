package com.sqhh99.punchreminder.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sqhh99.punchreminder.data.datastore.TaskDto
import com.sqhh99.punchreminder.data.mapper.TaskMapper
import com.sqhh99.punchreminder.domain.model.PunchTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 基于 Preferences DataStore 的任务仓库：单 key 存整张任务列表的 JSON。
 *
 * MVP 任务量小，整列表读写足够（实现方案 §2）。解析失败时回退空列表，保证不崩溃。
 * 接受外部注入的 [dataStore]，便于用临时文件 DataStore 做 JVM 单元测试。
 */
class DataStoreTaskRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = DefaultJson,
) : TaskRepository {

    override val tasks: Flow<List<PunchTask>> =
        dataStore.data.map { prefs -> decode(prefs[KEY]) }

    override suspend fun upsert(task: PunchTask) {
        mutate { current ->
            val index = current.indexOfFirst { it.id == task.id }
            if (index >= 0) current.toMutableList().also { it[index] = task }
            else current + task
        }
    }

    override suspend fun delete(taskId: String) {
        mutate { current -> current.filterNot { it.id == taskId } }
    }

    override suspend fun setEnabled(taskId: String, enabled: Boolean) {
        mutate { current ->
            current.map { if (it.id == taskId) it.copy(enabled = enabled) else it }
        }
    }

    override suspend fun getById(taskId: String): PunchTask? =
        getAll().firstOrNull { it.id == taskId }

    override suspend fun getAll(): List<PunchTask> = tasks.first()

    private suspend fun mutate(transform: (List<PunchTask>) -> List<PunchTask>) {
        dataStore.edit { prefs ->
            val updated = transform(decode(prefs[KEY]))
            prefs[KEY] = json.encodeToString(updated.map(TaskMapper::toDto))
        }
    }

    private fun decode(raw: String?): List<PunchTask> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<TaskDto>>(raw).map(TaskMapper::toDomain)
        }.getOrDefault(emptyList())
    }

    companion object {
        private val KEY = stringPreferencesKey("tasks_json")
        val DefaultJson = Json { ignoreUnknownKeys = true }
    }
}
