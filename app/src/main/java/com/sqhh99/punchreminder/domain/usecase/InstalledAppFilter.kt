package com.sqhh99.punchreminder.domain.usecase

import com.sqhh99.punchreminder.domain.model.InstalledApp

/**
 * 整理可启动应用列表（实现方案 §15.1 必测模块）。纯函数：
 * - 只保留可启动应用
 * - 按包名去重
 * - 可选排除本应用自身
 * - 可选按关键字过滤（名称或包名，忽略大小写）
 * - 按显示名（忽略大小写）排序
 */
class InstalledAppFilter {

    fun filter(
        apps: List<InstalledApp>,
        ownPackage: String? = null,
        query: String = "",
    ): List<InstalledApp> {
        val trimmedQuery = query.trim()
        return apps.asSequence()
            .filter { it.launchable }
            .filter { it.packageName != ownPackage }
            .distinctBy { it.packageName }
            .filter { app ->
                trimmedQuery.isEmpty() ||
                    app.label.contains(trimmedQuery, ignoreCase = true) ||
                    app.packageName.contains(trimmedQuery, ignoreCase = true)
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
