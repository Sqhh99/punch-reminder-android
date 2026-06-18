package com.sqhh99.punchreminder.system.launcher

import android.content.Context
import android.content.Intent
import com.sqhh99.punchreminder.domain.usecase.AppInstallChecker

/** 启动目标应用的结果。 */
enum class LaunchResult {
    SUCCESS,
    NOT_INSTALLED,
    NO_LAUNCH_INTENT,
    FAILED,
}

/**
 * 封装通过包名打开其他应用（实现方案 §5：系统能力封装在 system 层）。
 * 仅使用 PackageManager 的标准启动 Intent，不做任何后台保活或越权操作。
 */
class AppLauncher(private val context: Context) : AppInstallChecker {

    override fun isInstalled(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return context.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    fun launch(packageName: String?): LaunchResult {
        if (packageName.isNullOrBlank()) return LaunchResult.NOT_INSTALLED
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return LaunchResult.NO_LAUNCH_INTENT
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            LaunchResult.SUCCESS
        } catch (e: Exception) {
            LaunchResult.FAILED
        }
    }
}
