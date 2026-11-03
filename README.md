# MeetingCastLauncher

面向会议室的 Android TV 投屏桌面。以 HDMI 有线投屏为主，Mac 和 Windows 无线投屏为辅，适用于旧款小米、Redmi 电视的会议显示场景。

## 功能

- 深色、适合远距离观看的会议桌面
- 显示会议室设备名称，支持在桌面直接修改
- 同步设备名称到 AirPlay、蓝牙和 Wi-Fi Direct
- HDMI 连接状态展示与全屏入口
- Mac 屏幕镜像和 Windows 无线投屏入口
- 开机自动进入，支持设为默认桌面
- 默认无选中，方向键唤起并导航，返回键取消选中
- 空闲五分钟后进入系统屏保
- 保留系统设置入口

## 设备适配清单

以下为目前实际连接并验证过的设备。电视未提供具体硬件型号时，仅记录品牌和系统版本，不推测型号。

| 设备 | 系统版本 | 验证情况 |
| --- | --- | --- |
| Redmi 电视 | Android 9 | 已验证 v1.3.0：默认桌面、开机自启、HDMI 状态、无线投屏入口、设备名称同步、屏保及遥控器导航 |
| Redmi 电视 | MiTV OS 2.13.1365 | 已验证早期版本的默认桌面与会议投屏改造，尚未回归验证 v1.3.0 |
| 小米电视（MiTV） | MIUI TV 1.2.125 | 已验证早期版本的默认桌面与会议投屏改造，尚未回归验证 v1.3.0 |

不同系统版本的投屏服务、屏保组件和权限可能不同。未列出的设备请先备份并测试，欢迎通过 Issue 补充硬件型号、系统版本和验证结果。

## 构建

需要 JDK 和 Android SDK。首次构建前创建 `local.properties`，设置本机的 `sdk.dir`。

```bash
./gradlew :app:assembleDebug
```

APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 安装

开启电视的 ADB 调试后，在同一网络下执行：

```bash
adb connect <电视IP>:5555
adb install -r MeetingCastLauncher-1.3.0.apk
```

在电视的默认桌面设置中选择 MeetingCastLauncher。设备名称同步需要 `WRITE_SECURE_SETTINGS` 权限：

```bash
adb shell pm grant com.company.meetingcast android.permission.WRITE_SECURE_SETTINGS
```

本应用不包含关闭广告、禁用系统应用等操作；这些维护操作应根据设备型号和企业管理要求单独执行。

## 免责声明

本项目不是小米或 Redmi 官方产品，与相关公司没有隶属或背书关系。修改系统设置、默认桌面和权限前请备份并确认设备管理要求。

## License

MIT
