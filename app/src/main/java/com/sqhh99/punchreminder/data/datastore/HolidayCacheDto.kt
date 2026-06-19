package com.sqhh99.punchreminder.data.datastore

import kotlinx.serialization.Serializable

/**
 * 节假日缓存的持久化表示（存入 DataStore 单 key 的 JSON）。
 *
 * 只存特殊日：[days] 的 key 为 "yyyy-MM-dd"，value 为 [com.sqhh99.punchreminder.domain.holiday.DayType] 的
 * 名称（仅 HOLIDAY / MAKEUP_WORKDAY）。未列出的日期查询时即 UNKNOWN。
 *
 * @param years 本缓存覆盖的年份集合。
 * @param lastFetchedEpochDay 最近一次成功拉取的 epochDay，用于刷新节流。
 */
@Serializable
data class HolidayCacheDto(
    val version: Int = 1,
    val years: List<Int> = emptyList(),
    val days: Map<String, String> = emptyMap(),
    val lastFetchedEpochDay: Long = 0L,
)
