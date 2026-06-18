package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import com.sqhh99.punchreminder.domain.usecase.AppLaunchDecision
import com.sqhh99.punchreminder.domain.usecase.LaunchAction
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLaunchDecisionTest {

    private val decision = AppLaunchDecision()

    private fun task(pkg: String? = "com.example", autoLaunch: Boolean = true) =
        PunchTask(
            name = "t", hour = 9, minute = 0, schedule = TaskSchedule.Daily,
            targetPackage = pkg, autoLaunch = autoLaunch,
        )

    @Test
    fun installedAndAutoLaunch_launchesDirectly() {
        assertEquals(LaunchAction.LAUNCH_DIRECTLY, decision.decide(task(), targetInstalled = true))
    }

    @Test
    fun installedButAutoLaunchOff_notifyOnly() {
        assertEquals(
            LaunchAction.NOTIFY_ONLY,
            decision.decide(task(autoLaunch = false), targetInstalled = true),
        )
    }

    @Test
    fun notInstalled_promptReselect() {
        assertEquals(LaunchAction.PROMPT_RESELECT, decision.decide(task(), targetInstalled = false))
    }

    @Test
    fun noTargetApp_promptReselect() {
        assertEquals(
            LaunchAction.PROMPT_RESELECT,
            decision.decide(task(pkg = null), targetInstalled = true),
        )
    }
}
