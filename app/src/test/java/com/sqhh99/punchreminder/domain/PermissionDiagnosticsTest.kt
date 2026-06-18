package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.domain.permission.PermissionDiagnostics
import com.sqhh99.punchreminder.domain.permission.PermissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionDiagnosticsTest {

    @Test
    fun build_keepsStableOrderAndStatuses() {
        val items = PermissionDiagnostics.build(
            notificationGranted = true,
            exactAlarmGranted = false,
            fullScreenGranted = true,
        )
        assertEquals(
            listOf(PermissionType.NOTIFICATION, PermissionType.EXACT_ALARM, PermissionType.FULL_SCREEN),
            items.map { it.type },
        )
        assertTrue(items[0].granted)
        assertFalse(items[1].granted)
        assertTrue(items[2].granted)
    }

    @Test
    fun allGranted_trueOnlyWhenEveryItemGranted() {
        assertTrue(PermissionDiagnostics.allGranted(PermissionDiagnostics.build(true, true, true)))
        assertFalse(PermissionDiagnostics.allGranted(PermissionDiagnostics.build(true, false, true)))
    }
}
