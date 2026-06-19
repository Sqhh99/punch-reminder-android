package com.sqhh99.punchreminder.data

import com.sqhh99.punchreminder.data.datastore.HolidayCacheDto
import com.sqhh99.punchreminder.data.datastore.TimorYearResponse
import com.sqhh99.punchreminder.data.mapper.HolidayMapper
import com.sqhh99.punchreminder.data.repository.DataStoreTaskRepository
import com.sqhh99.punchreminder.domain.holiday.DayType
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayMapperTest {

    private val json = DataStoreTaskRepository.DefaultJson

    private val sampleJson = """
        {
          "code": 0,
          "holiday": {
            "10-01": { "holiday": true,  "name": "国庆节",     "date": "2026-10-01" },
            "10-11": { "holiday": false, "name": "国庆节后调休", "date": "2026-10-11" },
            "bad":   { "holiday": true,  "name": "畸形",       "date": "not-a-date" }
          }
        }
    """.trimIndent()

    @Test
    fun parsesResponse_andMapsTypes_skippingMalformed() {
        val resp = json.decodeFromString<TimorYearResponse>(sampleJson)
        assertEquals(0, resp.code)

        val entries = HolidayMapper.toCacheEntries(2026, resp)
        assertEquals(DayType.HOLIDAY, entries[LocalDate.of(2026, 10, 1)])
        assertEquals(DayType.MAKEUP_WORKDAY, entries[LocalDate.of(2026, 10, 11)])
        // 畸形日期被跳过
        assertEquals(2, entries.size)
    }

    @Test
    fun ignoresUnknownExtraFields() {
        val withExtra = """{"code":0,"holiday":{},"extraField":123,"type":{"x":1}}"""
        val resp = json.decodeFromString<TimorYearResponse>(withExtra)
        assertEquals(0, resp.code)
        assertTrue(resp.holiday.isEmpty())
    }

    @Test
    fun nonZeroCode_isDetectable() {
        val resp = json.decodeFromString<TimorYearResponse>("""{"code":-1,"holiday":{}}""")
        assertFalse(resp.code == 0)
    }

    @Test
    fun mergeReplacesPriorYearEntries_andRoundTripsCalendarMap() {
        val entries = HolidayMapper.toCacheEntries(2026, json.decodeFromString(sampleJson))
        val merged = HolidayMapper.mergeIntoCacheDto(HolidayCacheDto(), 2026, entries, fetchedEpochDay = 100L)

        assertEquals(listOf(2026), merged.years)
        assertEquals(100L, merged.lastFetchedEpochDay)

        // 用一份修订后的数据（去掉调休日）替换同一年，旧条目应被剔除。
        val revised = mapOf(LocalDate.of(2026, 10, 1) to DayType.HOLIDAY)
        val merged2 = HolidayMapper.mergeIntoCacheDto(merged, 2026, revised, fetchedEpochDay = 200L)
        val map = HolidayMapper.toCalendarMap(merged2)

        assertEquals(DayType.HOLIDAY, map[LocalDate.of(2026, 10, 1)])
        assertFalse(map.containsKey(LocalDate.of(2026, 10, 11))) // 旧调休日已被替换剔除
        assertEquals(1, map.size)
    }

    @Test
    fun mergeKeepsOtherYears() {
        val y2026 = HolidayMapper.mergeIntoCacheDto(
            HolidayCacheDto(), 2026,
            mapOf(LocalDate.of(2026, 10, 1) to DayType.HOLIDAY), 100L,
        )
        val both = HolidayMapper.mergeIntoCacheDto(
            y2026, 2027,
            mapOf(LocalDate.of(2027, 1, 1) to DayType.HOLIDAY), 100L,
        )
        val map = HolidayMapper.toCalendarMap(both)
        assertEquals(listOf(2026, 2027), both.years)
        assertEquals(DayType.HOLIDAY, map[LocalDate.of(2026, 10, 1)])
        assertEquals(DayType.HOLIDAY, map[LocalDate.of(2027, 1, 1)])
    }
}
