package com.sqhh99.punchreminder.domain.model

/**
 * 手机上已安装、可从桌面启动的应用（实现方案 §4.3）。
 * 图标不放在领域模型里，由 system 层按包名按需提供，保持领域层纯净可测。
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val launchable: Boolean = true,
)
