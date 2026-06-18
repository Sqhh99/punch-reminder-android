# 安卓打卡提醒助手实现方案

> 文档用途：本方案用于指导后续具体实施 AI 或开发人员进行工程落地。  
> 本文不深入 Kotlin 代码实现细节，重点约束项目工程结构、CI/CD、测试体系、验收标准与 AI 实施边界。

---

## 1. 项目背景

用户在上下班时需要打开指定打卡应用完成签到或签退。当前打卡应用本身具备自动签到能力，但前提是用户需要在特定时间点打开该应用。

由于用户经常忘记打开打卡软件，普通闹钟又需要用户手动再去查找并打开应用，因此希望开发一款安卓工具，在指定时间提醒用户，并尽可能自动拉起指定打卡应用。

本软件定位为：

**上下班打卡提醒助手 / 定时应用启动助手**

软件的核心目标不是破解打卡软件，也不是伪造签到行为，而是在用户授权和主动配置的前提下，帮助用户按时打开目标应用，降低忘记打卡的概率。

---

## 2. 技术选型

确定采用以下技术栈：

```text
开发语言：Kotlin
UI 框架：Jetpack Compose
架构：MVVM / MVI 均可，建议 MVVM 起步
数据存储：Room 或 DataStore
定时调度：AlarmManager 为主，WorkManager 为辅
后台触发：BroadcastReceiver
通知：Notification + Full-screen Intent
启动 App：PackageManager + Launch Intent
权限处理：AndroidX Activity Result API
依赖注入：Hilt 或 Koin，可选
CI/CD：GitHub Actions
```

建议 MVP 阶段优先使用：

```text
Kotlin + Jetpack Compose + MVVM + DataStore + AlarmManager + BroadcastReceiver
```

原因是第一版任务数据量较小，通常只有“上班打卡”“下班打卡”等少量任务。DataStore 足够支撑 MVP，后续如果加入复杂任务历史、节假日规则、打卡日志、统计功能，再考虑引入 Room。

---

## 3. 核心工程原则

本项目的实现重点不是 UI，而是 Android 系统能力的稳定使用。

核心系统能力包括：

```text
定时触发
后台广播
锁屏提醒
通知权限
精确闹钟权限
启动其他 App
开机后恢复任务
电池优化适配
国产手机后台限制适配
```

因此，工程设计需要遵循以下原则：

```text
1. UI 层只负责展示和交互，不直接调用系统能力。
2. ViewModel 只负责页面状态和用户操作分发。
3. UseCase 负责业务规则，例如下一次触发时间计算。
4. Repository 负责数据读写。
5. System 层封装 Android 系统 API，例如 AlarmManager、PackageManager、NotificationManager。
6. 所有核心业务逻辑必须可测试。
7. 所有 PR 必须通过 CI 后才能合并。
8. 真机验收不可被 CI 完全替代。
```

---

## 4. 推荐功能里程碑

### 4.1 里程碑一：工程骨架

目标：

```text
创建 Kotlin + Compose 安卓项目
接入 GitHub Actions
可以自动构建 debug APK
可以上传 APK 构建产物
```

验收标准：

```text
GitHub Actions 可以成功运行
main 分支提交后可以自动生成 APK
APK 可以下载安装到手机
项目具备基础目录结构
```

---

### 4.2 里程碑二：任务配置

目标：

```text
实现任务列表页
实现新增任务页
实现编辑任务页
实现任务启用 / 停用
实现任务数据本地保存
```

任务数据至少包含：

```text
任务名称
执行时间
执行周期
目标应用包名
是否启用
是否重复提醒
提醒间隔
最大提醒次数
```

验收标准：

```text
可以新增任务
可以编辑任务
可以删除任务
可以启用 / 停用任务
退出 App 后任务仍然存在
```

---

### 4.3 里程碑三：应用选择与打开

目标：

```text
读取手机中已安装且可从桌面启动的应用
展示应用图标、名称、包名
用户可以选择目标打卡应用
可以根据包名尝试打开目标应用
```

验收标准：

```text
可以进入应用选择页
可以看到手机上可启动的应用列表
可以选择一个目标应用
点击测试按钮可以打开目标应用
目标应用卸载后有明确提示
```

---

### 4.4 里程碑四：定时与后台触发

目标：

```text
接入 AlarmManager
接入 BroadcastReceiver
实现下一次触发时间计算
实现定时任务触发
实现任务触发后重新调度下一次任务
实现开机后恢复任务
```

验收标准：

```text
到达指定时间后可以触发提醒
每天任务可以顺延到下一天
工作日任务可以跳过周末
手机重启后可以恢复已启用任务
任务禁用后不再触发
```

---

### 4.5 里程碑五：提醒增强

目标：

```text
实现普通通知
实现高优先级通知
实现锁屏提醒
实现 Full-screen Intent
实现重复提醒
实现权限诊断页
```

验收标准：

```text
App 在后台时可以收到提醒
手机锁屏时可以收到提醒
点击通知可以打开目标 App
系统允许时可以尝试自动拉起目标 App
系统限制时有明确提示
用户可以看到权限状态和配置建议
```

---

## 5. 推荐项目目录结构

建议项目按照以下方式组织：

```text
app/
 ├─ ui/
 │   ├─ tasklist/
 │   ├─ taskedit/
 │   ├─ apppicker/
 │   ├─ permission/
 │   └─ alarm/
 │
 ├─ viewmodel/
 │   ├─ TaskListViewModel
 │   ├─ TaskEditViewModel
 │   └─ PermissionViewModel
 │
 ├─ domain/
 │   ├─ model/
 │   ├─ usecase/
 │   └─ scheduler/
 │
 ├─ data/
 │   ├─ datastore/
 │   ├─ repository/
 │   └─ mapper/
 │
 ├─ system/
 │   ├─ alarm/
 │   ├─ receiver/
 │   ├─ notification/
 │   ├─ launcher/
 │   └─ permission/
 │
 └─ test/
```

各层职责：

```text
ui：Compose 页面和组件
viewmodel：页面状态管理
domain：业务模型、业务规则、用例
data：任务数据存储、Repository 实现
system：Android 系统能力封装
test：单元测试、集成测试、测试工具
```

实施要求：

```text
Compose 页面不得直接调用 AlarmManager、PackageManager、NotificationManager。
系统 API 必须封装到 system 层。
时间计算逻辑必须放在 domain 层，方便单元测试。
```

---

## 6. GitHub 仓库与分支策略

建议使用 GitHub 作为代码仓库和 CI/CD 平台。

### 6.1 分支策略

```text
main：稳定主分支，只保存可构建代码
feature/*：功能开发分支
fix/*：缺陷修复分支
test/*：测试补充分支
ci/*：CI/CD 配置分支
```

示例：

```text
feature/task-scheduler
feature/app-picker
feature/notification-flow
feature/permission-guide
fix/alarm-reschedule-after-boot
test/scheduler-unit-tests
ci/android-build-workflow
```

### 6.2 合并策略

```text
禁止直接提交到 main
所有功能通过 Pull Request 合并
PR 必须通过 CI 检查
PR 必须有明确描述和验收标准
CI 失败时不得合并
```

---

## 7. 提交信息规范

建议采用统一提交格式：

```text
feat: 新增功能
fix: 修复问题
test: 添加或修改测试
ci: 修改 CI/CD 配置
docs: 修改文档
refactor: 重构代码
chore: 工程杂项调整
```

示例：

```text
feat: add task scheduler domain model
feat: add installed app picker
fix: restore enabled alarms after reboot
test: add next trigger time unit tests
ci: add android build workflow
docs: update implementation plan
```

这样便于后续生成 Release Notes，也便于 AI 理解项目演进历史。

---

## 8. CI/CD 总体设计

CI/CD 分成四条流水线：

```text
PR 检查流水线
main 构建流水线
nightly 检查流水线
release 发布流水线
```

---

## 9. PR 检查流水线

### 9.1 触发条件

```text
创建 Pull Request
更新 Pull Request
重新打开 Pull Request
```

### 9.2 执行内容

```text
1. 检出代码
2. 设置 JDK
3. 设置 Android SDK
4. 恢复 Gradle 缓存
5. 校验 Gradle Wrapper
6. 执行代码格式检查
7. 执行 Android Lint
8. 执行本地单元测试
9. 执行 debug 构建
10. 上传测试报告和构建日志
```

### 9.3 质量门禁

```text
项目必须能编译
单元测试必须全部通过
Lint 不允许有严重错误
不允许提交签名密钥
不允许提交明文敏感信息
PR 不允许绕过 CI 合并
```

PR 流水线的重点是快速反馈。建议控制在几分钟内完成，不要把所有模拟器测试都放到 PR 检查里。

---

## 10. main 构建流水线

### 10.1 触发条件

```text
代码合并到 main 分支
手动触发 workflow_dispatch
```

### 10.2 执行内容

```text
1. 检出代码
2. 设置构建环境
3. 执行单元测试
4. 执行 debug 构建
5. 上传 debug APK
6. 上传测试报告
7. 上传 lint 报告
8. 记录 commit hash 和构建时间
```

### 10.3 构建产物

建议每次 main 构建产出：

```text
app-debug.apk
unit test report
lint report
build log
version metadata
```

APK 命名建议：

```text
PunchReminder-debug-0.1.0-main-a1b2c3d.apk
```

这样方便在手机上测试不同版本时进行区分。

---

## 11. nightly 检查流水线

### 11.1 触发条件

```text
每天固定时间自动执行
手动触发
```

### 11.2 执行内容

nightly 可以运行更重的检查：

```text
1. 全量单元测试
2. 集成测试
3. Android Lint
4. Compose UI 测试
5. 模拟器仪器测试
6. 依赖安全检查
7. 构建 release candidate APK
```

nightly 流水线可以慢一些，不要求像 PR 流水线一样快速。

---

## 12. release 发布流水线

### 12.1 触发条件

release 流水线建议手动触发，不要每次 main 更新都自动发布。

触发时需要填写：

```text
版本号
发布说明
构建类型
是否创建 GitHub Release
```

### 12.2 执行内容

```text
1. 检出代码
2. 设置 JDK 和 Android SDK
3. 执行测试
4. 构建 release APK
5. 使用 GitHub Secrets 中的签名信息签名
6. 生成 SHA256 校验值
7. 创建 GitHub Release
8. 上传 APK、校验值、发布说明
```

### 12.3 签名安全要求

以下内容不能提交到仓库：

```text
keystore 文件
keystore 密码
key alias
key password
签名配置明文
```

这些内容必须放在 GitHub Secrets 中。

实施 AI 不得把密钥写入源码，不得把密钥打印到日志中。

---

## 13. 构建产物管理

构建产物建议保留策略：

```text
PR 构建产物：保留 7 天
main debug APK：保留 30 天
release APK：长期保留在 GitHub Release
```

每个 APK 应该能够追溯到：

```text
Git commit
构建时间
构建分支
版本号
构建类型
```

建议 App 内“关于页面”显示：

```text
版本号
构建类型
commit hash
构建时间
```

---

## 14. 测试体系设计

测试分为四层：

```text
本地单元测试
集成测试
仪器测试 / UI 测试
真机验收测试
```

---

## 15. 本地单元测试

本地单元测试运行在 JVM 上，速度快，适合每个 PR 都执行。

重点测试纯业务逻辑，不依赖真实 Android 系统环境。

### 15.1 必测模块

```text
NextTriggerTimeCalculator
TaskRepository
PermissionStateMapper
InstalledAppFilter
NotificationContentBuilder
AppLaunchDecision
AlarmScheduleRequestBuilder
```

### 15.2 时间计算测试

`NextTriggerTimeCalculator` 是核心模块，必须重点测试。

测试内容包括：

```text
每天执行时，下一次触发时间是否正确
工作日执行时，周五之后是否跳到下周一
自定义星期执行时，是否跳到正确日期
当前时间已超过今天任务时间时，是否顺延到下一次
任务被禁用时，是否不参与调度
跨天是否正确
跨周是否正确
月底是否正确
年底是否正确
```

### 15.3 任务状态测试

需要覆盖：

```text
任务启用
任务停用
任务删除
任务保存
任务读取
目标应用包名为空
目标应用不存在
任务配置不完整
```

---

## 16. 集成测试

集成测试关注多个模块之间是否能串起来。

建议覆盖以下流程：

```text
创建任务 → 保存任务 → 读取任务
创建任务 → 计算下一次触发时间 → 注册 Alarm
AlarmReceiver 收到触发 → 查询任务 → 生成提醒通知
BootReceiver 收到开机广播 → 重新注册所有启用任务
权限状态变化 → 重新调度精确闹钟
目标 App 被卸载 → 任务进入异常状态
```

集成测试中可以使用 fake 对象替代真实系统 API，例如：

```text
FakeAlarmScheduler
FakeTaskRepository
FakeNotificationDispatcher
FakeAppLauncher
FakePermissionChecker
```

这样可以降低测试对真实 Android 环境的依赖。

---

## 17. 仪器测试与 UI 测试

仪器测试运行在模拟器或真机上。由于速度较慢，不建议每个 PR 都跑完整仪器测试。

建议放到 nightly 流水线。

### 17.1 MVP 阶段 UI 测试范围

```text
打开 App 后能看到任务列表
可以进入新增任务页
可以设置任务名称
可以设置执行时间
可以选择执行周期
可以进入应用选择页
可以保存任务
可以启用任务
可以停用任务
可以进入权限引导页
```

### 17.2 UI 测试设计建议

```text
页面状态使用统一 ViewState
测试尽量不要依赖真实系统权限
复杂系统行为交给真机验收
Compose 组件要有稳定测试标识
```

---

## 18. 真机验收测试

本项目最重要的验证是 Android 真机测试，因为锁屏、后台、省电、通知、拉起第三方应用这些行为在不同系统上差异很大。

### 18.1 必测场景

每个重要版本发布前，至少测试：

```text
App 在前台，到点是否提醒
App 在后台，到点是否提醒
手机锁屏，到点是否提醒
手机息屏，到点是否提醒
点击通知是否能打开目标 App
全屏提醒页是否能显示
目标 App 是否能被拉起
任务触发后是否重新设置下一次时间
手机重启后任务是否恢复
关闭通知权限后提示是否正确
关闭精确闹钟权限后提示是否正确
开启省电模式后表现如何
目标 App 卸载后提示是否正确
```

### 18.2 建议设备矩阵

如果后续要提高兼容性，建议覆盖：

```text
原生/接近原生 Android：Pixel 或模拟器
小米 / Redmi：MIUI 或 HyperOS
华为 / 荣耀：HarmonyOS / MagicOS
OPPO / OnePlus / realme：ColorOS
vivo / iQOO：OriginOS
三星：One UI
```

### 18.3 真机测试记录

每次真机测试建议记录：

```text
APK 文件名
版本号
commit hash
手机品牌
手机型号
Android 版本
系统版本
通知权限状态
精确闹钟权限状态
电池优化状态
自启动权限状态
测试结果
异常现象
复现步骤
```

---

## 19. 权限诊断设计

建议 App 内提供“权限检查页”。

该页面用于告诉用户哪些权限已经开启，哪些权限可能影响提醒稳定性。

### 19.1 权限状态展示

建议展示：

```text
通知权限：已开启 / 未开启
精确闹钟权限：已开启 / 未开启
全屏提醒权限：已开启 / 未开启
锁屏通知：建议开启
电池优化：建议忽略
开机自启动：建议开启
后台运行：建议允许
```

### 19.2 权限说明原则

权限说明要站在用户角度，不要写技术术语。

示例：

```text
通知权限：用于在上下班时间提醒你打开打卡应用。
精确闹钟权限：用于尽量保证提醒时间准确。
忽略电池优化：用于减少系统省电策略导致提醒延迟的问题。
开机自启动：用于手机重启后恢复打卡提醒。
```

---

## 20. Android 系统限制说明

本项目必须明确边界：

```text
可以稳定保证：
任务保存
任务管理
到点通知
点击通知打开目标 App
开机后重新注册任务

尽量尝试：
锁屏状态下弹出提醒
系统允许时自动打开目标 App
后台状态下拉起目标 App

不能保证：
所有手机都能锁屏自动打开任意 App
所有国产系统都允许后台启动
所有省电模式下都能准时执行
```

需求文档和产品文案不应承诺“100% 锁屏自动打开目标应用”。

更合理的表述是：

```text
到点提醒，并在系统允许的情况下自动尝试打开目标 App；
如果系统限制后台启动，则通过通知或全屏提醒引导用户打开。
```

---

## 21. Issue 与 PR 工作流

后续可以让具体实施 AI 按以下流程工作：

```text
1. 用户创建 Issue，描述要实现的功能
2. AI 输出实现计划
3. 用户确认计划
4. AI 创建功能分支
5. AI 修改代码
6. AI 编写或更新测试
7. AI 提交 PR
8. GitHub Actions 自动构建和测试
9. 如果 CI 失败，AI 根据日志修复
10. CI 通过后合并到 main
11. main 自动生成 APK
12. 用户下载 APK 真机测试
13. 用户反馈问题，再创建 fix issue
```

### 21.1 Issue 模板建议

每个 Issue 建议包含：

```text
功能目标
用户场景
验收标准
不做什么
测试要求
影响模块
```

示例：

```text
Issue：实现工作日打卡任务

目标：
用户可以创建仅周一至周五执行的任务。

用户场景：
用户希望上班和下班提醒只在工作日触发，周末不提醒。

验收标准：
周一至周四触发后，下一次为第二天。
周五触发后，下一次为下周一。
周六周日不会触发。
相关单元测试必须覆盖。

不做什么：
不处理法定节假日和调休。

测试要求：
必须添加 NextTriggerTimeCalculator 的工作日测试。
```

---

## 22. AI 实施边界

为了避免后续 AI 过度设计或引入风险功能，需要提前规定边界。

### 22.1 AI 可以做

```text
创建项目结构
实现功能模块
编写单元测试
修复 CI 报错
补充文档
优化工程配置
调整 UI 交互
```

### 22.2 AI 不应擅自做

```text
引入大型复杂框架
修改核心技术栈
上传签名密钥
关闭测试
绕过 Android 权限限制
实现伪定位
模拟点击打卡
破解打卡软件
读取用户打卡账号密码
把敏感信息写入日志
```

### 22.3 必须先确认的事项

以下事项必须先让用户确认：

```text
新增第三方依赖
修改最低 Android 版本
修改签名方式
修改权限清单
加入后台保活方案
加入无障碍服务
加入悬浮窗权限
加入 Root 或系统级能力
```

MVP 阶段不建议使用无障碍服务、悬浮窗、Root、系统签名等能力。

---

## 23. 版本规划

建议使用简单语义化版本。

```text
0.1.0：基础工程骨架 + GitHub Actions 自动构建
0.2.0：任务管理 + 本地保存
0.3.0：应用选择 + 手动打开目标 App
0.4.0：AlarmManager 定时触发 + 通知提醒
0.5.0：开机恢复 + 工作日规则
0.6.0：锁屏提醒 + Full-screen Intent
0.7.0：重复提醒 + 权限诊断
1.0.0：稳定自用版本
```

---

## 24. MVP 验收标准

### 24.1 工程层面

```text
项目可以通过 GitHub Actions 自动构建
PR 会自动运行单元测试
main 分支会自动产出 debug APK
APK 可以下载安装到手机
构建失败时能看到明确日志
```

### 24.2 功能层面

```text
可以新增打卡任务
可以设置每天或工作日执行
可以选择手机上安装的目标 App
可以保存和启用任务
到达时间后可以弹出通知
点击通知可以打开目标 App
手机重启后任务可以恢复
关闭必要权限时 App 有明确提示
```

### 24.3 测试层面

```text
时间计算逻辑有单元测试
任务保存读取有测试
任务启用停用有测试
开机恢复逻辑有测试
目标 App 不存在的异常路径有测试
UI 核心流程有基础测试
真机验收清单有记录
```

---

## 25. 推荐最终实施路线

建议按以下顺序推进：

```text
第一步：创建 Android 工程骨架
第二步：接入 GitHub Actions
第三步：实现 DataStore 任务保存
第四步：实现任务列表和任务编辑页面
第五步：实现应用选择页面
第六步：实现手动打开目标应用
第七步：实现 AlarmManager 定时触发
第八步：实现通知提醒
第九步：实现开机恢复
第十步：实现锁屏提醒和全屏提醒
第十一步：实现重复提醒
第十二步：实现权限诊断
第十三步：进行真机兼容性测试
```

这条路线的核心思想是：

```text
先建立 CI/CD 和测试闭环，再逐步实现功能；
先保证到点提醒和点击打开，再尝试增强锁屏自动拉起；
先做稳定 MVP，再做复杂兼容性优化。
```

---

## 26. 交给实施 AI 的简短指令

后续如果要把项目交给 AI Coding 工具实施，可以使用以下指令作为起点：

```text
请基于本需求和实现方案创建一个 Android Kotlin + Jetpack Compose 项目。

技术栈要求：
Kotlin、Jetpack Compose、MVVM、DataStore、AlarmManager、BroadcastReceiver、Notification、PackageManager、AndroidX Activity Result API、GitHub Actions。

工程要求：
1. 不要直接在 main 分支开发。
2. 每个功能通过独立 feature 分支完成。
3. 每个 PR 必须包含必要单元测试。
4. 必须接入 GitHub Actions，保证可以自动构建 debug APK。
5. UI 层不得直接调用 AlarmManager、PackageManager、NotificationManager。
6. Android 系统能力必须封装在 system 层。
7. 时间计算逻辑必须可单元测试。
8. 不允许实现伪定位、模拟点击、破解打卡软件、读取账号密码等功能。
9. 不允许提交签名密钥或敏感信息。
10. 先完成 MVP：任务管理、应用选择、到点通知、点击通知打开目标 App、开机恢复。
```

---

## 27. 总结

本项目的核心不是简单做一个安卓页面，而是建立一个可持续迭代、可测试、可自动构建的安卓工程。

最推荐的落地方式是：

```text
GitHub 仓库管理代码
GitHub Actions 自动构建 APK
PR 负责功能开发和质量门禁
单元测试覆盖核心业务逻辑
nightly 测试覆盖较重流程
真机验收验证锁屏、后台和系统权限行为
```

最终目标是让整个开发流程变成：

```text
提出需求
    ↓
AI 输出计划
    ↓
创建 PR
    ↓
自动测试和构建
    ↓
下载 APK
    ↓
真机测试
    ↓
反馈问题
    ↓
继续迭代
```

这样即使后续主要交给 AI 实施，也能通过 CI/CD、测试体系、分支策略和真机验收清单，把项目风险控制在可接受范围内。
