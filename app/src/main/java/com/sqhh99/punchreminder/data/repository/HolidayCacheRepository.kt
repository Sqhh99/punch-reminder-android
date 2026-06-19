package com.sqhh99.punchreminder.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sqhh99.punchreminder.data.datastore.HolidayCacheDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 节假日缓存仓库：复用任务用的同一个 DataStore（不另开文件），单独一个 key 存缓存 JSON。
 * 解析失败时回退空缓存，保证永不崩溃。
 */
class HolidayCacheRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = DataStoreTaskRepository.DefaultJson,
) {

    suspend fun load(): HolidayCacheDto =
        dataStore.data.map { prefs -> decode(prefs[KEY]) }.first()

    suspend fun save(dto: HolidayCacheDto) {
        dataStore.edit { prefs -> prefs[KEY] = json.encodeToString(dto) }
    }

    private fun decode(raw: String?): HolidayCacheDto {
        if (raw.isNullOrBlank()) return HolidayCacheDto()
        return runCatching { json.decodeFromString<HolidayCacheDto>(raw) }.getOrDefault(HolidayCacheDto())
    }

    private companion object {
        val KEY = stringPreferencesKey("holiday_cache_json")
    }
}
