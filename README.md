# 打卡提醒助手 (punch-reminder-android)

[![PR Check](https://github.com/Sqhh99/punch-reminder-android/actions/workflows/pr-check.yml/badge.svg)](https://github.com/Sqhh99/punch-reminder-android/actions/workflows/pr-check.yml)
[![Main Build](https://github.com/Sqhh99/punch-reminder-android/actions/workflows/main-build.yml/badge.svg)](https://github.com/Sqhh99/punch-reminder-android/actions/workflows/main-build.yml)
[![Release](https://github.com/Sqhh99/punch-reminder-android/actions/workflows/release.yml/badge.svg)](https://github.com/Sqhh99/punch-reminder-android/actions/workflows/release.yml)

上下班打卡提醒助手 / 定时应用启动助手。在用户授权和主动配置的前提下，于指定时间提醒用户，
并尽可能自动拉起指定打卡应用，降低忘记打卡的概率。

> 详细需求见 [需求说明](Requirements-Specification-for-Android-Scheduled-Clock-in-Tool.md)，
> 实现方案见 [实现方案](Android-Clock-in-Reminder-Assistant-Implementation-Plan-CICD-and-Testing.md)。

## 技术栈

- Kotlin + Jetpack Compose + Material3
- MVVM（手动构造依赖，MVP 阶段不引入 DI 框架）
- DataStore（任务本地存储，后续里程碑接入）
- AlarmManager + BroadcastReceiver（定时与后台触发，后续里程碑接入）
- Notification + Full-screen Intent（提醒，后续里程碑接入）
- GitHub Actions（CI/CD）

构建配置：`minSdk 26`、`compileSdk/targetSdk 35`、JDK 17、Gradle 8.11.1、AGP 8.7.x。

## 工程结构（实现方案 §5）

```
app/src/main/java/com/sqhh99/punchreminder/
 ├─ ui/          Compose 页面与组件
 ├─ viewmodel/   页面状态管理
 ├─ domain/      业务模型、业务规则、用例、调度计算（可单元测试）
 ├─ data/        任务存储、Repository
 └─ system/      Android 系统能力封装（Alarm/Receiver/Notification/Launcher/Permission）
```

约束：UI 层不得直接调用 AlarmManager / PackageManager / NotificationManager；系统 API 封装在 `system` 层；
时间计算逻辑放在 `domain` 层以便单元测试。

## 本地构建

```bash
./gradlew assembleDebug        # 构建 debug APK
./gradlew testDebugUnitTest    # 运行本地单元测试
./gradlew lint                 # Android Lint
```

需要本地安装 Android SDK（platform 35、build-tools）与 JDK 17。CI 已自动具备该环境。

## 分支与 PR 约定（实现方案 §6 / §7）

- 禁止直接提交 `main`；所有变更通过 Pull Request 合并，且必须通过 CI。
- 分支命名：`feature/*`、`fix/*`、`test/*`、`ci/*`。
- 提交信息：`feat` / `fix` / `test` / `ci` / `docs` / `refactor` / `chore`。

## CI/CD

- **PR Check**（`.github/workflows/pr-check.yml`）：wrapper 校验 + Lint + 单元测试 + debug 构建，快速反馈。
- **Main Build**（`.github/workflows/main-build.yml`）：合并到 main 后产出可追溯命名的 debug APK
  （`PunchReminder-debug-<version>-main-<shortsha>.apk`）及测试/lint 报告 artifact。
- **Release**（`.github/workflows/release.yml`）：推送 `v*` tag 后自动构建并发布到 GitHub Releases。
- nightly 流水线将在后续里程碑加入。

## 发布（Release）

本地打 tag 即触发自动发布，APK 作为附件出现在 [Releases](https://github.com/Sqhh99/punch-reminder-android/releases) 页：

```bash
# 确认 app/build.gradle.kts 的 versionName 与 tag 一致（去掉前缀 v），例如 0.5.0
git tag v0.5.0 -m "0.5.0 开机恢复 + Release 流水线"
git push origin v0.5.0
```

工作流构建 `PunchReminder-v<tag>-debug.apk` 并创建对应 Release。当前为 **debug 签名**包，可直接安装；
正式 keystore 签名（通过 GitHub Secrets 注入，不提交密钥）留待后续里程碑。

## 能力边界（实现方案 §20）

本软件**不是**自动打卡作弊器，也不破解第三方打卡软件。不模拟点击、不伪造定位、不绕过人脸识别、
不读取打卡账号密码。仅在系统允许时帮助用户按时打开已安装的打卡应用。

- 可稳定保证：任务保存与管理、到点通知、点击通知打开目标 App、开机后重新注册任务。
- 尽量尝试：锁屏弹出提醒、系统允许时自动打开目标 App。
- 不能保证：所有手机/国产系统/省电模式下都能锁屏自动打开任意 App 或准时执行。

## 路线图（实现方案 §23）

- [x] 0.1.0 工程骨架 + GitHub Actions 自动构建
- [x] 0.2.0 任务管理 + 本地保存
- [x] 0.3.0 应用选择 + 手动打开目标 App
- [x] 0.4.0 AlarmManager 定时触发 + 通知提醒
- [x] 0.5.0 开机恢复 + tag 触发 Release 发布
- [x] 0.6.0 锁屏提醒 + Full-screen Intent（本里程碑）
- [ ] 0.7.0 重复提醒 + 权限诊断
- [ ] 1.0.0 稳定自用版本
