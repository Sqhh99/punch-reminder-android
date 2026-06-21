<p align="center">
  <img src="icons/android/playstore-icon.png" width="120" alt="Punch Reminder icon">
</p>

<h1 align="center">Punch Reminder</h1>

<p align="center">
  上下班打卡提醒 / 定时启动助手，帮助你在合适的时间打开目标打卡应用。
</p>

<p align="center">
  <a href="https://github.com/Sqhh99/punch-reminder-android/actions/workflows/pr-check.yml"><img src="https://github.com/Sqhh99/punch-reminder-android/actions/workflows/pr-check.yml/badge.svg" alt="PR Check"></a>
  <a href="https://github.com/Sqhh99/punch-reminder-android/actions/workflows/main-build.yml"><img src="https://github.com/Sqhh99/punch-reminder-android/actions/workflows/main-build.yml/badge.svg" alt="Main Build"></a>
  <a href="https://github.com/Sqhh99/punch-reminder-android/actions/workflows/release.yml"><img src="https://github.com/Sqhh99/punch-reminder-android/actions/workflows/release.yml/badge.svg" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-AGPL--3.0-blue.svg" alt="License: AGPL-3.0"></a>
</p>

## 简介

Punch Reminder 是一个 Android 打卡提醒工具。你可以创建上下班提醒任务，选择目标打卡应用，到点后通过通知、锁屏提醒或重复提醒降低忘记打卡的概率。

## 主要功能

- 创建、编辑、启用或停用打卡提醒任务。
- 选择已安装应用作为提醒后打开的目标应用。
- 使用 AlarmManager 定时触发通知提醒。
- 点击通知打开目标应用或返回本应用。
- 支持锁屏强提醒和重复提醒。
- 支持法定节假日 / 调休感知，联网拉取后可离线缓存使用。
- 支持开机、时间变更、覆盖安装后重新注册提醒。
- 使用前台服务降低部分国产 ROM 清理后台后漏提醒的概率。

## 能力边界

本项目只做提醒和辅助打开应用，不破解第三方打卡软件，不模拟点击，不伪造定位，不绕过人脸识别，也不读取打卡账号密码。

Android 厂商系统、省电策略、后台限制和通知权限会影响提醒稳定性。建议为本应用开启通知、自启动、后台活动权限，并关闭电池优化。

## 状态栏图标说明

原生 Android 自 5.0 起会丢弃通知小图标的颜色，状态栏图标**强制为单色白色剪影**，应用代码无法让其变彩色。要在状态栏看到本应用的彩色图标，需开启厂商系统的「用应用图标显示状态栏通知」开关——该功能由 ROM 用应用的**桌面启动图标**替换状态栏图标实现：

- ColorOS / Realme UI：设置 → 通知与状态栏 →（状态栏）显示应用图标。
- MIUI / HyperOS：设置 → 通知 → 状态栏 → 使用应用图标。

开启后状态栏即显示本应用彩色图标；未开启或原生系统下，显示干净的单色「签到」剪影，下拉通知内仍带品牌色小图标与彩色大图标。

## 本地构建

环境要求：

- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin / Gradle 版本以仓库配置为准

常用命令：

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lint
```

## 项目结构

```text
app/src/main/java/com/sqhh99/punchreminder/
├── ui/          Jetpack Compose 页面与主题
├── viewmodel/   页面状态管理
├── domain/      业务模型、规则、调度计算和用例
├── data/        本地存储、DTO、Mapper 和 Repository
└── system/      通知、闹钟、权限、广播、应用启动等 Android 系统能力封装
```

## 许可证

本项目基于 [GNU Affero General Public License v3.0](LICENSE) 开源。
