package com.sqhh99.punchreminder.domain.holiday

import org.junit.Assert.assertEquals
import org.junit.Test

/** 穷举决策表：4 DayType × follow∈{true,false} × base∈{true,false}。 */
class StatutoryDayDecisionTest {

    @Test
    fun followFalse_alwaysEqualsBase() {
        for (type in DayType.values()) {
            assertEquals(true, StatutoryDayDecision.shouldRemind(base = true, follow = false, dayType = type))
            assertEquals(false, StatutoryDayDecision.shouldRemind(base = false, follow = false, dayType = type))
        }
    }

    @Test
    fun followTrue_holiday_alwaysFalse() {
        assertEquals(false, StatutoryDayDecision.shouldRemind(base = true, follow = true, dayType = DayType.HOLIDAY))
        assertEquals(false, StatutoryDayDecision.shouldRemind(base = false, follow = true, dayType = DayType.HOLIDAY))
    }

    @Test
    fun followTrue_makeupWorkday_alwaysTrue() {
        assertEquals(true, StatutoryDayDecision.shouldRemind(base = true, follow = true, dayType = DayType.MAKEUP_WORKDAY))
        assertEquals(true, StatutoryDayDecision.shouldRemind(base = false, follow = true, dayType = DayType.MAKEUP_WORKDAY))
    }

    @Test
    fun followTrue_workdayOrUnknown_equalsBase() {
        for (type in listOf(DayType.WORKDAY, DayType.UNKNOWN)) {
            assertEquals(true, StatutoryDayDecision.shouldRemind(base = true, follow = true, dayType = type))
            assertEquals(false, StatutoryDayDecision.shouldRemind(base = false, follow = true, dayType = type))
        }
    }
}
