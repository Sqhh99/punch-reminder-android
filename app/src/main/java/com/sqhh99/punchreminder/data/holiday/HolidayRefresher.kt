package com.sqhh99.punchreminder.data.holiday

import com.sqhh99.punchreminder.data.datastore.HolidayCacheDto
import com.sqhh99.punchreminder.data.mapper.HolidayMapper
import com.sqhh99.punchreminder.data.repository.HolidayCacheRepository
import com.sqhh99.punchreminder.system.holiday.HolidayApiClient
import java.time.Clock
import java.time.LocalDate

/**
 * 机会性刷新节假日缓存（在应用启动/打开时调用，节流）。
 *
 * 流程：读缓存 → 若不陈旧则只把缓存装入内存日历并返回 → 否则拉取**今年+明年**（跨年边界），
 * 合并保存并更新内存日历。任一年拉取失败则跳过该年（保留上次好数据）。绝不在闹钟触发关键路径调用。
 */
class HolidayRefresher(
    private val apiClient: HolidayApiClient,
    private val cacheRepo: HolidayCacheRepository,
    private val calendar: InMemoryHolidayCalendar,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    suspend fun refreshIfStale(minIntervalDays: Long = DEFAULT_MIN_INTERVAL_DAYS) {
        val existing = cacheRepo.load()
        val today = LocalDate.now(clock)

        if (!isStale(existing, today, minIntervalDays)) {
            calendar.replace(HolidayMapper.toCalendarMap(existing))
            return
        }

        var merged = existing
        var changed = false
        for (year in listOf(today.year, today.year + 1)) {
            val resp = apiClient.fetchYear(year) ?: continue // 失败保留上次好数据
            merged = HolidayMapper.mergeIntoCacheDto(
                existing = merged,
                year = year,
                entries = HolidayMapper.toCacheEntries(year, resp),
                fetchedEpochDay = today.toEpochDay(),
            )
            changed = true
        }
        if (changed) cacheRepo.save(merged)
        calendar.replace(HolidayMapper.toCalendarMap(merged))
    }

    private fun isStale(dto: HolidayCacheDto, today: LocalDate, minIntervalDays: Long): Boolean {
        // 覆盖年份缺今年或明年 → 必须刷新（跨年边界）。
        if (today.year !in dto.years || (today.year + 1) !in dto.years) return true
        if (dto.lastFetchedEpochDay <= 0L) return true
        return today.toEpochDay() - dto.lastFetchedEpochDay >= minIntervalDays
    }

    private companion object {
        const val DEFAULT_MIN_INTERVAL_DAYS = 7L
    }
}
