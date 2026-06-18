package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.notification.NotificationContentBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationContentBuilderTest {

    private val builder = NotificationContentBuilder()

    private fun task(label: String? = null, autoLaunch: Boolean = true, lockScreenAlert: Boolean = true) =
        PunchTask(
            name = "上班打卡", hour = 9, minute = 0, schedule = TaskSchedule.Daily,
            targetPackage = label?.let { "com.example" }, targetAppLabel = label,
            autoLaunch = autoLaunch, lockScreenAlert = lockScreenAlert,
        )

    @Test
    fun includesTaskNameInTitle() {
        assertTrue(builder.build(task()).title.contains("上班打卡"))
    }

    @Test
    fun withTargetApp_showsOpenActionAndAppName() {
        val content = builder.build(task(label = "企业微信"))
        assertTrue(content.showOpenAction)
        assertTrue(content.text.contains("企业微信"))
    }

    @Test
    fun withoutTargetApp_noOpenAction() {
        val content = builder.build(task(label = null))
        assertFalse(content.showOpenAction)
    }

    @Test
    fun lockScreenAlertOn_usesFullScreen() {
        assertTrue(builder.build(task(lockScreenAlert = true)).useFullScreen)
    }

    @Test
    fun lockScreenAlertOff_noFullScreen() {
        assertFalse(builder.build(task(lockScreenAlert = false)).useFullScreen)
    }
}
