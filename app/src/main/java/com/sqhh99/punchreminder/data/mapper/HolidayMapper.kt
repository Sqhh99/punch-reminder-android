package com.sqhh99.punchreminder.data.mapper

import com.sqhh99.punchreminder.data.datastore.HolidayCacheDto
import com.sqhh99.punchreminder.data.datastore.TimorYearResponse
import com.sqhh99.punchreminder.domain.holiday.DayType
import java.time.LocalDate

/** 节假日数据的纯映射：API 响应 ↔ 缓存 DTO ↔ 内存日历 map。可单测。 */
object HolidayMapper {

    /**
     * 把某年的 API 响应转为 `date -> DayType` 条目。仅产出 HOLIDAY / MAKEUP_WORKDAY；
     * 每条 runCatching，畸形日期跳过。优先用条目自带的完整 [date]，否则用 "$year-$key" 组合。
     */
    fun toCacheEntries(year: Int, resp: TimorYearResponse): Map<LocalDate, DayType> =
        resp.holiday.entries.mapNotNull { (key, day) ->
            val date = runCatching {
                if (day.date.isNotBlank()) LocalDate.parse(day.date) else LocalDate.parse("$year-$key")
            }.getOrNull() ?: return@mapNotNull null
            val type = if (day.holiday) DayType.HOLIDAY else DayType.MAKEUP_WORKDAY
            date to type
        }.toMap()

    /**
     * 把某年的新条目并入既有缓存：**先剔除该年旧条目**再写入（纠正官方修订），更新 years 与拉取时间。
     */
    fun mergeIntoCacheDto(
        existing: HolidayCacheDto,
        year: Int,
        entries: Map<LocalDate, DayType>,
        fetchedEpochDay: Long,
    ): HolidayCacheDto {
        val kept = existing.days.filterKeys { key ->
            runCatching { LocalDate.parse(key).year != year }.getOrDefault(true)
        }
        val merged = kept + entries.mapKeys { it.key.toString() }.mapValues { it.value.name }
        val years = (existing.years + year).distinct().sorted()
        return existing.copy(days = merged, years = years, lastFetchedEpochDay = fetchedEpochDay)
    }

    /** 缓存 DTO → 内存日历 map（字符串 key 解析回 LocalDate，畸形条目跳过）。 */
    fun toCalendarMap(dto: HolidayCacheDto): Map<LocalDate, DayType> =
        dto.days.entries.mapNotNull { (key, value) ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@mapNotNull null
            val type = runCatching { DayType.valueOf(value) }.getOrNull() ?: return@mapNotNull null
            date to type
        }.toMap()
}
