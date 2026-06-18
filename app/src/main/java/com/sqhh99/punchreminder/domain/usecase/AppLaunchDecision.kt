package com.sqhh99.punchreminder.domain.usecase

import com.sqhh99.punchreminder.domain.model.PunchTask

/** 到点时对目标应用应采取的动作（实现方案 §15.1 / §20）。 */
enum class LaunchAction {
    /** 系统允许时直接尝试打开目标应用。 */
    LAUNCH_DIRECTLY,

    /** 不自动打开，仅发通知，由用户点击打开。 */
    NOTIFY_ONLY,

    /** 目标应用未配置或已卸载，提示用户重新选择。 */
    PROMPT_RESELECT,
}

/**
 * 决定到点时如何处理目标应用。纯函数，可单测。
 *
 * 注意：是否「真的能」后台拉起由系统决定（§20）；这里只表达业务意图，
 * system 层据此尝试启动或仅发通知。
 */
class AppLaunchDecision {

    fun decide(task: PunchTask, targetInstalled: Boolean): LaunchAction = when {
        !task.hasTargetApp || !targetInstalled -> LaunchAction.PROMPT_RESELECT
        task.autoLaunch -> LaunchAction.LAUNCH_DIRECTLY
        else -> LaunchAction.NOTIFY_ONLY
    }
}
