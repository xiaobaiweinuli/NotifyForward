# 🔔 NotifyForward — Android/鸿蒙通知实时转发到企业微信

**将 Android / 鸿蒙设备上的任意系统通知，按自定义规则过滤后实时推送到企业微信机器人，本地优先，零依赖云服务。**

[![Release](https://img.shields.io/github/v/release/xiaobaiweinuli/NotifyForward?style=flat-square)](https://github.com/xiaobaiweinuli/NotifyForward/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208%2B%20%7C%20HarmonyOS%204%2B-green?style=flat-square)](https://github.com/xiaobaiweinuli/NotifyForward)
[![Build](https://github.com/xiaobaiweinuli/NotifyForward/actions/workflows/build_release.yml/badge.svg)](https://github.com/xiaobaiweinuli/NotifyForward/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org)

---

## ✨ 核心功能

| 功能         | 说明                                                                  |
| ---------- | ------------------------------------------------------------------- |
| **实时通知捕获** | 基于 `NotificationListenerService`，实时获取标题、正文、包名、时间戳                   |
| **灵活规则过滤** | 按包名白名单 + 包含关键词 + 排除关键词三维过滤，规则优先级可排序                                 |
| **企业微信推送** | Webhook 机器人推送，支持失败重试，消息长度自动截断至 2048 字节                              |
| **模板引擎**   | 支持 8 个内置变量（appName/title/content/datetime 等），规则级模板优先于全局模板           |
| **去重过滤**   | 可配置时间窗口（1–60 秒），窗口内相同内容只转发一次                                        |
| **转发历史**   | 本地查看成功/失败/过滤记录，支持关键词搜索与状态筛选                                         |
| **多级保活**   | ForegroundService + WorkManager 心跳 + BootReceiver + NetworkReceiver |
| **深浅色主题**  | 跟随系统 / 强制浅色 / 强制深色，MainActivity 实时响应切换                              |
| **关于页可配置** | 社交链接支持增删，DataStore 本地持久化                                            |

---

## 🏗️ 技术架构

```
通知捕获层
  NotificationListenerService
    ↓ Channel<NotificationData>（无限缓冲协程队列）

处理层
  ForwardRepository.processQueue()
    ├── 全局开关 & Webhook 检查
    ├── 过滤自身 & 系统通知
    ├── 去重（ConcurrentHashMap 时间窗口）
    ├── 规则匹配（包名 + 包含词 + 排除词，AND 逻辑）
    └── 模板渲染（规则模板 → 全局模板）
    ↓ OkHttp POST (JSON)

网络层
  WeChatForwarder → 企业微信 Webhook
    └── sendWithRetry（指数退避）

持久化层
  Room        → ForwardRule + ForwardHistory
  DataStore   → AppConfig（含 ThemeMode）+ SocialLinks

保活层
  ForwardForegroundService  START_STICKY + IMPORTANCE_MIN 通知
  ServiceCheckWorker        WorkManager 每 15 分钟心跳
  BootReceiver              开机/包更新自启
  NetworkChangeReceiver     网络恢复触发重启

UI 层（Jetpack Compose）
  首页      → 状态总览 + 主开关 + 今日统计
  规则      → CRUD + App 选择器 + 关键词过滤
  记录      → 转发历史 + 搜索 + 状态筛选
  设置      → 主题切换 + Webhook + 模板 + 策略
  关于      → App 图标 + 社交链接（可增删）+ 版本
```

**技术栈：**

| 组件                    | 版本           |
| --------------------- | ------------ |
| Kotlin                | 2.1.0        |
| Jetpack Compose BOM   | 2025.01.00   |
| Room                  | 2.6.1        |
| DataStore Preferences | 1.1.3        |
| OkHttp                | 4.12.0       |
| WorkManager           | 2.10.0       |
| Navigation Compose    | 2.8.5        |
| Gson                  | 2.11.0       |
| KSP                   | 2.1.0-1.0.29 |

---

## 📲 快速开始

### 1. 下载安装

前往 [Releases](https://github.com/xiaobaiweinuli/NotifyForward/releases) 下载最新 APK 安装。

### 2. 配置 Webhook

在企业微信群中 **添加机器人** → 复制 Webhook 地址 → 进入 App **设置 → 企业微信 Webhook** → 粘贴保存 → 点击「测试发送」验证。

### 3. 授权通知权限

首页点击「通知监听权限 → 未授权」卡片，跳转系统通知使用权设置页面，找到「通知转发」并开启。

### 4. 创建转发规则

进入**规则**页 → 右下角「+ 新建规则」：

- 选择要监听的 App（留空 = 监听全部）
- 设置包含关键词（命中任一即转发）
- 设置排除关键词（命中任一则丢弃）
- 可选填写本规则的消息模板

### 5. 开启总开关

回到**首页**，打开「转发总开关」即可实时转发通知到企业微信。

---

## 🔧 模板变量参考

| 变量              | 说明   | 示例                  |
| --------------- | ---- | ------------------- |
| `{appName}`     | 应用名称 | 微信                  |
| `{title}`       | 通知标题 | 张三                  |
| `{content}`     | 通知正文 | 在吗？                 |
| `{subText}`     | 副标题  | —                   |
| `{packageName}` | 包名   | com.tencent.mm      |
| `{time}`        | 时间   | 14:35:20            |
| `{date}`        | 日期   | 2025-01-08          |
| `{datetime}`    | 日期时间 | 2025-01-08 14:35:20 |

**默认全局模板：**

```
【{appName}】{title}
{content}
─── {datetime}
```

---

## 📦 自行编译

```bash
# 克隆仓库
git clone https://github.com/xiaobaiweinuli/NotifyForward.git
cd NotifyForward

# 编译 Debug APK（无需签名，直接安装）
./gradlew assembleDebug

# 编译 Release APK（指定版本号）
./gradlew assembleRelease \
  -PVERSION_NAME=1.2.0 \
  -PVERSION_CODE=12
```

**环境要求：** JDK 17 · Android SDK API 35 · Gradle 8.x

---

## 🤖 GitHub Actions 自动发布

### 方式一：推送 Git Tag（推荐）

```bash
git tag v1.2.0
git push origin v1.2.0
# → 自动构建并发布到 GitHub Releases
```

### 方式二：手动触发 Workflow

进入仓库 **Actions → 🚀 Build & Release APK → Run workflow**，填写版本名称和版本号后点击运行。

### 签名配置（可选，提供正式签名 APK）

在仓库 **Settings → Secrets and variables → Actions** 中添加以下 Secret：

| Secret 名称           | 说明                     |
| ------------------- | ---------------------- |
| `KEYSTORE_BASE64`   | keystore 文件的 Base64 编码 |
| `KEY_ALIAS`         | Key alias              |
| `KEYSTORE_PASSWORD` | KeyStore 密码            |
| `KEY_PASSWORD`      | Key 密码                 |

生成 Base64：

```bash
base64 -w 0 my-release-key.jks > keystore.b64
# 将 keystore.b64 内容粘贴到 KEYSTORE_BASE64 Secret
```

不配置签名时，CI 仍会输出 `*-unsigned.apk`，可在本地手动签名。

---

## 📱 保活策略说明

| 机制                            | 触发时机      | 作用                    |
| ----------------------------- | --------- | --------------------- |
| `NotificationListenerService` | 系统管理      | 核心监听，系统断开后自动请求重绑      |
| `ForwardForegroundService`    | App 开启转发时 | 提升进程优先级（START_STICKY） |
| `ServiceCheckWorker`          | 每 15 分钟   | 检测并重启已被回收的前台服务        |
| `BootReceiver`                | 开机/包更新    | 开机后自动恢复转发服务           |
| `NetworkChangeReceiver`       | 网络连接恢复    | 网络恢复后自动重启服务           |

> **鸿蒙设备额外建议：** 系统设置 → 应用 → 通知转发 → 开启「允许后台运行」和「自启动」权限，效果最佳。

---

## 📄 开源协议

```
MIT License
Copyright (c) 2025 xiaobaiweinuli
```

详见 [LICENSE](LICENSE) 文件。

---

## 🔗 联系

- **GitHub：** [@xiaobaiweinuli](https://github.com/xiaobaiweinuli)
- **Telegram：** [@MyResNav](https://t.me/MyResNav)
- **仓库：** [xiaobaiweinuli/NotifyForward](https://github.com/xiaobaiweinuli/NotifyForward)

---

**仓库描述（复制到 GitHub About 栏）：** 基于 Kotlin + Jetpack Compose 开发的 Android/鸿蒙通知转发工具，支持自定义规则过滤、企业微信 Webhook 推送、深浅色主题切换与多层保活策略，开箱即用，本地优先。

**Topics：** `android` `harmonyos` `notification-forwarder` `wecom` `wechat-work` `webhook` `kotlin` `jetpack-compose` `notification-listener` `foreground-service` `workmanager` `room` `datastore`
