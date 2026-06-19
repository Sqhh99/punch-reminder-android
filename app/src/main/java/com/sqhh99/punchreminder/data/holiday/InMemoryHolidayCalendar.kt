package com.sqhh99.punchreminder.data.holiday

import com.sqhh99.punchreminder.data.mapper.HolidayMapper
import com.sqhh99.punchreminder.data.repository.HolidayCacheRepository
import com.sqhh99.punchreminder.domain.holiday.DayType
import com.sqhh99.punchreminder.domain.holiday.HolidayCalendar
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

/**
 * 运行时 [HolidayCalendar] 实现：内存快照（[AtomicReference]，线程安全），[dayType] 同步无 IO，
 * 可安全用于闹钟触发关键路径。
 *
 * [ensureLoaded] 从 [HolidayCacheRepository] 装载一次（幂等，Mutex 守护）；[replace] 供刷新后更新快照。
 */
class InMemoryHolidayCalendar(
    private val cacheRepo: HolidayCacheRepository,
) : HolidayCalendar {

    private val snapshot = AtomicReference<Map<LocalDate, DayType>>(emptyMap())
    private val loadMutex = Mutex()
    @Volatile private var loaded = false

    override fun dayType(date: LocalDate): DayType = snapshot.get()[date] ?: DayType.UNKNOWN

    suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            snapshot.set(HolidayMapper.toCalendarMap(cacheRepo.load()))
            loaded = true
        }
    }

    fun replace(map: Map<LocalDate, DayType>) {
        snapshot.set(map)
        loaded = true
    }
}
