package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.validation.TaskValidationError
import com.sqhh99.punchreminder.domain.validation.TaskValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskValidatorTest {

    private val validator = TaskValidator()

    @Test
    fun validTask_passes() {
        val task = PunchTask(name = "上班", hour = 9, minute = 0, schedule = TaskSchedule.Weekdays)
        assertTrue(validator.isValid(task))
    }

    @Test
    fun blankName_reported() {
        val task = PunchTask(name = "  ", hour = 9, minute = 0, schedule = TaskSchedule.Daily)
        assertTrue(TaskValidationError.EMPTY_NAME in validator.validate(task))
    }

    @Test
    fun customWithNoDays_reported() {
        val task = PunchTask(name = "x", hour = 9, minute = 0, schedule = TaskSchedule.custom(emptySet()))
        assertTrue(TaskValidationError.NO_CUSTOM_DAY in validator.validate(task))
        assertFalse(validator.isValid(task))
    }

    @Test
    fun invalidTime_reported() {
        val task = PunchTask(name = "x", hour = 25, minute = 70, schedule = TaskSchedule.Daily)
        assertEquals(listOf(TaskValidationError.INVALID_TIME), validator.validate(task))
    }
}
