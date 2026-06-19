package com.sqhh99.punchreminder.system.holiday

import com.sqhh99.punchreminder.data.datastore.TimorYearResponse
import com.sqhh99.punchreminder.data.repository.DataStoreTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * 节假日 API 客户端。用 [HttpURLConnection]（无新增依赖）拉取 timor.tech 整年数据。
 *
 * [fetchYear] **永不抛异常**：网络/HTTP/解析任一失败或 `code!=0` 都返回 null，由调用方保留上次好缓存。
 * 仅在机会性刷新时调用，绝不在闹钟触发关键路径调用。
 */
class HolidayApiClient(
    private val json: Json = DataStoreTaskRepository.DefaultJson,
) {

    suspend fun fetchYear(year: Int): TimorYearResponse? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://timor.tech/api/holiday/year/$year").openConnection() as HttpURLConnection)
                .apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }
            try {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<TimorYearResponse>(body).takeIf { it.code == 0 }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private companion object {
        const val TIMEOUT_MS = 8000
    }
}
