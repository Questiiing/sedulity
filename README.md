### 纯ai写的安卓软件

# 业精于勤

Android 个人侧载应用：给自己的应用使用时间设「双重关卡」。

- **保底任务（下限）**：给指定应用（如微信读书）设每日最少使用分钟数和提醒时间，到点未达标发通知提醒"该去打开它了"。
- **使用限制（上限）**：给指定应用（如小红书、Instagram）设每日最多使用分钟数，打开超限应用时弹出全屏拦截页（30 秒倒计时），点「继续使用」后有 60 秒宽限。

## 技术栈

- Kotlin + XML 视图 + Material Components
- minSdk 24 / targetSdk 35（兼容 Android 15）
- 无障碍事件驱动实时前台检测（**无轮询**，待机零耗电），WorkManager 15 分钟兜底
- 数据存于 SharedPreferences（JSON）

## 需要的权限

- 无障碍「实时拦截」：事件驱动检测当前打开的应用
- 使用情况访问：统计每日使用时长
- 通知：到点提醒 / 超限提醒
- 精确闹钟：保底任务到点提醒

> 另有轻量保活前台服务（无轮询、几乎不耗电），避免国产系统冻结进程导致事件延迟。

## 构建

```
. D:\DevTools\build.ps1
cd C:\Users\Questiiing\Desktop\项目尝试\sedulity
gradle assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/`

## 使用说明

安装授权与小米/红米 HyperOS 专属设置详见 [使用说明.md](使用说明.md)。
