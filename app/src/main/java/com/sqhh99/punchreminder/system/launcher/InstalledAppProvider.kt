package com.sqhh99.punchreminder.system.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.sqhh99.punchreminder.domain.model.InstalledApp

/**
 * 读取手机中可从桌面启动的应用（实现方案 §4.3）。
 *
 * 使用 ACTION_MAIN + CATEGORY_LAUNCHER 的 queryIntentActivities，配合清单中的 <queries> 声明，
 * 避免使用 QUERY_ALL_PACKAGES 这一受限权限。
 */
class InstalledAppProvider(private val context: Context) {

    /** 返回原始可启动应用列表（未过滤/排序，交给 domain 层 InstalledAppFilter 处理）。 */
    fun loadLaunchableApps(): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.map { info ->
            InstalledApp(
                packageName = info.activityInfo.packageName,
                label = info.loadLabel(pm).toString(),
                launchable = true,
            )
        }
    }

    /** 按包名加载应用图标，供 UI 渲染（图标不进领域模型）。 */
    fun iconFor(packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
