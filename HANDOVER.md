# MeetingCastLauncher 工程移交手册

更新时间：2026-11-03

## 1. 项目目标

将企业现有小米、Redmi 电视改造成会议投屏显示器：

- 以 HDMI 有线投屏为主，Mac、Windows 无线投屏为辅。
- 使用深色会议桌面，显示设备名称和 HDMI 状态。
- 保留小米原生系统设置。
- 去除视频内容、广告和无关后台服务。
- 保留投屏核心组件，避免为精简系统而影响主要功能。

本工程不是小米官方项目。设备系统差异较大，所有系统修改必须逐台验证，不可直接批量执行。

## 2. 工程结构

```text
MeetingCastLauncher/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/company/meetingcast/
│   │   ├── MainActivity.java
│   │   ├── HdmiFullscreenActivity.java
│   │   ├── HdmiReturnService.java
│   │   └── BootReceiver.java
│   └── res/
├── build.gradle
├── settings.gradle
├── README.md
└── LICENSE
```

- `MainActivity.java`：会议桌面、遥控器焦点、设备名称读取与同步、系统设置、HDMI 检测、屏保计时。
- `HdmiFullscreenActivity.java`：HDMI 直通播放、拔线返回、果冻连接动画和淡入淡出。
- `BootReceiver.java`：开机启动；旧系统有 5 秒接管等待，新系统立即启动。
- `HdmiReturnService.java`：预留的 HDMI 广播返回服务，目前主要返回逻辑由全屏页面轮询完成。修改前先确认是否仍需保留。

包名：`com.company.meetingcast`

当前版本标记：1.3.0（versionCode 4）。此后已加入多项兼容修复，发布新版本前应先提升版本号，不要继续以同一版本号发布不同 APK。

## 3. 当前交互约定

- 桌面启动后默认无选中。
- 第一次按方向键唤起 Mac 卡片，之后方向键正常导航。
- 有选中时按返回键取消选中；无选中时按返回键不退出桌面。
- HDMI 已连接信息使用绿色 `#30D158`。
- 最新代码在识别 HDMI 后自动打开全屏投屏，不再要求按确认。
- 全屏投屏前显示深色“正在连接 HDMI”果冻动画。
- 拔出 HDMI 后轮询检测断开并返回桌面。
- 空闲五分钟后尝试启动小米原生屏保。
- 系统设置直接打开小米原生设置（`com.xiaomi.mitv.settings/.entry.MainActivity`），失败后回退标准设置入口。

## 4. 构建与安装

需要 JDK 和 Android SDK。`local.properties` 只保存本机 SDK 路径，不提交到 Git。

```bash
./gradlew :app:assembleDebug
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

ADB 安装：

```bash
adb connect <电视IP>:5555
adb install -r --no-streaming app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.company.meetingcast/.MainActivity
```

旧电视安装可能较慢，不要因暂时无输出立即重复安装。不要在安装后习惯性执行 `am force-stop`；旧固件可能把应用记录为停止状态，影响开机广播。

设备名称同步需要：

```bash
adb shell pm grant com.company.meetingcast android.permission.WRITE_SECURE_SETTINGS
```

## 5. 设备适配记录

### Redmi 电视，Android 9

企业内部测试设备，维护地址和设备名称已从公开文档中移除。

已验证：

- 默认桌面、开机自启、方向键和返回键。
- 设备名称同步到 AirPlay、蓝牙、Wi-Fi Direct。
- 小米原生屏保。
- 小米系统自动 HDMI 全屏投屏。

注意：这台的投屏和桌面改动早于最近针对 Android 6 的多项修复，后续维护时需重新回归测试。

部署状态：2026-11-04 已安装 1.3.7（versionCode 11）。系统设置直进小米原生设置已实测通过。曾出现 HDMI 投屏约 2 分钟后进入小米屏保，日志确认是系统电源服务在 `HdmiFullscreenActivity` 无 `FLAG_KEEP_SCREEN_ON` 时拉起 `com.mitv.screensaver/.RenderActivity`；1.3.7 已补充常亮标志，修复后连续投屏超过 2 分钟未再触发。Mac/Windows 无线投屏、重启自启等仍需回归。

### Redmi 电视，MiTV OS 2.13.1365

企业内部测试设备，维护地址已从公开文档中移除。

部署状态：2026-11-04 已升级 1.3.7（versionCode 11）。默认 HOME 为会议桌面；设置键直进小米原生设置、Mac/Windows 卡进入小米 `WfdActivity`、返回键不退出桌面均已远程实测通过。为让本应用接管 HDMI，设备端已设置 `settings put system tvapp_one_touch_play 0`（恢复小米即插即播则设回 1），并设置 `not_disturb_watching 1`。小米 SystemUI 的"输入源接入提醒"由全屏页每 500 毫秒广播 `CLOSE_SYSTEM_DIALOGS` 自动关闭，插线时可能短暂闪现。小爱语音已停用（`pm uninstall -k --user 0 com.xiaomi.voicecontrol`，可用 `pm install-existing` 恢复）。1.3.7 已包含 HDMI 全屏常亮修复，但当前 HDMI 未连接，投屏 2 分钟不进入屏保仍需插线回归；Mac/Windows 实际无线投屏和重启自启也待验证。设备名称已统一并同步到 System/Secure `dk_device_name`、Global `device_name`、蓝牙和 Wi-Fi Direct，且已发送 AirPlay 名称变更广播；WRITE_SECURE_SETTINGS 已授权。

### 小米电视，MIUI TV 1.2.125，Android 6.0.1

企业内部测试设备，维护地址和设备名称已从公开文档中移除。

已验证：

- 小米原生设置入口。
- 从小米 `dk_device_name` 读取设备名称。
- 默认进入会议桌面。
- HDMI 直通、果冻动画、无线投屏和设置功能。
- 精简后功能正常。

默认桌面关键操作：旧视频桌面拥有小米专用权限，标准 HOME 选择方式不可用。实际成功方案是为当前用户移除旧桌面：

```bash
adb shell pm uninstall -k --user 0 com.mitv.tvhome
adb shell input keyevent 3
```

恢复旧桌面：

```bash
adb shell pm install-existing com.mitv.tvhome
```

旧系统 HDMI 地址必须使用：

```java
TvContract.buildChannelUriForPassthroughInput(inputId)
```

不要手工拼接 `content://android.media.tv/input/<inputId>`。后者在 Android 6 上会造成 `IllegalArgumentException` 和反复崩溃，看起来就是屏幕闪烁。

部署状态：2026-11-04 已安装 1.3.7（versionCode 11），包含 HDMI 全屏常亮、输入源提醒自动关闭及快速检测修复。安装后 MainActivity 前台正常，设置键直进小米原生设置已实测通过。随后 ADB 再次超时掉线，无线投屏入口、HDMI 插拔、投屏 2 分钟不进入屏保和重启自启尚未完成本轮回归。该设备 `screen_saver_timeout=-1`，未设置 Android 9 测试设备的 120 秒值。

## 6. 已做系统精简

以下操作均针对用户 0，并保留数据，原则上可用 `pm install-existing <包名>` 恢复。

第一轮：

```text
com.mitv.shoplugin
com.xm.webcontent
com.ktcp.video
com.dbappstore.lite
com.gitv.dvb.live
com.ifacetv.browser
com.xiaomi.mitv.tvvideocall
cn.wps.moffice_i18n_TV
com.miui.systemAdSolution
```

第二轮：

```text
com.mitv.gallery
com.xiaomi.tv.gallery
com.xiaomi.smarthome.tv
com.xiaomi.smarthome.tv.service
com.xiaomi.mi_connect_service
com.xiaomi.statistic
com.miui.tv.analytics
com.xiaomi.mitv.tvpush.tvpushservice
com.xiaomi.mitv.appstore
com.duokan.videodaily
com.mitv.mivideoplayer
```

小爱语音随后通过以下方式停用：

```bash
adb shell pm uninstall -k --user 0 com.xiaomi.voicecontrol
```

恢复语音：

```bash
adb shell pm install-existing com.xiaomi.voicecontrol
```

`pm disable-user` 对部分 uid 1000 系统包会被拒绝，不要反复执行。当前用户移除是可逆方案，但仍需先确认业务需求。

## 7. 必须保留的组件

以下类型不得为追求开机速度而直接删除：

```text
com.xiaomi.mitv.smartshare
com.duokan.airkan.tvbox
com.xiaomi.wfdsinkhelperservice
com.xiaomi.upnp
com.xiaomi.miplay
com.milink.runtime
com.droidlogic.tvinput
com.xiaomi.mitv.settings
com.xiaomi.mitv.providers.settings
蓝牙、Wi-Fi、ADB 和系统升级相关组件
```

设备互联服务 `com.xiaomi.mi_connect_service` 已按业主要求移除；当前实测投屏正常。其他固件可能依赖它，不能照搬。

## 8. 设备名称逻辑

读取顺序：

1. System 表 `dk_device_name`（小米实际名称）。
2. Global 表 `device_name`。
3. Secure 表 `bluetooth_name`。
4. `Build.MODEL`。

保存时同步 Global、System/Secure、蓝牙、Wi-Fi Direct，并重载 AirPlay。部分固件 System 写入会抛出异常，再写 Secure；现有兼容捕获不要删除。

## 9. 已知限制

- 电视重启后 ADB 界面可能仍显示开启，但网络监听拒绝连接。通常需要关闭再打开 ADB。
- HDMI 信号初始化可能产生短暂黑屏，动画只能改善视觉，不能消除硬件切换。
- 屏保依赖 `com.mitv.screensaver`，不同固件可能不存在。小米电源服务另有约 120 秒无操作屏保检测，HDMI 全屏页必须保留 `FLAG_KEEP_SCREEN_ON`；1.3.6 及更早版本在部分 Android 9 固件上会在投屏约 2 分钟后进入小米屏保。
- 删除过多系统服务可能间接影响无线投屏，每批修改后必须测试 Mac、Windows 和 HDMI。
- 开机速度主要受系统初始化影响，精简后台应用只能有限改善。
- 当前系统修改发生在设备端，不在 APK 源码中；部署到新电视时必须按本手册单独处理。

## 10. Git 与开源发布

本地仓库已初始化，分支为 `main`，包含 MIT 许可证、README 和源码。截至移交时 GitHub 尚未授权，仓库尚未推送。

计划仓库名：`MeetingCastLauncher`。

发布前：

1. 检查 `git status`，确认无缓存、本机路径和设备隐私文件。
2. 更新版本号和 README 适配清单。
3. 在干净环境构建 APK。
4. 完成至少一台 Android 6 和一台 Android 9 设备回归。
5. 创建公开 GitHub 仓库并推送。
6. 发布带唯一版本号的 Release 和 APK。

## 11. 后续维护流程

1. 先读取设备型号、Android 版本和现有包状态。
2. 备份当前 APK、包清单和关键系统配置。
3. 一次只修改一类功能或一批明确无关的应用。
4. 验证默认桌面、HDMI、Mac、Windows、设置、名称和重启。
5. 出现问题优先读取日志，不要靠反复重启猜测。
6. 将设备差异和实际验证结果补回 README 与本手册。
