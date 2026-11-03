# AutoFigure Companion Android 工程（非官方）

这是一个 Android WebView 客户端壳，用于在手机上打开你自己部署的 AutoFigure 网页前端，并把 Flask API 后端地址写入网页的 `localStorage`。它不是把大模型或 Python 后端塞进手机；完整生成能力仍由服务器和模型 API 提供。

## 服务器部署

完整的 Ubuntu + Docker Compose + HTTPS 部署步骤见仓库根目录的 [`deploy/README_DEPLOY_CN.md`](../deploy/README_DEPLOY_CN.md)。

## 先决条件

1. 将本仓库的 Next.js 前端部署到一个手机可访问的 URL。
2. 将 `backend/` 和 `autofigure/` 部署为 Python Flask API 服务，端口建议为 `8796`。
3. 准备模型供应商 API Key（Gemini、Bianxie、OpenRouter 等，具体以项目设置页支持项为准）。
4. 正式使用时，前端和后端都应使用 HTTPS。

## 服务器端必须做的配置

### 1. 前端改动

`frontend/contexts/autofigure-context.tsx` 已调整为优先读取 `localStorage.autofigure_backend_url`。Android 客户端会在网页加载后写入该值，这样用户可在 APK 内更改后端地址，而不必重新编译网页。

### 2. 后端 CORS

`backend/app.py` 已支持 `AUTOFIGURE_ALLOWED_ORIGINS` 环境变量。部署后端时设置为前端的**精确 Origin**，多个域名用逗号分隔，例如：

```bash
AUTOFIGURE_ALLOWED_ORIGINS=https://fig.example.com
AUTOFIGURE_HOST=0.0.0.0
AUTOFIGURE_BACKEND_PORT=8796
```

不要为了省事设置 `*`。如果使用反向代理，建议让后端只在内网监听，由 HTTPS 代理转发。

### 3. 启动后端（单机测试）

在项目根目录执行：

```bash
python -m pip install -r backend/requirements.txt
playwright install chromium
AUTOFIGURE_HOST=0.0.0.0 AUTOFIGURE_ALLOWED_ORIGINS=https://fig.example.com python backend/app.py
```

后端需要 Python 依赖、Chromium（项目的图形渲染流程会用到）和可访问的模型 API。生产部署不要直接暴露 Flask debug server；应使用生产 WSGI Server、HTTPS 反向代理、防火墙和访问控制。后端当前把会话保存在进程内存中，部署时应保持单实例/单 worker，服务重启会丢失进行中的会话。

## APK 内设置

首次启动会显示服务器设置页：

- **网页前端地址**：例如 `https://fig.example.com`
- **Python 后端 API 地址**：例如 `https://api.example.com`

两个 URL 都必须能从手机网络访问。不能填写 `localhost` 或 `127.0.0.1`，因为在手机上它们指向手机自己。保存后即可进入网页端；API Key 仍在 AutoFigure 页面自身的模型设置中填写。

## 构建 APK

### GitHub Actions（推荐）

1. 将此仓库推送到你自己的 GitHub 仓库。
2. 打开仓库的 **Actions → Build Android APK**。
3. 点击 **Run workflow**，或修改 `android/` 后 push 自动触发。
4. 构建完成后，在该次运行页面的 **Artifacts** 下载 `AutoFigure-Companion-debug-apk`。
5. 解压得到 `app-debug.apk`，传到 Android 手机上安装（需允许该来源安装应用）。

### Android Studio

用 Android Studio 打开 `android/` 目录，安装 Android SDK Platform 35 和 Build Tools 35.0.0，使用 JDK 17 / Gradle 8.9，然后运行 `:app:assembleDebug`。生成位置：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

这是用于测试分发的 debug APK，不是 Google Play 发布用的签名 release APK。公开分发前应配置你自己的 release signing key，并进行安全审查。

## 安全提醒

- 模型 API Key 会由网页提交给后端；请使用 HTTPS，不要把密钥写进 APK、仓库或公开日志。
- 后端 API 不应直接裸露在公网；至少加上 HTTPS、反向代理访问控制、速率限制和日志脱敏。
- APK 的 `usesCleartextTraffic` 为兼容局域网测试暂时开启；正式环境请仅使用 HTTPS，并可在确认无 HTTP 依赖后关闭。

## 署名与商标

本客户端为非官方第三方封装，基于 AutoFigure 开源仓库。原项目软件许可、版权声明、引用说明和商标指南仍适用；此客户端不代表原作者或原项目官方发布。
